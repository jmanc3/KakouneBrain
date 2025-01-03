package com.jmanc3.kakounebrain.input;

//import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
//import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.messages.MessageBusConnection;
import com.jmanc3.kakounebrain.KakOnFileOpen;
import com.jmanc3.kakounebrain.KeyboardBindings;
import com.jmanc3.kakounebrain.PluginStartup;
import com.jmanc3.kakounebrain.input.implementation.KakFunctions;
import com.jmanc3.kakounebrain.input.implementation.MacroRenderer;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import com.jmanc3.kakounebrain.input.implementation.other.MenuRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.intellij.openapi.application.ActionsKt.runUndoTransparentWriteAction;

/**
 * Handles what happens when input is received
 */
public class KakInput implements TypedActionHandlerEx, DumbAware {

    public static KakInput instance;

    public static KakInput getInstance() {
        return instance;
    }

    private TypedActionHandlerEx defaultInputHandler;

    private KakFunctions kakFunctions;

    public MenuRenderer menuRenderer = new MenuRenderer();

    public MacroRenderer macroRenderer = new MacroRenderer();

    public boolean recordingMacro = false;

    public enum MacroActionsType {
        Text, // Like typing
        ActionText,
        Shortcut // Like IDE actions
    }

    public static class MacroActions {
        public MacroActionsType type;
        public String text;
        public KeyEvent event;

        public MacroActions(MacroActionsType type, String text) {
            this.type = type;
            this.text = text;
        }

        public MacroActions(MacroActionsType action, KeyEvent inputEvent) {
            this.type = action;
            this.event = inputEvent;
        }
    }

    public ArrayList<MacroActions> macroActions = new ArrayList<>();

    private class MyAwtPreprocessor implements AWTEventListener {
        @Override
        public void eventDispatched(AWTEvent event) {
            if (event instanceof KeyEvent) {
                if (recordingMacro) {
                    KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getID() == KeyEvent.KEY_TYPED) {
                        if (!keyEvent.isAltDown() && !keyEvent.isControlDown()) {
                            macroActions.add(new MacroActions(MacroActionsType.Text, String.valueOf(keyEvent.getKeyChar())));
                        }
                    }
                }
            }
        }
    }

    public KakInput(TypedActionHandlerEx defaultInputHandler) {
        this.defaultInputHandler = defaultInputHandler;
        kakFunctions = new KakFunctions();
        KakInput.instance = this;

        // Macro stuff
        Toolkit.getDefaultToolkit().addAWTEventListener(new MyAwtPreprocessor(), AWTEvent.KEY_EVENT_MASK);
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(AnActionListener.TOPIC, new AnActionListener() {
            @Override
            public void beforeShortcutTriggered(@NotNull Shortcut shortcut, @NotNull List<AnAction> actions, @NotNull DataContext dataContext) {
//                IdeEventQueue ideEventQueue = IdeEventQueue.getInstance();
//                IdeKeyEventDispatcher keyEventDispatcher = ideEventQueue.getKeyEventDispatcher();
//                KeyEvent inputEvent = keyEventDispatcher.getContext().getInputEvent();
//
//                if (recordingMacro) {
//                    macroActions.add(new MacroActions(MacroActionsType.Shortcut, inputEvent));
//                }
            }

            @Override
            public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event, @NotNull AnActionResult result) {
                if (result.isPerformed()) {
                    if (recordingMacro) {
                        var actionId = ActionManager.getInstance().getId(action);
                        if (actionId != null && !actionId.equals(KeyboardBindings.KakAction.START_STOP_MACRO)) {
                            macroActions.add(new MacroActions(MacroActionsType.ActionText, actionId));
                        }
//                        if (actionId != null && actionId.equals("SearchEverywhere")) {
//                            macroActions.add(new MacroActions(MacroActionsType.ActionText, "SearchEverywhere"));
//                        }
//                        if (actionId != null && actionId.equals("SearchEverywhere")) {
//                        }
                    }
                }
            }
        });
    }

    public Callable<Boolean> someoneWantsKeyPress = null;

    public char c;
    public char before_current = '0';

    @Override
    public void beforeExecute(@NotNull Editor editor, char c, @NotNull DataContext context, @NotNull ActionPlan plan) {
        State editorState = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (editorState == null) {
            defaultInputHandler.beforeExecute(editor, c, context, plan);
            return;
        }

        if (editorState.mode != State.Mode.NORMAL && someoneWantsKeyPress == null) {
            defaultInputHandler.beforeExecute(editor, c, context, plan);
        } else {
            kakFunctions.hanledBefore(editor, c, context, plan);
        }
    }

    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext context) {
        State editorState = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (editorState == null) {
            defaultInputHandler.execute(editor, charTyped, context);
            return;
        }

        KakInput.getInstance().c = charTyped;
        if (someoneWantsKeyPress != null) {
            try {
                Boolean consumed = someoneWantsKeyPress.call();
                if (consumed) {
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (editorState.mode == State.Mode.NORMAL) {
            kakFunctions.handled(editor, charTyped, context);
        } else {
            defaultInputHandler.execute(editor, charTyped, context);

            UndoManager undoManager = UndoManager.getInstance(editor.getProject());
            var fileEditor = TextEditorProvider.getInstance().getTextEditor(editor);

            if (before_current == 'j' && c == 'k') {
                CharSequence beforeUndo = editor.getDocument().getImmutableCharSequence();
                int startOff = editor.getCaretModel().getPrimaryCaret().getOffset();

                String text = beforeUndo.subSequence(0, startOff - 2) + beforeUndo.subSequence(startOff, beforeUndo.length()).toString();

                undoManager.undo(fileEditor);

                runUndoTransparentWriteAction(() -> {
                    editor.getDocument().replaceString(0, editor.getDocument().getTextLength(), text);
                    return null;
                });

                editorState.mode = State.Mode.NORMAL;
                editor.getCaretModel().runForEachCaret(caret -> {
                    if (editorState.mode == State.Mode.INSERT) {
                        caret.setVisualAttributes(PluginStartup.INSERT_CARET);
                    } else {
                        caret.setVisualAttributes(PluginStartup.NORMAL_CARET);
                    }
                });
                editor.getCaretModel().removeSecondaryCarets();
                editor.getCaretModel().moveToOffset(startOff - 2);
            }
            before_current = c;
        }
    }
}
