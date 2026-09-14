package com.jmanc3.kakounebrain.input;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.project.DumbAware;
import com.jmanc3.kakounebrain.KakOnFileOpen;
import com.jmanc3.kakounebrain.KeyboardBindings;
import com.jmanc3.kakounebrain.input.implementation.KakFunctions;
import com.jmanc3.kakounebrain.input.implementation.MacroRenderer;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import com.jmanc3.kakounebrain.input.implementation.other.MenuRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.Callable;


/**
 * Handles what happens when input is received
 */
public class KakInput implements TypedActionHandlerEx, DumbAware, Disposable {

    public static KakInput instance;

    public static KakInput getInstance() {
        return instance;
    }

    private final TypedActionHandler defaultInputHandler;
    private final AWTEventListener awtListener = new MyAwtPreprocessor();
    private boolean active;

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

    public KakInput(TypedActionHandler defaultInputHandler) {
        this.defaultInputHandler = defaultInputHandler;
        kakFunctions = new KakFunctions();
    }

    public void install() {
        KakInput.instance = this;
        active = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.KEY_EVENT_MASK);
        var connection = ApplicationManager.getApplication().getMessageBus().connect(this);
        connection.subscribe(AnActionListener.TOPIC, new AnActionListener() {
            @Override
            public void afterActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event, @NotNull AnActionResult result) {
                if (active && recordingMacro && result.isPerformed()) {
                    String actionId = ActionManager.getInstance().getId(action);
                    if (actionId != null && !actionId.equals(KeyboardBindings.KakAction.START_STOP_MACRO)) {
                        macroActions.add(new MacroActions(MacroActionsType.ActionText, actionId));
                    }
                }
            }
        });
    }

    public Callable<Boolean> someoneWantsKeyPress = null;

    public char c;

    public boolean hasPendingMenu(Editor editor) {
        return active && someoneWantsKeyPress != null && menuRenderer.isForEditor(editor);
    }

    public void cancelPendingMenu(Editor editor) {
        if (menuRenderer.isForEditor(editor)) cancelPendingMenu();
    }

    private void cancelPendingMenu() {
        someoneWantsKeyPress = null;
        menuRenderer.clear();
    }

    @Override
    public void dispose() {
        active = false;
        recordingMacro = false;
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
        cancelPendingMenu();
        macroRenderer.clear();
        macroActions.clear();
        if (instance == this) instance = null;
    }

    @Override
    public void beforeExecute(@NotNull Editor editor, char c, @NotNull DataContext context, @NotNull ActionPlan plan) {
        State state = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (active && state != null && (state.mode == State.Mode.NORMAL || hasPendingMenu(editor))) {
            kakFunctions.hanledBefore(editor, c, context, plan);
        } else if (defaultInputHandler instanceof TypedActionHandlerEx extended) {
            extended.beforeExecute(editor, c, context, plan);
        }
    }

    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext context) {
        if (someoneWantsKeyPress != null && !hasPendingMenu(editor)) cancelPendingMenu();
        State state = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (!active || state == null) {
            defaultInputHandler.execute(editor, charTyped, context);
            return;
        }

        c = charTyped;
        if (hasPendingMenu(editor)) {
            try {
                if (WriteIntentReadAction.computeThrowable(() -> someoneWantsKeyPress.call())) return;
            } catch (Exception exception) {
                cancelPendingMenu();
                throw new RuntimeException(exception);
            }
        }
        if (state.mode == State.Mode.NORMAL) {
            state.resetTyping();
            kakFunctions.handled(editor, charTyped, context);
            return;
        }

        var document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        boolean escape = charTyped == 'k' && state.pendingJOffset == offset
                && state.pendingJStamp == document.getModificationStamp()
                && editor.getCaretModel().getCaretCount() == 1 && !editor.getSelectionModel().hasSelection();
        state.resetTyping();
        defaultInputHandler.execute(editor, charTyped, context);
        int after = editor.getCaretModel().getOffset();
        if (escape && after == offset + 1 && after >= 2
                && "jk".contentEquals(document.getCharsSequence().subSequence(after - 2, after))) {
            // Delete only the verified chord; never undo or replace unrelated document content.
            WriteCommandAction.runWriteCommandAction(editor.getProject(), "Exit insert mode", null, () -> {
                document.deleteString(after - 2, after);
                editor.getCaretModel().moveToOffset(after - 2);
                KakOnFileOpen.setMode(editor, State.Mode.NORMAL);
            });
        } else if (charTyped == 'j' && after == offset + 1 && editor.getCaretModel().getCaretCount() == 1
                && document.getCharsSequence().charAt(after - 1) == 'j') {
            state.pendingJOffset = after;
            state.pendingJStamp = document.getModificationStamp();
        }
    }
}
