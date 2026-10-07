package com.saiprasad.talktome.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi

/**
 * Inserts dictated text into the focused text field of another app.
 *
 * Order of attempts:
 * 1. Android 13+: commit through the accessibility input connection, exactly like a keyboard
 *    typing at the cursor. Never sees placeholder text like WhatsApp's "Message".
 * 2. ACTION_SET_TEXT, inserting at the cursor (keeps what the user already typed, no clipboard).
 * 3. ACTION_PASTE, then restoring the user's previous clipboard when we could read it.
 * 4. Copy to the clipboard so the words are never lost.
 */
object TextInjector {
    private const val TAG = "TalkToMeInjector"

    enum class Result { INSERTED, PASTED, COPIED_TO_CLIPBOARD }

    /** The editable node that currently holds input focus, across all windows. */
    fun findFocusedEditable(service: AccessibilityService): AccessibilityNodeInfo? {
        val focused = try {
            service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (e: Exception) {
            null
        }
        if (focused?.isEditable == true) return focused

        val root = service.rootInActiveWindow ?: return null
        return findFocusedEditableIn(root)
    }

    /** True for fields Wispr Flow also stays out of: passwords, PINs, numbers, phone numbers, dates. */
    fun isSensitive(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val inputType = node.inputType
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> true
            InputType.TYPE_CLASS_TEXT -> when (inputType and InputType.TYPE_MASK_VARIATION) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> true
                else -> false
            }
            else -> false
        }
    }

    /**
     * @param fallbackNode the field that was focused when recording started, used if focus
     *   can't be found any more (e.g. the app briefly re-laid out while we were transcribing).
     */
    fun inject(service: AccessibilityService, text: String, fallbackNode: AccessibilityNodeInfo?): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && commitWithInputConnection(service, text)) {
            return Result.INSERTED
        }

        val node = findFocusedEditable(service)
            ?: fallbackNode?.takeIf { it.refresh() && it.isEditable }

        if (node == null) {
            Log.w(TAG, "No focused editable node; copying to clipboard")
            copyToClipboard(service, text)
            return Result.COPIED_TO_CLIPBOARD
        }

        return when {
            insertWithSetText(node, text) -> Result.INSERTED
            paste(service, node, text) -> Result.PASTED
            else -> {
                copyToClipboard(service, text)
                Result.COPIED_TO_CLIPBOARD
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun commitWithInputConnection(service: AccessibilityService, text: String): Boolean {
        return try {
            val inputMethod = service.inputMethod ?: return false
            if (!inputMethod.currentInputStarted) return false
            val connection = inputMethod.currentInputConnection ?: return false

            // Synchronous call: proves the connection is live, and gives the real characters
            // around the cursor (placeholder text is not part of the editor's content).
            val surrounding = connection.getSurroundingText(1, 1, 0) ?: return false
            val chars = surrounding.text
            val start = surrounding.selectionStart
            val end = surrounding.selectionEnd
            val charBefore = if (start in 1..chars.length) chars[start - 1] else null
            val charAfter = if (end in 0 until chars.length) chars[end] else null

            val insertion = buildString {
                if (charBefore != null && !charBefore.isWhitespace()) append(' ')
                append(text)
                if (charAfter != null && !charAfter.isWhitespace()) append(' ')
            }
            connection.commitText(insertion, 1, null)
            Log.d(TAG, "Inserted text via accessibility input connection")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Input connection insert failed", e)
            false
        }
    }

    private fun insertWithSetText(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            val existing = currentText(node)
            val rawStart = node.textSelectionStart
            val rawEnd = node.textSelectionEnd
            val validSelection = rawStart in 0..existing.length && rawEnd in 0..existing.length
            val start = if (validSelection) minOf(rawStart, rawEnd) else existing.length
            val end = if (validSelection) maxOf(rawStart, rawEnd) else existing.length

            val before = existing.substring(0, start)
            val after = existing.substring(end)
            val insertion = buildString {
                if (before.isNotEmpty() && !before.last().isWhitespace()) append(' ')
                append(text)
                if (after.isNotEmpty() && !after.first().isWhitespace()) append(' ')
            }

            val setTextArgs = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, before + insertion + after)
            }
            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs)) {
                Log.w(TAG, "ACTION_SET_TEXT not supported by this field")
                return false
            }

            val cursor = before.length + insertion.length
            val selectionArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
            Log.d(TAG, "Inserted text via ACTION_SET_TEXT")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_SET_TEXT failed", e)
            false
        }
    }

    /**
     * The field's real text, ignoring placeholder text that empty fields report. Some apps
     * (e.g. WhatsApp's "Message" box) report the placeholder as the text without flagging it.
     */
    private fun currentText(node: AccessibilityNodeInfo): String {
        if (node.isShowingHintText) return ""
        val text = node.text?.toString() ?: return ""
        val cursorAtStart = node.textSelectionStart <= 0 && node.textSelectionEnd <= 0
        val placeholders = listOfNotNull(node.hintText?.toString(), node.contentDescription?.toString())
        if (cursorAtStart && placeholders.any { it == text }) return ""
        return text
    }

    private fun paste(service: AccessibilityService, node: AccessibilityNodeInfo, text: String): Boolean {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Android 10+ usually hides the clipboard from background apps; restore only if we could read it.
        val previous = try {
            clipboard.primaryClip
        } catch (e: Exception) {
            null
        }

        clipboard.setPrimaryClip(ClipData.newPlainText("TalkToMe Transcript", text))
        val pasted = try {
            node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        } catch (e: Exception) {
            false
        }

        if (pasted) {
            Log.d(TAG, "Inserted text via ACTION_PASTE")
            if (previous != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        clipboard.setPrimaryClip(previous)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not restore clipboard", e)
                    }
                }, 800)
            }
        } else {
            Log.w(TAG, "ACTION_PASTE failed")
        }
        return pasted
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("TalkToMe Transcript", text))
    }

    private fun findFocusedEditableIn(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findFocusedEditableIn(child)?.let { return it }
        }
        return null
    }
}
