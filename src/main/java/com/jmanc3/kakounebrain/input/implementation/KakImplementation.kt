package com.jmanc3.kakounebrain.input.implementation

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.ide.DataManager
import com.intellij.ide.IdeEventQueue
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.actionSystem.ActionPlan
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.content.ContentManagerUtil
import com.intellij.util.PlatformUtils
import com.jmanc3.kakounebrain.KakOnFileOpen
import com.jmanc3.kakounebrain.KeyboardBindings.KakAction
import com.jmanc3.kakounebrain.KeyboardBindings.NormalModeCommands
import com.jmanc3.kakounebrain.PluginStartup
import com.jmanc3.kakounebrain.input.KakInput
//import com.jmanc3.kakounebrain.input.implementation.other.KakFindUtil
import com.jmanc3.kakounebrain.input.implementation.other.State
import java.awt.event.KeyEvent
import java.util.concurrent.Callable

class KakFunctions {

    fun handled(editor: Editor?, charTyped: Char, context: DataContext?): Boolean {
        if (editor == null)
            return false

        val editorState = editor.getUserData(KakOnFileOpen.kakStateKey) ?: return false

        if (editorState.mode == State.Mode.NORMAL) {
            for (normalCommand in NormalModeCommands) {
                val character = normalCommand[0]
                val actionName = normalCommand[1]
                if (normalCommand.size >= 2 && character is String && actionName is String) {
                    if (character[0] == charTyped) {
                        if (editor != null) {
                            executeAction(editor, actionName, true);
                        }
                    }
                }
            }
        }
        return true
    }

    fun hanledBefore(editor: Editor?, c: Char, context: DataContext?, plan: ActionPlan?): Boolean {
        return true
    }

}

private var originalOffset: Int = 0
private var selectionStartBefore: Int = 0
private var onLeftEdge: Boolean = false
private var startedWithSelection: Boolean = false

class KakCommand(val type: String) : AnAction(), DumbAware {

    // Crucial or will get massive lag
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT;
    }

    override fun update(e: AnActionEvent) {
        when (type) {
            KakAction.NEXT_ITEM, KakAction.PREVIOUS_ITEM -> {
                val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
                if (editor == null) {
                    e.presentation.isEnabled = false;
                } else {
                    // if there is no popup, then next item and previous item are disabled so other actions can execute
                    val activeLookup = LookupManager.getActiveLookup(editor)
                    e.presentation.isEnabled = activeLookup != null
                }
            }

            KakAction.CLOSE_MENU -> {
                e.presentation.isEnabled = KakInput.getInstance().someoneWantsKeyPress != null;
            }

            KakAction.ESCAPE_INSERT_MODE -> {
                val editor = CommonDataKeys.EDITOR.getData(e.dataContext)

                if (editor == null) {
                    e.presentation.isEnabled = false;
                } else {
                    val editorState = editor.getUserData(KakOnFileOpen.kakStateKey)
                    if (editorState == null) {
                        e.presentation.isEnabled = false;
                        return
                    }

//                    val activeLookup = LookupManager.getActiveLookup(editor)
                    var shouldInterpretEscapeAsKakEscape = true
//                    if (activeLookup == null) {
//                        shouldInterpretEscapeAsKakEscape = true
//                    } else {
//                        if (!activeLookup.component.isVisible) {
//                            shouldInterpretEscapeAsKakEscape = true
//                            // Check if it's going to the activeLookup is going to become visible
//                            // If it is, then we don't want to interpret escape as kak escape
//
//                            if (activeLookup.)
//
//                        }
//                    }
                    if (editor.selectionModel.hasSelection()) {
                        shouldInterpretEscapeAsKakEscape = false
                    }
                    if (editorState.mode == State.Mode.NORMAL) {
                        shouldInterpretEscapeAsKakEscape = false
                    }

                    e.presentation.isEnabled = shouldInterpretEscapeAsKakEscape;
                }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return
        val editorState = editor.getUserData(KakOnFileOpen.kakStateKey) ?: return

        fun scrollIntoView() {
            val savedSetting = EditorSettingsExternalizable.getInstance().isRefrainFromScrolling
            var overriddenInEditor = false
            try {
                EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = false
                if (editor.settings.isRefrainFromScrolling) {
                    overriddenInEditor = true
                    editor.settings.isRefrainFromScrolling = false
                }
                editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            } finally {
                EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = savedSetting
                if (overriddenInEditor) {
                    editor.settings.isRefrainFromScrolling = true
                }
            }
        }

        when (type) {
            KakAction.SET_MODE_INSERT -> {
                editorState.mode = State.Mode.INSERT
                PluginStartup.removeAllShortcuts()
                PluginStartup.addInsertKakShortcuts()
                PluginStartup.addGlobalKakShortcuts()
                editor.caretModel.runForEachCaret {
                    it.visualAttributes = PluginStartup.INSERT_CARET
                }
            }

            KakAction.SET_MODE_NORMAL -> {
                editorState.mode = State.Mode.NORMAL
                PluginStartup.removeAllShortcuts()
                PluginStartup.addNormalKakShortcuts()
                PluginStartup.addGlobalKakShortcuts()
                editor.caretModel.runForEachCaret {
                    it.visualAttributes = PluginStartup.NORMAL_CARET
                }
            }

            KakAction.ESCAPE_INSERT_MODE -> {
                executeAction(editor, KakAction.SET_MODE_NORMAL, true)
            }

            KakAction.NEXT_WORD -> {
                editor.caretModel.runForEachCaret {
                    seekToken(e, Motion.Right, true, it)
                    val caretModel = editor.caretModel
                    caretModel.moveToOffset(caretModel.offset) // Ensure caret position is updated
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }

            KakAction.PREVIOUS_WORD -> {
                editor.caretModel.runForEachCaret {
                    seekToken(e, Motion.Left, true, it)
                    val caretModel = editor.caretModel
                    caretModel.moveToOffset(caretModel.offset) // Ensure caret position is updated
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }

            KakAction.NEXT_WORD_WITH_SELECTION -> {
                editor.caretModel.runForEachCaret {
                    seekToken(e, Motion.Right, false, it)
                    val caretModel = editor.caretModel
                    caretModel.moveToOffset(caretModel.offset) // Ensure caret position is updated
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }

            KakAction.PREVIOUS_WORD_WITH_SELECTION -> {
                editor.caretModel.runForEachCaret {
                    seekToken(e, Motion.Left, false, it)
                    val caretModel = editor.caretModel
                    caretModel.moveToOffset(caretModel.offset) // Ensure caret position is updated
                    editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
                }
            }

            KakAction.DELETE -> {
                var lineIsEmpty = true
                val lineStart = editor.document.getLineStartOffset(editor.caretModel.primaryCaret.logicalPosition.line)
                val lineEnd = editor.document.getLineEndOffset(editor.caretModel.primaryCaret.logicalPosition.line)
                for (i in lineStart until lineEnd) {
                    if (!editor.document.charsSequence[i].isWhitespace()) {
                        lineIsEmpty = false
                        break
                    }
                }

                // Acts like cut instead
                if (editor.selectionModel.hasSelection()) {
                    executeAction(editor, IdeActions.ACTION_EDITOR_CUT, false)
                } else if (lineIsEmpty) {
                    executeAction(editor, IdeActions.ACTION_EDITOR_DELETE_LINE, false)
                } else {
                    executeAction(editor, IdeActions.ACTION_EDITOR_DELETE, false)
                }
            }

            KakAction.DELETE_LINE -> {
                editor.selectionModel.selectLineAtCaret()
                EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor)
                executeAction(editor, KakAction.SWAP_SELECTION_BOUNDARIES, false)
                executeAction(editor, IdeActions.ACTION_EDITOR_CUT, false)
            }

            KakAction.NO_OP -> {

            }

            KakAction.NEXT_ITEM -> {
                executeAction(editor, "EditorDown", false)
            }

            KakAction.PREVIOUS_ITEM -> {
                executeAction(editor, "EditorUp", false)
            }

            KakAction.PASTE_BEFORE_SELECTION -> {
                val offsetStart = editor.caretModel.offset
                val selectionStart = editor.selectionModel.selectionStart
                val selectionEnd = editor.selectionModel.selectionEnd
                val lenStart = editor.document.textLength

                if (editor.selectionModel.hasSelection()) {
                    // Set cursor at end of selection, end selection
                    if (editor.selectionModel.selectionStart < offsetStart) {
                        executeAction(editor, KakAction.SWAP_SELECTION_BOUNDARIES, false)
                    }
                    editor.selectionModel.removeSelection()
                }
                // TODO: don't use this because it creates an extra undo step we'd prefer not to have
                executeAction(editor, IdeActions.ACTION_EDITOR_PASTE, false)

                val lenEnd = editor.document.textLength
                val change = lenEnd - lenStart
                editor.caretModel.moveToOffset(offsetStart + change)
                editor.selectionModel.setSelection(selectionStart + change, selectionEnd + change)
            }

            KakAction.PASTE_AFTER_SELECTION -> {
                beforeSelectionMovement(editor)
                val offsetStart = editor.caretModel.offset
                val selectionStart = editor.selectionModel.selectionStart
                val selectionEnd = editor.selectionModel.selectionEnd

                if (editor.selectionModel.hasSelection()) {
                    // Set cursor at end of selection, end selection
                    if (offsetStart < selectionEnd) {
                        executeAction(editor, KakAction.SWAP_SELECTION_BOUNDARIES, false)
                    }
                    editor.selectionModel.removeSelection()
                }
                // TODO: don't use this because it creates an extra undo step we'd prefer not to have
                executeAction(editor, IdeActions.ACTION_EDITOR_PASTE, false)

                editor.caretModel.moveToOffset(offsetStart)
                editor.selectionModel.setSelection(selectionStart, selectionEnd)
                afterSelectionMovement(editor)
            }

            KakAction.SELECT_CURRENT_LINE -> {
                EditorActionUtil.moveCaretToLineStartIgnoringSoftWraps(editor)
                executeAction(editor, KakAction.SWAP_SELECTION_BOUNDARIES, false)
            }

            KakAction.GOTO_LINE_END_AND_INSERT -> {
                editor.selectionModel.removeSelection()
                EditorActionUtil.moveCaretToLineEnd(editor, false)
                executeAction(editor, KakAction.SET_MODE_INSERT, false)
            }

            KakAction.CUT_SELECTED_AND_INSERT -> {
                executeAction(editor, KakAction.DELETE, false)
                executeAction(editor, KakAction.SET_MODE_INSERT, false)
            }

            KakAction.NEW_LINE_AFTER -> {
                editor.selectionModel.removeSelection()

                executeAction(editor, IdeActions.ACTION_EDITOR_START_NEW_LINE, false)
                executeAction(editor, KakAction.SET_MODE_INSERT, false)
            }

            KakAction.NEW_LINE_BEFORE -> {
                editor.selectionModel.removeSelection()

                val caretPosition = editor.caretModel.logicalPosition
                val line = caretPosition.line
                val lineStartOffset = editor.document.getLineStartOffset(line)
                editor.caretModel.moveToOffset(lineStartOffset)
                executeAction(editor, IdeActions.ACTION_EDITOR_ENTER, false)
                editor.caretModel.moveToOffset(editor.document.getLineStartOffset(line))
                executeAction(editor, IdeActions.ACTION_EDITOR_MOVE_LINE_END, false)

                editor.selectionModel.removeSelection()
                executeAction(editor, KakAction.SET_MODE_INSERT, false)
            }

            KakAction.TOGGLE_ZEN_MODE -> {
                executeAction(editor, "ToggleZenMode", false)
            }

            KakAction.GO_TO_FILE -> {
                executeAction(editor, "GotoFile", false)
            }

            KakAction.MENU_VIEW -> {
                openViewMenu(editor)
            }

            KakAction.MENU_MISC -> {
                openMiscMenu(editor)
            }

            KakAction.MENU_REFACTOR -> {
                openRefactorMenu(editor)
            }

            KakAction.PLAY_PREVIOUS_MACRO -> {
                val kakInput = KakInput.getInstance()

                if (kakInput.recordingMacro)
                    return;

                for (macroAction in kakInput.macroActions) {
                    if (macroAction.type == KakInput.MacroActionsType.Text) {
                        for (c in macroAction.text.toCharArray()) {
                            if (c == 'Q' && editorState.mode == State.Mode.NORMAL)
                                continue;
                            // TODO: it should execute not in the editor but in whatever the current context is of the project
                            val instance = TextEditorProvider.getInstance()

                            // get the current editor
//                            var current_editor = instance.getTextEditor();

                            val instance1 = FileEditorManager.getInstance(editor.project!!)
                            val selectedFileEditor = instance1.selectedEditor
                            if (selectedFileEditor is TextEditor) {
                                val selectedEditor = (selectedFileEditor as TextEditor).editor
                                kakInput.execute(selectedEditor, c, e.dataContext);
                            }
//                            TypedAction.getInstance().actionPerformed(editor, c, e.dataContext)
                        }
                    } else if (macroAction.type == KakInput.MacroActionsType.Shortcut) {
                        val instance = IdeEventQueue.getInstance()
                        val keyEventDispatcher = instance.keyEventDispatcher
                        val event = macroAction.event
                        val non_consumed_event = KeyEvent(
                            event.component,
                            event.id,
                            event.`when`,
                            event.modifiers,
                            event.keyCode,
                            event.keyChar,
                            event.keyLocation
                        );
                        if (non_consumed_event.keyChar == 'Q' && editorState.mode == State.Mode.NORMAL && !non_consumed_event.isAltDown && !non_consumed_event.isControlDown)
                            continue
                        keyEventDispatcher.dispatchKeyEvent(non_consumed_event);
                    } else if (macroAction.type == KakInput.MacroActionsType.ActionText) {
                        executeAction(editor, macroAction.text);
                    }
                }
            }

            KakAction.START_STOP_MACRO -> {
                val kakInput = KakInput.getInstance()
                kakInput.recordingMacro = !kakInput.recordingMacro

                if (kakInput.recordingMacro) {
                    KakInput.getInstance().macroRenderer.showIt(editor);
                    kakInput.macroActions.clear()
                } else {
                    KakInput.getInstance().macroRenderer.hideIt(editor);
                }
            }

            KakAction.FIND_FORWARD_COVER -> {
                openLetterFindMenu(editor, KakAction.FIND_FORWARD_COVER)
            }

            KakAction.FIND_FORWARD_AHEAD -> {
                openLetterFindMenu(editor, KakAction.FIND_FORWARD_AHEAD)
            }

            KakAction.FIND_REVERSE_COVER -> {
                openLetterFindMenu(editor, KakAction.FIND_REVERSE_COVER);
            }

            KakAction.FIND_REVERSE_AHEAD -> {
                openLetterFindMenu(editor, KakAction.FIND_REVERSE_AHEAD);
            }

            KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION -> {
                openLetterFindMenu(editor, KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION);
            }

            KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION -> {
                openLetterFindMenu(editor, KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION);
            }

            KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION -> {
                openLetterFindMenu(editor, KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION);
            }

            KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION -> {
                openLetterFindMenu(editor, KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION);
            }

            KakAction.MOVE_UP_10 -> {
                val column = editor.caretModel.logicalPosition.column
                val pos = LogicalPosition((editor.caretModel.logicalPosition.line - 10).coerceAtLeast(0), column)
                editor.caretModel.moveToLogicalPosition(pos)
                scrollIntoView()
                editor.selectionModel.removeSelection()
            }

            KakAction.MOVE_DOWN_10 -> {
                val column = editor.caretModel.logicalPosition.column
                val pos = LogicalPosition(editor.caretModel.logicalPosition.line + 10, column)
                editor.caretModel.moveToLogicalPosition(pos)
                scrollIntoView()
                editor.selectionModel.removeSelection()
            }

            KakAction.MOVE_UP_10_WITH_SELECTION -> {
                beforeSelectionMovement(editor)
                val selectionStart = editor.selectionModel.leadSelectionOffset
                val column = editor.caretModel.logicalPosition.column
                val pos = LogicalPosition((editor.caretModel.logicalPosition.line - 10).coerceAtLeast(0), column)
                editor.caretModel.moveToLogicalPosition(pos)
                scrollIntoView()
                editor.selectionModel.setSelection(
                    selectionStart, editor.caretModel.visualPosition, editor.caretModel.offset
                )
                afterSelectionMovement(editor)
            }

            KakAction.MOVE_DOWN_10_WITH_SELECTION -> {
                beforeSelectionMovement(editor)
                val selectionStart = editor.selectionModel.leadSelectionOffset
                val column = editor.caretModel.logicalPosition.column
                val pos = LogicalPosition(editor.caretModel.logicalPosition.line + 10, column)
                editor.caretModel.moveToLogicalPosition(pos)
                scrollIntoView()
                editor.selectionModel.setSelection(
                    selectionStart, editor.caretModel.visualPosition, editor.caretModel.offset
                )
                afterSelectionMovement(editor)
            }

            KakAction.SAVE_MODE -> {
                editorState.savedMode = editorState.mode;
            }

            KakAction.RELOAD_MODE -> {
                if (editorState.savedMode == State.Mode.INSERT) {
                    executeAction(editor, KakAction.SET_MODE_INSERT, false)
                } else if (editorState.savedMode == State.Mode.NORMAL) {
                    executeAction(editor, KakAction.SET_MODE_NORMAL, false)
                }
            }

            KakAction.CLOSE_ALL_NON_PROJECT_TABS -> {
                val contentManager = ContentManagerUtil.getContentManagerFromContext(e.dataContext, true)
                var processed = false
                if (contentManager != null && contentManager.canCloseContents()) {
                    val selectedContent = contentManager.selectedContent
                    if (selectedContent != null && selectedContent.isCloseable) {
                        contentManager.removeContent(selectedContent, true)
                        processed = true
                    }
                }

                if (!processed && contentManager != null) {
                    val context = DataManager.getInstance().getDataContext(contentManager.component)
                    val toolWindow = PlatformDataKeys.TOOL_WINDOW.getData(context)
                    toolWindow?.hide(null)
                }
            }

            KakAction.NEXT_INSTANCE_OF_SELECTION -> {
                KakFindUtildoSearch(editor, true)
            }

            KakAction.PREVIOUS_INSTANCE_OF_SELECTION -> {
                KakFindUtildoSearch(editor, false)
            }

            KakAction.CLOSE_MENU -> {
                KakInput.getInstance().menuRenderer.hideIt(editor)
                KakInput.getInstance().someoneWantsKeyPress = null
                if (editorState.mode == State.Mode.INSERT) {
                    executeAction(editor, KakAction.SET_MODE_INSERT, false)
                } else {
                    executeAction(editor, KakAction.SET_MODE_NORMAL, false)
                }
            }

            KakAction.SWAP_SELECTION_BOUNDARIES -> {
                editor.caretModel.runForEachCaret {
                    if (!it.hasSelection()) return@runForEachCaret

                    val start: Int = it.selectionStart
                    val end: Int = it.selectionEnd
                    val moveToEnd = it.offset == start

                    if (editor is EditorEx) {
                        if (editor.isStickySelection) {
                            editor.isStickySelection = false
                            editor.isStickySelection = true
                        }
                    }

                    if (moveToEnd) {
                        it.moveToOffset(end - 1)
                    } else {
                        it.moveToOffset(start)
                    }
                }
            }

            KakAction.MENU_OPEN_PANEL -> {
                openPanelMenu(editor, e)
            }

            KakAction.GOTO_MENU_WITH_SELECTION -> {
                openGotoMenu(true, editor)
            }

            KakAction.GOTO_MENU -> {
                openGotoMenu(false, editor)
            }

            "KakEditorForwardParagraphWithSelection" -> {
                moveWithSelection(editor, "BackupEditorForwardParagraphWithSelection");
            }

            "KakEditorBackwardParagraphWithSelection" -> {
                moveWithSelection(editor, "BackupEditorBackwardParagraphWithSelection");
            }

            "KakEditorTextStartWithSelection" -> {
                moveWithSelection(editor, "BackupEditorTextStartWithSelection");
            }

            "KakEditorTextEndWithSelection" -> {
                moveWithSelection(editor, "BackupEditorTextEndWithSelection");
            }

            "KakEditorLineStartWithSelection" -> {
                moveWithSelection(editor, "BackupEditorLineStartWithSelection");
            }

            "KakEditorLineEndWithSelection" -> {
                moveWithSelection(editor, "BackupEditorLineEndWithSelection");
            }

            "KakEditorLeftWithSelection" -> {
                moveWithSelection(editor, "BackupEditorLeftWithSelection");
            }

            "KakEditorRightWithSelection" -> {
                moveWithSelection(editor, "BackupEditorRightWithSelection");
            }

            "KakEditorUpWithSelection" -> {
                moveWithSelection(editor, "BackupEditorUpWithSelection");
            }

            "KakEditorDownWithSelection" -> {
                moveWithSelection(editor, "BackupEditorDownWithSelection");
            }

            "KakEditorPageUpWithSelection" -> {
                moveWithSelection(editor, "BackupEditorPageUpWithSelection");
            }

            "KakEditorPageDownWithSelection" -> {
                moveWithSelection(editor, "BackupEditorPageDownWithSelection");
            }

            "KakEditorNextWordWithSelection" -> {
                moveWithSelection(editor, "BackupEditorNextWordWithSelection");
            }

            "KakEditorPreviousWordWithSelection" -> {
                moveWithSelection(editor, "BackupEditorPreviousWordWithSelection");
            }

        }
    }

    fun scrollToCenter(editor: Editor) {
        val savedSetting = EditorSettingsExternalizable.getInstance().isRefrainFromScrolling
        var overriddenInEditor = false
        try {
            EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = false
            if (editor.settings.isRefrainFromScrolling) {
                overriddenInEditor = true
                editor.settings.isRefrainFromScrolling = false
            }
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        } finally {
            EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = savedSetting
            if (overriddenInEditor) {
                editor.settings.isRefrainFromScrolling = true
            }
        }
    }

    private fun ensureCaretFullyVisible(editor: Editor) {
        val caretModel = editor.caretModel
        val scrollingModel = editor.scrollingModel
        val visibleArea = scrollingModel.visibleArea

        // Get the caret's position and translate to pixel coordinates
        val caretPosition = caretModel.visualPosition
        val caretPoint = editor.visualPositionToXY(caretPosition)

        // Check if the caret is outside the visible area
        if (!visibleArea.contains(caretPoint)) {
            // Scroll to bring the caret into view
            scrollingModel.scrollToCaret(ScrollType.RELATIVE)
        }

        // Additional horizontal scrolling if needed
        if (caretPoint.x < visibleArea.x || caretPoint.x > visibleArea.x + visibleArea.width) {
            scrollingModel.scrollHorizontally(caretPoint.x)
        }
    }

    private fun moveWithSelection(editor: Editor, originalAction: String) {
        // replace all ide with selection moves with a Kak_ action which after executing the original move,
        // makes the original offset+1, and the current offset+1 a part of the selection.
        // if the new total selection size is 1, it removes the selection.
        beforeSelectionMovement(editor)
        executeAction(editor, originalAction, false)
        afterSelectionMovement(editor)

        ensureCaretFullyVisible(editor)
    }

    private fun textMatchesAt(text: String, pattern: CharArray, startIndex: Int): Boolean {
        if (startIndex + pattern.size > text.length) {
            // If the pattern went out of bounds, it can't match
            return false
        }

        for (i in pattern.indices) {
            if (text[startIndex + i] != pattern[i]) {
                return false
            }
        }
        return true
    }

    private var previous_search: CharArray = charArrayOf()

    private fun KakFindUtildoSearch(editor: Editor, forward: Boolean) {
        val text = editor.document.text
        for (caret in editor.caretModel.allCarets) {
            val currentCaretOffset = caret.offset
            var selectionStart = caret.selectionStart
            var selectionEnd = caret.selectionEnd
            var selectionText = editor.document.text.toCharArray(selectionStart, selectionEnd)
            if (selectionStart == selectionEnd && previous_search.isNotEmpty()) {
                selectionText = previous_search
                selectionStart = 0
                selectionEnd = previous_search.size
            }
            previous_search = selectionText

            for (i in text.indices) {
                var index = i + currentCaretOffset + 1
                if (index > text.length - 1)
                    index -= text.length
                if (!forward) {
                    index = currentCaretOffset - i - (selectionEnd - selectionStart) - 1
                    if (index < 0)
                        index += text.length
                }

                if (textMatchesAt(text, selectionText, index)) {
                    caret.setSelection(index, index + (selectionEnd - selectionStart))
                    if (currentCaretOffset == selectionStart) {
                        caret.moveToOffset(index)
                    } else {
                        caret.moveToOffset(index + (selectionEnd - selectionStart) - 1)
                    }
                    break
                }
            }
        }
        val scrollingModel = editor.scrollingModel
        val scrollType = if (forward) ScrollType.CENTER_DOWN else ScrollType.CENTER_UP
        scrollingModel.scrollToCaret(scrollType)
    }

    private fun openMiscMenu(editor: Editor) {
        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("misc")
        KakInput.getInstance().menuRenderer.text.add("j: extend caret down")
        KakInput.getInstance().menuRenderer.text.add("k: extend caret up")
        KakInput.getInstance().menuRenderer.text.add("h: add carets to end of selected lines")
        KakInput.getInstance().menuRenderer.text.add("l: add carets to start of selected lines")
        KakInput.getInstance().menuRenderer.text.add("g: copilot generate")

        KakInput.getInstance().menuRenderer.showIt(editor)

        KakInput.getInstance().someoneWantsKeyPress = Callable {

            var ate = true
            when (KakInput.getInstance().c) {
                'j' -> {
                    val column = editor.caretModel.logicalPosition.column
                    val pos = LogicalPosition(editor.caretModel.logicalPosition.line + 1, column)
                    editor.caretModel.addCaret(pos, true)
                }

                'k' -> {
                    val column = editor.caretModel.logicalPosition.column
                    val pos = LogicalPosition(editor.caretModel.logicalPosition.line - 1, column)
                    editor.caretModel.addCaret(pos, true)
                }

                'l' -> {
                    executeAction(editor, "EditorAddCaretPerSelectedLine", false);
                    editor.caretModel.runForEachCaret {
                        // if line is empty (no non whitespace chars) remove the caret
                        var lineIsEmpty = true
                        val lineStart = editor.document.getLineStartOffset(it.logicalPosition.line)
                        val lineEnd = editor.document.getLineEndOffset(it.logicalPosition.line)
                        for (i in lineStart until lineEnd) {
                            if (!editor.document.charsSequence[i].isWhitespace()) {
                                lineIsEmpty = false
                                break
                            }
                        }

                        if (lineIsEmpty) {
                            editor.caretModel.removeCaret(it)
                        }
                    }
                }

                'h' -> {
                    executeAction(editor, "EditorAddCaretPerSelectedLine", false);
                    executeAction(editor, IdeActions.ACTION_EDITOR_MOVE_LINE_START, false)

                    editor.caretModel.runForEachCaret {
                        // if line is empty (no non whitespace chars) remove the caret
                        var lineIsEmpty = true
                        val lineStart = editor.document.getLineStartOffset(it.logicalPosition.line)
                        val lineEnd = editor.document.getLineEndOffset(it.logicalPosition.line)
                        for (i in lineStart until lineEnd) {
                            if (!editor.document.charsSequence[i].isWhitespace()) {
                                lineIsEmpty = false
                                break
                            }
                        }

                        if (lineIsEmpty) {
                            editor.caretModel.removeCaret(it)
                        }
                    }
                }

                'g' -> {
                    // Make a list of lines current, above, and below that contains as much of the current method text that will fit in 1080
                    // Always include method header
                    // Insert // ... Where lines had to be removed
                    // Only include 12+ after lines or until method body end
                    // Where cursor is put <REPLACE_ME_WITH_CODE>
                    // send the following request to llama.cpp deepcoder
                    /*
                    Replace <REPLACE_ME_WITH_CODE> in the following c++:
                    ```c++
                    void method_header() {
                        // ...
                        stuff
                        <REPLACE_ME_WITH_CODE>
                        stuff
                    }
                    ```

                    The code that should go in <REPLACE_ME_WITH_CODE> is the following:


                    */
                    // Use a ```c++ text ``` grammar
                }


                else -> {
                    ate = false;
                }
            }

            executeAction(editor, KakAction.CLOSE_MENU, false)

            ate
        }
    }

    private fun openLetterFindMenu(editor: Editor, type: String) {
        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("find: $type")

        KakInput.getInstance().menuRenderer.showIt(editor)

        KakInput.getInstance().someoneWantsKeyPress = Callable {
            val key = KakInput.getInstance().c

            val text = editor.document.text
            for (caret in editor.caretModel.allCarets) {
                val currentCaretOffset = caret.offset
                val selectionStart = caret.selectionStart
                val selectionEnd = caret.selectionEnd

                var keyIndex = -1
                if (type == KakAction.FIND_REVERSE_COVER || type == KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION) {
                    keyIndex = text.lastIndexOf(key, currentCaretOffset - 1)
                } else if (type == KakAction.FIND_REVERSE_AHEAD || type == KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION) {
                    keyIndex = text.lastIndexOf(key, currentCaretOffset - 2)
                } else if (type == KakAction.FIND_FORWARD_AHEAD || type == KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION) {
                    keyIndex = text.indexOf(key, currentCaretOffset + 2)
                } else if (type == KakAction.FIND_FORWARD_COVER || type == KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION) {
                    keyIndex = text.indexOf(key, currentCaretOffset + 1)
                }

                if (keyIndex != -1 && keyIndex != currentCaretOffset) {
                    if (type == KakAction.FIND_REVERSE_COVER || type == KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION) {
                        if (type == KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION) {
                            if (caret.hasSelection()) {
                                caret.setSelection(selectionEnd, keyIndex)
                            } else {
                                caret.setSelection(currentCaretOffset, keyIndex)
                            }
                            caret.moveToOffset(keyIndex)
                        } else {
                            caret.moveToOffset(keyIndex)
                        }
                    } else if (type == KakAction.FIND_REVERSE_AHEAD || type == KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION) {
                        if (type == KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION) {
                            if (caret.hasSelection()) {
                                caret.setSelection(selectionEnd, keyIndex + 1)
                            } else {
                                caret.setSelection(currentCaretOffset, keyIndex + 1)
                            }
                            caret.moveToOffset(keyIndex + 1)
                        } else {
                            caret.moveToOffset(keyIndex + 1)
                        }
                    } else if (type == KakAction.FIND_FORWARD_AHEAD || type == KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION) {
                        if (type == KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION) {
                            if (caret.hasSelection()) {
                                caret.setSelection(selectionStart, keyIndex)
                            } else {
                                caret.setSelection(currentCaretOffset, keyIndex)
                            }
                            caret.moveToOffset(keyIndex - 1)
                        } else {
                            caret.moveToOffset(keyIndex - 1)
                        }
                    } else if (type == KakAction.FIND_FORWARD_COVER || type == KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION) {
                        if (type == KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION) {
                            if (caret.hasSelection()) {
                                caret.setSelection(selectionStart, keyIndex + 1)
                            } else {
                                caret.setSelection(currentCaretOffset, keyIndex + 1)
                            }
                            caret.moveToOffset(keyIndex)
                        } else {
                            caret.moveToOffset(keyIndex)
                        }
                    }
                }
            }

            executeAction(editor, KakAction.CLOSE_MENU, false)

            true
        }
    }

    private fun openViewMenu(editor: Editor) {
        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("view")
        KakInput.getInstance().menuRenderer.text.add("v,c: center cursor (vertically)")
        KakInput.getInstance().menuRenderer.text.add("m:   center cursor (horizontally)")
        KakInput.getInstance().menuRenderer.text.add("t:   cursor on top")
        KakInput.getInstance().menuRenderer.text.add("b:   cursor on bottom")
        KakInput.getInstance().menuRenderer.text.add("h:   scroll left")
        KakInput.getInstance().menuRenderer.text.add("j:   scroll down")
        KakInput.getInstance().menuRenderer.text.add("k:   scroll up")
        KakInput.getInstance().menuRenderer.text.add("l:   scroll right")

        KakInput.getInstance().menuRenderer.showIt(editor)

        KakInput.getInstance().someoneWantsKeyPress = Callable {
            false
        }
    }

    private fun openRefactorMenu(editor: Editor) {
        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("refactor")

        KakInput.getInstance().menuRenderer.showIt(editor)

        KakInput.getInstance().someoneWantsKeyPress = Callable {
            false
        }
    }

    private fun openGotoMenu(increaseSelection: Boolean, editor: Editor) {
        val editorState = editor.getUserData(KakOnFileOpen.kakStateKey) ?: return

        beforeSelectionMovement(editor)

        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("goto")
        KakInput.getInstance().menuRenderer.text.add("g,k: buffer top")
        KakInput.getInstance().menuRenderer.text.add("l:   line end")
        KakInput.getInstance().menuRenderer.text.add("h:   line begin")
        KakInput.getInstance().menuRenderer.text.add("i:   line (text) begin")
        KakInput.getInstance().menuRenderer.text.add("j:   buffer bottom")
        KakInput.getInstance().menuRenderer.text.add("e:   buffer end")
        KakInput.getInstance().menuRenderer.text.add("t:   window top")
        KakInput.getInstance().menuRenderer.text.add("b:   window bottom")
        KakInput.getInstance().menuRenderer.text.add("c:   window center")
        KakInput.getInstance().menuRenderer.text.add("a:   previous file")
        if (PlatformUtils.isCLion()) {
            KakInput.getInstance().menuRenderer.text.add("m:   matching .cpp/.h")
        }
        KakInput.getInstance().menuRenderer.text.add("f:   class under cursor")
        KakInput.getInstance().menuRenderer.text.add(".:   last text change")
        KakInput.getInstance().menuRenderer.text.add("d:   declaration/usage")
        KakInput.getInstance().menuRenderer.showIt(editor)

        fun scrollToCenter() {
            val savedSetting = EditorSettingsExternalizable.getInstance().isRefrainFromScrolling
            var overriddenInEditor = false
            try {
                EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = false
                if (editor.settings.isRefrainFromScrolling) {
                    overriddenInEditor = true
                    editor.settings.isRefrainFromScrolling = false
                }
                editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            } finally {
                EditorSettingsExternalizable.getInstance().isRefrainFromScrolling = savedSetting
                if (overriddenInEditor) {
                    editor.settings.isRefrainFromScrolling = true
                }
            }
        }

        var wasMovement = false

        KakInput.getInstance().someoneWantsKeyPress = Callable {
            val selectionStart = editor.selectionModel.leadSelectionOffset

            var ate = true
            when (KakInput.getInstance().c) {
                'g', 'k' -> {
                    editor.caretModel.moveToOffset(0)
                    editor.caretModel.removeSecondaryCarets();
                    scrollToCenter()
                    wasMovement = true
                }

                'l' -> {
                    executeAction(editor, IdeActions.ACTION_EDITOR_MOVE_LINE_END, false)
                    wasMovement = true
                }

                'h' -> {
                    // TODO: this should go to 0 index not text start
                    executeAction(editor, IdeActions.ACTION_EDITOR_MOVE_LINE_START, false)
                    wasMovement = true
                }

                'i' -> {
                    executeAction(editor, IdeActions.ACTION_EDITOR_MOVE_LINE_START, false)
                    wasMovement = true
                }

                'e' -> {
                    editor.caretModel.moveToOffset(editor.document.textLength)
                    editor.caretModel.removeSecondaryCarets();
                    scrollToCenter()
                    wasMovement = true
                }

                't' -> {
                    EditorActionUtil.moveCaretPageTop(editor, increaseSelection)
                    editor.caretModel.removeSecondaryCarets();
                    wasMovement = true
                }

                'j' -> {
                    val column = editor.caretModel.logicalPosition.column
                    val pos = LogicalPosition(editor.document.lineCount, column)
                    editor.caretModel.moveToLogicalPosition(pos)
                    scrollToCenter()
                    editor.caretModel.removeSecondaryCarets();
                    wasMovement = true
                }

                'b' -> {
                    EditorActionUtil.moveCaretPageBottom(editor, increaseSelection)
                    editor.caretModel.removeSecondaryCarets();
                    wasMovement = true
                }

                'c' -> {
                    val visibleArea =
                        if (EditorCoreUtil.isTrueSmoothScrollingEnabled()) editor.scrollingModel.visibleAreaOnScrollingFinished else editor.scrollingModel.visibleArea

                    val lineNumber = editor.yToVisualLine(visibleArea.y + visibleArea.height / 2)

                    val pos = VisualPosition(lineNumber, editor.caretModel.visualPosition.column)
                    editor.caretModel.moveToVisualPosition(pos)
                    editor.caretModel.removeSecondaryCarets();
                    wasMovement = true
                }

                'a' -> {
                    executeAction(editor, IdeActions.ACTION_PREVIOUS_EDITOR_TAB, false)
                }

                'm' -> {
                    if (PlatformUtils.isCLion()) {
                        executeAction(editor, "CIDR.Lang.SwitchHeaderSource", false);
                    } else {
                        ate = false;
                    }
                }

                'f' -> {
                    executeAction(editor, IdeActions.ACTION_GOTO_DECLARATION, false)
                }

                '.' -> {
                    editor.caretModel.moveToOffset(editorState.previousTextChange)
                    scrollToCenter()
                    wasMovement = true
                }

                'd' -> {
                    executeAction(editor, IdeActions.ACTION_GOTO_DECLARATION, false)
                }

                else -> {
                    ate = false;
                }
            }

            if (ate) {
                if (increaseSelection) {
                    editor.selectionModel.setSelection(
                        selectionStart, editor.caretModel.visualPosition, editor.caretModel.offset
                    )
                    if (wasMovement) afterSelectionMovement(editor)
                } else {
                    editor.selectionModel.removeSelection()
                }
            }

            executeAction(editor, KakAction.CLOSE_MENU, false)

            ate
        }
    }

    private fun openPanelMenu(editor: Editor, e: AnActionEvent) {
        KakInput.getInstance().menuRenderer.text.clear()
        KakInput.getInstance().menuRenderer.text.add("panels")
        KakInput.getInstance().menuRenderer.text.add("p: close panel")
        KakInput.getInstance().menuRenderer.text.add("h: split left")
        KakInput.getInstance().menuRenderer.text.add("j: split below")
        KakInput.getInstance().menuRenderer.text.add("k: split above")
        KakInput.getInstance().menuRenderer.text.add("l: split right")
        KakInput.getInstance().menuRenderer.text.add("s: swap panels")

        KakInput.getInstance().menuRenderer.showIt(editor)

        KakInput.getInstance().someoneWantsKeyPress = Callable {
            var ate = true
            var prev = false
            var closeEditor = false
            when (KakInput.getInstance().c) {
                'p' -> {
                    closeEditor = true
                }

                'h' -> {
                    val window: com.intellij.openapi.fileEditor.impl.EditorWindow =
                        e.getRequiredData(com.intellij.openapi.fileEditor.impl.EditorWindow.DATA_KEY)
                    val file = window.selectedFile
                    if (file != null) {
                        window.split(1, false, file, false)
                        window.requestFocus(true);
                        prev = true
                    }
                }

                'j' -> {
                    val window: com.intellij.openapi.fileEditor.impl.EditorWindow =
                        e.getRequiredData(com.intellij.openapi.fileEditor.impl.EditorWindow.DATA_KEY)
                    val file = window.selectedFile
                    if (file != null) {
                        window.split(0, true, file, true)
                    }
                }

                'k' -> {
                    val window: com.intellij.openapi.fileEditor.impl.EditorWindow =
                        e.getRequiredData(com.intellij.openapi.fileEditor.impl.EditorWindow.DATA_KEY)
                    val file = window.selectedFile
                    if (file != null) {
                        window.split(0, true, file, false)
                        window.requestFocus(true);
                        prev = true
                    }
                }

                'l' -> {
                    IdeActions.ACTION_OPEN_IN_RIGHT_SPLIT;
                    val window: com.intellij.openapi.fileEditor.impl.EditorWindow =
                        e.getRequiredData(com.intellij.openapi.fileEditor.impl.EditorWindow.DATA_KEY)
                    val file = window.selectedFile
                    if (file != null) {
                        window.split(1, true, file, true)
                    }
                }

                's' -> {

                }

                else -> {
                    ate = false;
                }
            }

            executeAction(editor, KakAction.CLOSE_MENU, false)
            if (closeEditor) {
                executeAction(editor, "CloseAllEditors")

                val window: com.intellij.openapi.fileEditor.impl.EditorWindow =
                    e.getRequiredData(com.intellij.openapi.fileEditor.impl.EditorWindow.DATA_KEY)
                val file = window.selectedFile
                if (file != null) {
                    window.requestFocus(true)
                }
            }
//            if (prev)
//                ApplicationManager.getApplication().invokeLater {
//                    val e = CommonDataKeys.EDITOR.getData(e.dataContext)
//                    if (e == null) {
//                        executeAction(e, "PrevSplitter", true)
//                    }
//                }

            ate
        }
    }
}

fun executeAction(editor: Editor, actionId: String) {
    executeAction(editor, actionId, true)
}

private fun createEditorContext(editor: Editor): DataContext {
    val hostEditor = if (editor is EditorWindow) editor.delegate else editor
    val parent = DataManager.getInstance().getDataContext(editor.contentComponent)
    return SimpleDataContext.builder().setParent(parent).add(CommonDataKeys.HOST_EDITOR, hostEditor)
        .add(CommonDataKeys.EDITOR, editor).build()
}

fun executeAction(editor: Editor, actionId: String, assertActionIsEnabled: Boolean) {
    val actionManager = ActionManagerEx.getInstanceEx()
    val action = actionManager.getAction(actionId)
//    Assert.assertNotNull(action)

    val event = AnActionEvent.createFromAnAction(action, null, "", createEditorContext(editor))
    if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
        ActionUtil.performActionDumbAwareWithCallbacks(action, event)
    } else if (assertActionIsEnabled) {
//        Assert.fail("Action $action is disabled")
    }
}


private fun beforeSelectionMovement(editor: Editor) {
    originalOffset = editor.caretModel.offset
    selectionStartBefore = editor.selectionModel.selectionStart
    onLeftEdge = (originalOffset == editor.selectionModel.selectionStart)
    startedWithSelection = editor.selectionModel.hasSelection()
}

private fun afterSelectionMovement(editor: Editor) {
    val postOffset = editor.caretModel.offset
    val movedRight = postOffset > originalOffset;

    if (startedWithSelection) {
        if (movedRight) {
            if (!onLeftEdge) {
                editor.selectionModel.setSelection(
                    selectionStartBefore.coerceAtLeast(0), (postOffset + 1).coerceAtMost(editor.document.textLength)
                )
            }
        } else {
            if (!onLeftEdge) {
                editor.selectionModel.setSelection(
                    selectionStartBefore.coerceAtLeast(0), (postOffset + 1).coerceAtMost(editor.document.textLength)
                )
            }
        }
    } else {
        if (movedRight) {
            editor.selectionModel.setSelection(
                editor.selectionModel.selectionStart.coerceAtLeast(0),
                (postOffset + 1).coerceAtMost(editor.document.textLength)
            )
        } else {
            editor.selectionModel.setSelection(
                editor.selectionModel.selectionStart.coerceAtLeast(0),
                (originalOffset + 1).coerceAtMost(editor.document.textLength)
            )
        }
    }

//    if (editor.selectionModel.hasSelection()) {
//        if (editor.selectionModel.selectionEnd - editor.selectionModel.selectionStart == 1) {
//            editor.selectionModel.removeSelection()
//        }
//    }
}


private val tokensList: CharArray = charArrayOf(
    '[',
    ']',
    '|',
    '(',
    ')',
    '{',
    '}',
    ';',
    '.',
    '!',
    '@',
    '#',
    '$',
    '%',
    '^',
    '&',
    '*',
    '-',
    '=',
    '+',
    ':',
    '\'',
    '\"',
    '<',
    '>',
    '?',
    '|',
    '\\',
    '/',
    ',',
    '`',
    '~'
)

private enum class Motion(i: Int) {
    Left(-1), Right(1)
}

private enum class Group(i: Int) {
    Edge(0), Space(1), Token(3), Normal(4),
}

private fun seekToken(event: AnActionEvent, motionDirection: Motion, shouldUpdateSelection: Boolean, caret: Caret) {
    val editor = event.getData(CommonDataKeys.EDITOR) ?: return

    fun tokenType(pos: Int): Group {
        if (pos < 0 || pos >= editor.document.textLength) return Group.Edge

        val character = try {
            editor.document.charsSequence[pos]
        } catch (ignored: Exception) {
            return Group.Edge
        }

        if (character == ' ' || character == '\t' || character == '\n' || character == '\r') return Group.Space
        for (token in tokensList) {
            if (token == character) return Group.Token
        }

        return Group.Normal
    }

    fun getTokenRight(startPos: Int): IntRange {
        val originalTokenType = tokenType(startPos)
        if (originalTokenType == Group.Edge) {
            return IntRange(startPos - 1, startPos)
        }
        var pos = startPos
        while (tokenType(++pos) == originalTokenType) {

        }
        return IntRange(startPos, pos)
    }

    fun getTokenLeft(startPos: Int): IntRange {
        val originalTokenType = tokenType(startPos - 1)
        if (originalTokenType == Group.Edge) {
            return IntRange(startPos - 1, startPos)
        }
        var pos = startPos
        while (tokenType(pos - 1) == originalTokenType) {
            pos--
        }
        return IntRange(pos, startPos)
    }

    val startOffset = caret.offset

    val startingGroup = tokenType(startOffset)

    val inToken = tokenType(startOffset + 1) == startingGroup && tokenType(startOffset - 1) == startingGroup
    val atRightBoundary = tokenType(startOffset + 1) != startingGroup
    val atLeftBoundary = tokenType(startOffset - 1) != startingGroup

    var newSelection = IntRange(0, 0)
    var newCaret = 0

    if (inToken) {
        if (motionDirection == Motion.Right) {
            newSelection = getTokenRight(startOffset)
            newCaret = newSelection.last - 1
        } else if (motionDirection == Motion.Left) {
            newSelection = getTokenLeft(startOffset + 1)
            newCaret = newSelection.first
        }
    } else { // At edge
        if (motionDirection == Motion.Right) {
            if (atRightBoundary) {
                newSelection = getTokenRight(startOffset + 1)
            } else if (atLeftBoundary) {
                newSelection = getTokenRight(startOffset)
            }
            newCaret = newSelection.last - 1
        } else if (motionDirection == Motion.Left) {
            if (atLeftBoundary) {
                newSelection = getTokenLeft(startOffset)
            } else if (atRightBoundary) {
                newSelection = getTokenLeft(startOffset + 1)
            }
            newCaret = newSelection.first
        }
    }

    val oldSelection = IntRange(caret.selectionStart, caret.selectionEnd)
    val min = newSelection.min()
    val max = newSelection.max()
    newSelection = IntRange(min.coerceAtLeast(0), max.coerceAtMost(editor.document.textLength))
    newCaret = newCaret.coerceAtLeast(0).coerceAtMost(editor.document.textLength)

    caret.moveToOffset(newCaret)
    if (shouldUpdateSelection) {
        caret.setSelection(newSelection.min(), newSelection.max())
    } else {
        // The newSelection has to be added to be merged with the old selection
        if (newSelection != oldSelection) {
            val shrinking = oldSelection.first <= newCaret && newCaret < oldSelection.last

            if (shrinking) {
                if (motionDirection == Motion.Right) {
                    caret.setSelection(
                        newCaret, oldSelection.max().coerceAtLeast(newSelection.max())
                    )
                } else {
                    caret.setSelection(
                        oldSelection.min().coerceAtMost(newSelection.min()), newCaret + 1
                    )
                }
            } else {
                if (motionDirection == Motion.Right) {
                    caret.setSelection(
                        oldSelection.min().coerceAtMost(newSelection.min()),
                        newCaret + 1,
                    )
                } else if (motionDirection == Motion.Left) {
                    caret.setSelection(
                        newCaret, oldSelection.max().coerceAtLeast(newSelection.max())
                    )
                }
            }
        }
    }

    // if range wasOnlyWhitespace rerun
    var foundSpace = false
    var foundNewLine = false
    for (i in newSelection.min() until newSelection.max()) {
        val character = editor.document.text[i]
        if (character == ' ' || character == '\t') foundSpace = true
        if (character == '\n' || character == '\r') foundNewLine = true
    }
    if (foundSpace && !foundNewLine) {
        if (motionDirection == Motion.Right) {
            if (newSelection.max() < editor.document.textLength) {
                seekToken(event, motionDirection, shouldUpdateSelection, caret)
            }
        } else if (motionDirection == Motion.Left) {
            if (newSelection.min() > 0) {
                seekToken(event, motionDirection, shouldUpdateSelection, caret)
            }
        }
    }

    // if selection is one in length, remove selection
//    if (carent.selectionModel.hasSelection()) {
//        if (carent.selectionModel.selectionEnd - carent.selectionModel.selectionStart == 1) {
//            carent.selectionModel.removeSelection()
//        }
//    }
}