package com.jmanc3.kakounebrain;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.jmanc3.kakounebrain.input.implementation.KakCommand;
import com.jmanc3.kakounebrain.input.implementation.other.State;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class KeyboardBindings {

    /**
     * Utility function to create a KeyboardShortcut
     */
    private static KeyboardShortcut sc(int keyCode, int modifiers) {
        return new KeyboardShortcut(KeyStroke.getKeyStroke(keyCode, modifiers), null);
    }

    private static KeyboardShortcut sc(int keyCode) {
        return new KeyboardShortcut(KeyStroke.getKeyStroke(keyCode, 0), null);
    }

    public interface KakAction {
        String SET_MODE_NORMAL = "KAK_SET_MODE_NORMAL";
        String SET_MODE_INSERT = "KAK_SET_MODE_INSERT";
        String NEXT_WORD = "KAK_NEXT_WORD";
        String PREVIOUS_WORD = "KAK_PREVIOUS_WORD";
        String NEXT_WORD_WITH_SELECTION = "KAK_NEXT_WORD_WITH_SELECTION";
        String PREVIOUS_WORD_WITH_SELECTION = "KAK_PREVIOUS_WORD_WITH_SELECTION";
        String SELECTION_EXTEND = "KAK_SELECTION_EXTEND";
        String TOGGLE_ZEN_MODE = "KAK_TOGGLE_ZEN_MODE";
        String NEW_LINE_BEFORE = "KAK_NEW_LINE_BEFORE";
        String NEW_LINE_AFTER = "KAK_NEW_LINE_AFTER";
        String MENU_OPEN_PANEL = "KAK_MENU_OPEN_PANEL";
        String CLOSE_MENU = "KAK_MENU_CLOSED";
        String SELECT_CURRENT_LINE = "KAK_SELECT_CURRENT_LINE";
        String DELETE = "KAK_DELETE";
        String DELETE_LINE = "KAK_DELETE_LINE";
        String PASTE_BEFORE_SELECTION = "KAK_PASTE_BEFORE_SELECTION";
        String PASTE_AFTER_SELECTION = "KAK_PASTE_AFTER_SELECTION";
        String GOTO_LINE_END_AND_INSERT = "KAK_GOTO_LINE_END_AND_INSERT";
        String CUT_SELECTED_AND_INSERT = "KAK_CUT_SELECTED_AND_INSERT";
        String GOTO_MENU = "KAK_GOTO_MENU";
        String GOTO_MENU_WITH_SELECTION = "KAK_GOTO_MENU_WITH_SELECTION";
        String CLOSE_ALL_NON_PROJECT_TABS = "KAK_CLOSE_ALL_NON_PROJECT_TABS";
        String PREVIOUS_INSTANCE_OF_SELECTION = "KAK_PREVIOUS_INSTANCE_OF_SELECTION";
        String NEXT_INSTANCE_OF_SELECTION = "KAK_NEXT_INSTANCE_OF_SELECTION";
        String SAVE_MODE = "KAK_SAVE_MODE";
        String RELOAD_MODE = "KAK_RELOAD_MODE";
        String MOVE_UP_10 = "KAK_MOVE_UP_10";
        String MOVE_DOWN_10 = "KAK_MOVE_DOWN_10";
        String MOVE_UP_10_WITH_SELECTION = "KAK_MOVE_UP_10_WITH_SELECTION";
        String MOVE_DOWN_10_WITH_SELECTION = "KAK_MOVE_DOWN_10_WITH_SELECTION";
        String SWAP_SELECTION_BOUNDARIES = "SWAP_SELECTION_BOUNDARIES";
        String NO_OP = "NO_OP";
        String GO_TO_FILE = "GO_TO_FILE";
        //String MENU_REFACTOR = "MENU_REFACTOR";
        String MENU_VIEW = "MENU_VIEW";
        String MENU_MISC = "MENU_MISC";
        String FIND_FORWARD_COVER = "FIND_FORWARD_COVER";
        String FIND_FORWARD_AHEAD = "FIND_FORWARD_AHEAD";
        String FIND_REVERSE_COVER = "FIND_REVERSE_COVER";
        String FIND_REVERSE_AHEAD = "FIND_REVERSE_AHEAD";
        String FIND_FORWARD_COVER_INCREASE_SELECTION = "FIND_FORWARD_COVER_INCREASE_SELECTION";
        String FIND_FORWARD_AHEAD_INCREASE_SELECTION = "FIND_FORWARD_AHEAD_INCREASE_SELECTION";
        String FIND_REVERSE_COVER_INCREASE_SELECTION = "FIND_REVERSE_COVER_INCREASE_SELECTION";
        String FIND_REVERSE_AHEAD_INCREASE_SELECTION = "FIND_REVERSE_AHEAD_INCREASE_SELECTION";
        String NEXT_ITEM = "NEXT_ITEM";
        String KAK_YANK = "KAK_YANK";
        String PREVIOUS_ITEM = "PREVIOUS_ITEM";
        String ESCAPE_INSERT_MODE = "ESCAPE_INSERT_MODE";
        String START_STOP_MACRO = "START_STOP_MACRO";
        String PLAY_PREVIOUS_MACRO = "PLAY_PREVIOUS_MACRO";
        String INDENT_LEFT = "INDENT_LEFT";
        String INDENT_RIGHT = "INDENT_RIGHT";
    }

    public static Object[][] CustomKakActions = {
            // NAME, ACTION
            {KakAction.SET_MODE_NORMAL, new KakCommand(KakAction.SET_MODE_NORMAL)},
            {KakAction.SET_MODE_INSERT, new KakCommand(KakAction.SET_MODE_INSERT)},
            {KakAction.NEXT_WORD, new KakCommand(KakAction.NEXT_WORD)},
            {KakAction.PREVIOUS_WORD, new KakCommand(KakAction.PREVIOUS_WORD)},
            {KakAction.NEXT_WORD_WITH_SELECTION, new KakCommand(KakAction.NEXT_WORD_WITH_SELECTION)},
            {KakAction.PREVIOUS_WORD_WITH_SELECTION, new KakCommand(KakAction.PREVIOUS_WORD_WITH_SELECTION)},
            {KakAction.SELECTION_EXTEND, new KakCommand(KakAction.SELECTION_EXTEND)},
            {KakAction.TOGGLE_ZEN_MODE, new KakCommand(KakAction.TOGGLE_ZEN_MODE)},
            {KakAction.NEW_LINE_BEFORE, new KakCommand(KakAction.NEW_LINE_BEFORE)},
            {KakAction.NEW_LINE_AFTER, new KakCommand(KakAction.NEW_LINE_AFTER)},
            {KakAction.MENU_OPEN_PANEL, new KakCommand(KakAction.MENU_OPEN_PANEL)},
            {KakAction.CLOSE_MENU, new KakCommand(KakAction.CLOSE_MENU)},
            {KakAction.SELECT_CURRENT_LINE, new KakCommand(KakAction.SELECT_CURRENT_LINE)},
            {KakAction.DELETE, new KakCommand(KakAction.DELETE)},
            {KakAction.DELETE_LINE, new KakCommand(KakAction.DELETE_LINE)},
            {KakAction.PASTE_BEFORE_SELECTION, new KakCommand(KakAction.PASTE_BEFORE_SELECTION)},
            {KakAction.PASTE_AFTER_SELECTION, new KakCommand(KakAction.PASTE_AFTER_SELECTION)},
            {KakAction.GOTO_LINE_END_AND_INSERT, new KakCommand(KakAction.GOTO_LINE_END_AND_INSERT)},
            {KakAction.CUT_SELECTED_AND_INSERT, new KakCommand(KakAction.CUT_SELECTED_AND_INSERT)},
            {KakAction.GOTO_MENU, new KakCommand(KakAction.GOTO_MENU)},
            {KakAction.GOTO_MENU_WITH_SELECTION, new KakCommand(KakAction.GOTO_MENU_WITH_SELECTION)},
            {KakAction.CLOSE_ALL_NON_PROJECT_TABS, new KakCommand(KakAction.CLOSE_ALL_NON_PROJECT_TABS)},
            {KakAction.PREVIOUS_INSTANCE_OF_SELECTION, new KakCommand(KakAction.PREVIOUS_INSTANCE_OF_SELECTION)},
            {KakAction.NEXT_INSTANCE_OF_SELECTION, new KakCommand(KakAction.NEXT_INSTANCE_OF_SELECTION)},
            {KakAction.SAVE_MODE, new KakCommand(KakAction.SAVE_MODE)},
            {KakAction.RELOAD_MODE, new KakCommand(KakAction.RELOAD_MODE)},
            {KakAction.MOVE_UP_10, new KakCommand(KakAction.MOVE_UP_10)},
            {KakAction.MOVE_DOWN_10, new KakCommand(KakAction.MOVE_DOWN_10)},
            {KakAction.MOVE_UP_10_WITH_SELECTION, new KakCommand(KakAction.MOVE_UP_10_WITH_SELECTION)},
            {KakAction.MOVE_DOWN_10_WITH_SELECTION, new KakCommand(KakAction.MOVE_DOWN_10_WITH_SELECTION)},
            {KakAction.NO_OP, new KakCommand(KakAction.NO_OP)},
            {KakAction.SWAP_SELECTION_BOUNDARIES, new KakCommand(KakAction.SWAP_SELECTION_BOUNDARIES)},
            {KakAction.GO_TO_FILE, new KakCommand(KakAction.GO_TO_FILE)},
            //{KakAction.MENU_REFACTOR, new KakCommand(KakAction.MENU_REFACTOR)},
            {KakAction.MENU_VIEW, new KakCommand(KakAction.MENU_VIEW)},
            {KakAction.MENU_MISC, new KakCommand(KakAction.MENU_MISC)},
            {KakAction.FIND_FORWARD_COVER, new KakCommand(KakAction.FIND_FORWARD_COVER)},
            {KakAction.FIND_FORWARD_AHEAD, new KakCommand(KakAction.FIND_FORWARD_AHEAD)},
            {KakAction.FIND_REVERSE_COVER, new KakCommand(KakAction.FIND_REVERSE_COVER)},
            {KakAction.FIND_REVERSE_AHEAD, new KakCommand(KakAction.FIND_REVERSE_AHEAD)},
            {KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION, new KakCommand(KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION)},
            {KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION, new KakCommand(KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION)},
            {KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION, new KakCommand(KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION)},
            {KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION, new KakCommand(KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION)},
            {KakAction.NEXT_ITEM, new KakCommand(KakAction.NEXT_ITEM)},
            {KakAction.KAK_YANK, new KakCommand(KakAction.KAK_YANK)},
            {KakAction.PREVIOUS_ITEM, new KakCommand(KakAction.PREVIOUS_ITEM)},
            {KakAction.ESCAPE_INSERT_MODE, new KakCommand(KakAction.ESCAPE_INSERT_MODE)},
            {KakAction.START_STOP_MACRO, new KakCommand(KakAction.START_STOP_MACRO)},
            {KakAction.PLAY_PREVIOUS_MACRO, new KakCommand(KakAction.PLAY_PREVIOUS_MACRO)},
            {KakAction.INDENT_LEFT, new KakCommand(KakAction.INDENT_LEFT)},
            {KakAction.INDENT_RIGHT, new KakCommand(KakAction.INDENT_RIGHT)},
    };

    /**
     * 1) Action ID to execute
     * 2) Shortcut
     * 3) (Optional) Should override default bindings for that shortcut
     */
    public static Object[][] GlobalShortcuts = {
            {IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, sc(KeyEvent.VK_W, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK)},

            {IdeActions.ACTION_REPLACE, sc(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)},

            {KakAction.CLOSE_MENU, sc(KeyEvent.VK_ESCAPE)},

            {IdeActions.ACTION_EDITOR_BACKWARD_PARAGRAPH_WITH_SELECTION, sc(KeyEvent.VK_K, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK)},
            {IdeActions.ACTION_EDITOR_FORWARD_PARAGRAPH_WITH_SELECTION, sc(KeyEvent.VK_J, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK)},
            {IdeActions.ACTION_EDITOR_BACKWARD_PARAGRAPH, sc(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK)},
            {IdeActions.ACTION_EDITOR_FORWARD_PARAGRAPH, sc(KeyEvent.VK_J, InputEvent.ALT_DOWN_MASK)},

            {IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION, sc(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},
            {IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION, sc(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},

            {IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN, sc(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), true},
            {IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP, sc(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK), true},

            {IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN_WITH_SELECTION, sc(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), true},
            {IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP_WITH_SELECTION, sc(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), true},

            {KakAction.MOVE_UP_10, sc(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), true},
            {KakAction.MOVE_DOWN_10, sc(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK), true},
            {KakAction.MOVE_UP_10_WITH_SELECTION, sc(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), true},
            {KakAction.MOVE_DOWN_10_WITH_SELECTION, sc(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), true},

            {KakAction.PREVIOUS_INSTANCE_OF_SELECTION, sc(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK), true},
            {KakAction.MENU_OPEN_PANEL, sc(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK), true},

            {KakAction.GO_TO_FILE, sc(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), true},
            //{KakAction.MENU_REFACTOR, sc(KeyEvent.VK_R, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},

            {KakAction.FIND_REVERSE_COVER_INCREASE_SELECTION, sc(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},

            {KakAction.FIND_REVERSE_COVER, sc(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK), true},
            {KakAction.FIND_REVERSE_AHEAD, sc(KeyEvent.VK_T, InputEvent.ALT_DOWN_MASK), true},
            {KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION, sc(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},
            {KakAction.FIND_REVERSE_AHEAD_INCREASE_SELECTION, sc(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), true},

            {KakAction.NEXT_ITEM, sc(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK)},
            {KakAction.PREVIOUS_ITEM, sc(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)},
    };


    // NOTE: You cannot bind single characters here (You do that in NormalModeCommands)
    public static Object[][] NormalShortcuts = {
    };

    /**
     * 1) Action ID to execute
     * 2) Shortcut
     * 3) (Optional) Should override default bindings for that shortcut
     */
    public static Object[][] InsertShortcuts = {
            {KakAction.ESCAPE_INSERT_MODE, sc(KeyEvent.VK_ESCAPE)},
    };

    /**
     * 1) Action ID to execute
     * 2) Shortcut
     * 3) (Optional) Should override default bindings for that shortcut
     */
    public static Object[][] NormalModeCommands = {
            {"i", KakAction.SET_MODE_INSERT},

            {"h", IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT},
            {"l", IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT},
            {"k", IdeActions.ACTION_EDITOR_MOVE_CARET_UP},
            {"j", IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN},

            {"H", IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT_WITH_SELECTION},
            {"L", IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT_WITH_SELECTION},
            {"J", IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN_WITH_SELECTION},
            {"K", IdeActions.ACTION_EDITOR_MOVE_CARET_UP_WITH_SELECTION},

            {"w", KakAction.NEXT_WORD},
            {"b", KakAction.PREVIOUS_WORD},

            {"d", KakAction.DELETE},

            {"W", KakAction.NEXT_WORD_WITH_SELECTION},
            {"B", KakAction.PREVIOUS_WORD_WITH_SELECTION},

            {"u", IdeActions.ACTION_UNDO},
            {"U", IdeActions.ACTION_REDO},

            {"g", KakAction.GOTO_MENU, true},
            {"G", KakAction.GOTO_MENU_WITH_SELECTION, true},

            {"r", IdeActions.ACTION_REPLACE},

            {"`", KakAction.CLOSE_ALL_NON_PROJECT_TABS},

            {";", KakAction.SWAP_SELECTION_BOUNDARIES},

            {"z", KakAction.TOGGLE_ZEN_MODE},

            {"o", KakAction.NEW_LINE_AFTER},
            {"O", KakAction.NEW_LINE_BEFORE},

            {"X", KakAction.DELETE_LINE},
            {"x", KakAction.SELECT_CURRENT_LINE},

            {"c", KakAction.CUT_SELECTED_AND_INSERT},
            {"A", KakAction.GOTO_LINE_END_AND_INSERT},

            {"P", KakAction.PASTE_BEFORE_SELECTION},
            {"p", KakAction.PASTE_AFTER_SELECTION},

            {"y", KakAction.KAK_YANK},

            {"q", KakAction.PLAY_PREVIOUS_MACRO},
            {"Q", KakAction.START_STOP_MACRO},

            {"n", KakAction.NEXT_INSTANCE_OF_SELECTION},

            {"/", IdeActions.ACTION_FIND},

            {"v", KakAction.MENU_VIEW},

            {"f", KakAction.FIND_FORWARD_COVER},
            {"t", KakAction.FIND_FORWARD_AHEAD},
            {"F", KakAction.FIND_FORWARD_COVER_INCREASE_SELECTION},
            {"T", KakAction.FIND_FORWARD_AHEAD_INCREASE_SELECTION},

            {".", KakAction.MENU_MISC},

            {",", "NextSplitter"},

            {"<", KakAction.INDENT_LEFT},
            {">", KakAction.INDENT_RIGHT},
    };

    /**
     * 1) Action ID we want to disable,
     * 2) Mode we want to disable it in
     * 3) (Optional) actions to be done instead
     */
    public static Object[][] DefaultActionOverride = {

            {"EditorForwardParagraphWithSelection", "BackupEditorForwardParagraphWithSelection", "KakEditorForwardParagraphWithSelection"},
            {"EditorBackwardParagraphWithSelection", "BackupEditorBackwardParagraphWithSelection", "KakEditorBackwardParagraphWithSelection"},
            {"EditorTextStartWithSelection", "BackupEditorTextStartWithSelection", "KakEditorTextStartWithSelection"},
            {"EditorTextEndWithSelection", "BackupEditorTextEndWithSelection", "KakEditorTextEndWithSelection"},
            {"EditorLineStartWithSelection", "BackupEditorLineStartWithSelection", "KakEditorLineStartWithSelection"},
            {"EditorLineEndWithSelection", "BackupEditorLineEndWithSelection", "KakEditorLineEndWithSelection"},
            {"EditorLeftWithSelection", "BackupEditorLeftWithSelection", "KakEditorLeftWithSelection"},
            {"EditorRightWithSelection", "BackupEditorRightWithSelection", "KakEditorRightWithSelection"},
            {"EditorUpWithSelection", "BackupEditorUpWithSelection", "KakEditorUpWithSelection"},
            {"EditorDownWithSelection", "BackupEditorDownWithSelection", "KakEditorDownWithSelection"},
            {"EditorPageUpWithSelection", "BackupEditorPageUpWithSelection", "KakEditorPageUpWithSelection"},
            {"EditorPageDownWithSelection", "BackupEditorPageDownWithSelection", "KakEditorPageDownWithSelection"},
            {"EditorNextWordWithSelection", "BackupEditorNextWordWithSelection", "KakEditorNextWordWithSelection"},
            {"EditorPreviousWordWithSelection", "BackupEditorPreviousWordWithSelection", "KakEditorPreviousWordWithSelection"},
    };

    /**
     * 1) Action ID we are going to be replacing
     * 2) The backup ID we are going to make for it (so it can be restored and called later)
     * 3) The Action it should do instead
     */
    public static Object[][] ShortcutActionRemap = {
//            {"EditorEnter", State.Mode.NORMAL},
//            {"EditorTab", State.Mode.NORMAL},
//            {"EditorDelete", State.Mode.NORMAL},
//            {"$Delete", State.Mode.NORMAL},
//            {"EditorBackSpace", State.Mode.NORMAL},
//            {"EditorDeleteToWordStart", State.Mode.NORMAL},
//            {"EditorDeleteToWordEnd", State.Mode.NORMAL},

            {"RenameElement", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "RenameElement"},
            {"IntroduceVariable", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceVariable"},
            //{"refactoring.introduce.property", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "refactoring.introduce.property"},
            {"IntroduceField", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceField"},
            {"IntroduceProperty", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceProperty"},
            {"IntroduceFunctionalParameter", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceFunctionalParameter"},
            {"IntroduceTypeAlias", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceTypeAlias"},
            {"IntroduceConstant", State.Mode.NORMAL, KakAction.SAVE_MODE, KakAction.SET_MODE_INSERT, "IntroduceConstant"},

    };
}

/**
 * keys_global = {
 * { "interactive_open_or_new", "O", "Control",},
 * };
 * <p>
 * keys_normal = {
 * // Switch mode
 * <p>
 * { "jmanc3_kak_misc_menu",   "Period",},
 * { "change_active_panel",           "Comma" },
 * { "change_active_panel_backwards", "Comma", "Shift" }, * };
 */
