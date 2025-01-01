package com.jmanc3.kakounebrain;


import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class KakOnFileOpen implements FileEditorManagerListener, DumbAware {

    public static Key<Object> listenersAttached = Key.create("listenersAttached");

    public static Key<State> kakStateKey = Key.create("kakState");

    KakOnFileOpen() {
        PluginStartup.startup();
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        FileEditorManagerListener.super.fileOpened(source, file);

        for (FileEditor allEditor : source.getAllEditors(file)) {
            if (allEditor instanceof TextEditor) {
                final Editor editor = ((TextEditor) allEditor).getEditor();
                EditorKind editorKind = editor.getEditorKind();
                State editorState = editor.getUserData(kakStateKey);
                if (editorState == null) {
                    editor.putUserData(kakStateKey, new State());
                    State state = editor.getUserData(kakStateKey);
                    if (editorKind == EditorKind.MAIN_EDITOR && !editor.isOneLineMode()) {
                        state.mode = State.Mode.NORMAL;
                    } else {
                        state.mode = State.Mode.INSERT;
                    }

                    editor.getCaretModel().runForEachCaret(it -> {
                        if (state.mode == State.Mode.INSERT)
                            it.setVisualAttributes(PluginStartup.INSERT_CARET);
                        else
                            it.setVisualAttributes(PluginStartup.NORMAL_CARET);
                    });
                } else {
                    editor.getCaretModel().runForEachCaret(it -> {
                        if (editorState.mode == State.Mode.INSERT)
                            it.setVisualAttributes(PluginStartup.INSERT_CARET);
                        else
                            it.setVisualAttributes(PluginStartup.NORMAL_CARET);
                    });
                }

                PluginStartup.removeAllShortcuts();
                PluginStartup.addInsertKakShortcuts();
                PluginStartup.addGlobalKakShortcuts();

                editor.getCaretModel().addCaretListener(new CaretListener() {
                    @Override
                    public void caretAdded(@NotNull CaretEvent event) {
                        CaretListener.super.caretAdded(event);
                        Caret caret = event.getCaret();
                        if (caret != null) {
                            State editorState = event.getEditor().getUserData(kakStateKey);
                            if (editorState != null) {
                                if (editorState.mode == State.Mode.INSERT) {
                                    caret.setVisualAttributes(PluginStartup.INSERT_CARET);
                                } else {
                                    caret.setVisualAttributes(PluginStartup.NORMAL_CARET);
                                }
                            }
                        }
                    }
                });

                if (editor.getUserData(listenersAttached) == null) {
                    editor.putUserData(listenersAttached, "ignore");
                    editor.getDocument().addDocumentListener(new DocumentListener() {
                        public void documentChanged(@NotNull DocumentEvent event) {
                            DocumentListener.super.documentChanged(event);
                            State editorState = editor.getUserData(kakStateKey);
                            if (editorState != null) {
                                editorState.previousTextChange = event.getOffset() + event.getNewFragment().length();
                            }
                        }
                    });
                }
            }
        }
    }

}
