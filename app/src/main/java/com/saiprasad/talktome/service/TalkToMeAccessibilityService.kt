package com.saiprasad.talktome.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TalkToMeAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TalkToMeAccessibility", "Accessibility Service Connected")
        
        job = scope.launch {
            TranscriptionEventBus.transcriptions.collectLatest { text ->
                injectText(text)
            }
        }
        
        // Start the bubble service
        val intent = android.content.Intent(this, BubbleService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun isKeyboardVisible(): Boolean {
        val windowsList = windows ?: return false
        for (window in windowsList) {
            if (window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true
            }
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val keyboardVisible = isKeyboardVisible()
                val editableFocused = isEditableFocused()
                
                // Show the bubble only if the keyboard is explicitly visible or an editable node is focused
                FocusEventBus.updateFocusState(keyboardVisible || editableFocused)
            }
        }
    }

    private fun isEditableFocused(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = findFocusedNode(rootNode)
        val isEditable = focusedNode?.isEditable == true
        focusedNode?.recycle()
        rootNode.recycle()
        return isEditable
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedNode(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    override fun onInterrupt() {
        Log.w("TalkToMeAccessibility", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun injectText(text: String) {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e("TalkToMeAccessibility", "Root node is null. Cannot inject text.")
            fallbackToClipboard(text)
            return
        }

        val targetNode = findFocusedEditableNode(rootNode)
        if (targetNode != null) {
            Log.d("TalkToMeAccessibility", "Pasting text at cursor...")
            fallbackToPaste(targetNode, text)
        } else {
            Log.e("TalkToMeAccessibility", "No focused editable node found.")
            fallbackToClipboard(text)
        }
        
        rootNode.recycle()
    }

    private fun findFocusedEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // First pass: look for focused and editable
        var result = searchNode(node, requireFocus = true)
        if (result != null) return result
        
        // Second pass: look for any editable
        return searchNode(node, requireFocus = false)
    }

    private fun searchNode(node: AccessibilityNodeInfo, requireFocus: Boolean): AccessibilityNodeInfo? {
        if (node.isEditable && (!requireFocus || node.isFocused)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = searchNode(child, requireFocus)
            if (found != null) {
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun fallbackToPaste(node: AccessibilityNodeInfo, text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("TalkToMe Transcript", text)
        clipboardManager.setPrimaryClip(clipData)
        
        val success = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        if (success) {
            Log.d("TalkToMeAccessibility", "Successfully injected text via ACTION_PASTE")
        } else {
            Log.e("TalkToMeAccessibility", "ACTION_PASTE also failed.")
        }
    }
    
    private fun fallbackToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("TalkToMe Transcript", text)
        clipboardManager.setPrimaryClip(clipData)
        android.widget.Toast.makeText(this, "No text field found. Copied to clipboard.", android.widget.Toast.LENGTH_LONG).show()
    }
}
