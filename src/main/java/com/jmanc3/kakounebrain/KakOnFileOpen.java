package com.jmanc3.kakounebrain;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.jmanc3.kakounebrain.input.KakInput;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import org.jetbrains.annotations.NotNull;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;

public final class KakOnFileOpen implements FileEditorManagerListener, DumbAware {
    public static final Key<State> kakStateKey = Key.create("KakouneBrain.state");

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        KakPluginService service = KakPluginService.getInstance();
        for (FileEditor fileEditor : source.getAllEditors(file)) {
            if (fileEditor instanceof TextEditor textEditor) service.attachEditor(textEditor.getEditor());
        }
    }

    static boolean isSupported(Editor editor) {
        return !editor.isDisposed() && editor.getProject() != null && !editor.getProject().isDisposed()
                && editor.getEditorKind() == EditorKind.MAIN_EDITOR && !editor.isOneLineMode() && !editor.isViewer();
    }

    public static void setMode(Editor editor, State.Mode mode) {
        State state = editor.getUserData(kakStateKey);
        if (state == null || editor.isDisposed()) return;
        state.mode = mode;
        state.resetTyping();
        editor.getCaretModel().runForEachCaret(caret -> caret.setVisualAttributes(
                mode == State.Mode.INSERT ? State.INSERT_CARET : State.NORMAL_CARET));
    }

    /** One attachment per editor, disposed on editor release or plugin deactivation. */
    static final class EditorAttachment implements Disposable {
        private final Editor editor;
        private final KakInput input;
        private final State state = new State();
        private final Map<Caret, CaretVisualAttributes> originalCarets = new IdentityHashMap<>();
        private final FocusAdapter focusListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (input.recordingMacro) input.macroRenderer.showIt(editor);
            }

            @Override
            public void focusLost(FocusEvent event) {
                state.resetTyping();
                input.cancelPendingMenu(editor);
                input.macroRenderer.hideIt(editor);
            }
        };

        EditorAttachment(Editor editor, KakInput input) {
            this.editor = editor;
            this.input = input;
        }

        void install() {
            editor.putUserData(kakStateKey, state);
            for (Caret caret : editor.getCaretModel().getAllCarets()) rememberCaret(caret);
            editor.getCaretModel().addCaretListener(new CaretListener() {
                @Override
                public void caretAdded(@NotNull CaretEvent event) {
                    if (event.getCaret() != null) rememberCaret(event.getCaret());
                }

                @Override
                public void caretRemoved(@NotNull CaretEvent event) {
                    originalCarets.remove(event.getCaret());
                    state.resetTyping();
                }

                @Override
                public void caretPositionChanged(@NotNull CaretEvent event) {
                    state.resetTyping();
                }
            }, this);
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    state.previousTextChange = event.getOffset() + event.getNewLength();
                    state.resetTyping();
                }
            }, this);
            editor.getContentComponent().addFocusListener(focusListener);
            installShortcuts(KeyboardBindings.GlobalShortcuts, State.Mode.ALL);
            installShortcuts(KeyboardBindings.NormalShortcuts, State.Mode.NORMAL);
            installShortcuts(KeyboardBindings.InsertShortcuts, State.Mode.INSERT);
        }

        private void rememberCaret(Caret caret) {
            originalCarets.put(caret, caret.getVisualAttributes());
            caret.setVisualAttributes(state.mode == State.Mode.INSERT ? State.INSERT_CARET : State.NORMAL_CARET);
        }

        private void installShortcuts(Object[][] bindings, State.Mode mode) {
            Map<String, Set<Shortcut>> shortcuts = new LinkedHashMap<>();
            for (Object[] binding : bindings) {
                shortcuts.computeIfAbsent((String) binding[0], ignored -> new LinkedHashSet<>()).add((Shortcut) binding[1]);
            }
            shortcuts.forEach((id, keys) -> {
                AnAction action = ActionManager.getInstance().getAction(id);
                if (action != null) new KakShortcutAction(action, id, mode).registerCustomShortcutSet(
                        new CustomShortcutSet(keys.toArray(Shortcut[]::new)), editor.getContentComponent(), this);
            });
        }

        @Override
        public void dispose() {
            input.cancelPendingMenu(editor);
            input.macroRenderer.hideIt(editor);
            editor.getContentComponent().removeFocusListener(focusListener);
            if (!editor.isDisposed()) {
                originalCarets.forEach((caret, attributes) -> {
                    if (caret.isValid()) caret.setVisualAttributes(attributes);
                });
            }
            editor.putUserData(kakStateKey, null);
            originalCarets.clear();
            state.resetTyping();
        }
    }
}
