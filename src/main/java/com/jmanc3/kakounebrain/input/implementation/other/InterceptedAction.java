package com.jmanc3.kakounebrain.input.implementation.other;

import com.intellij.ide.DataManager;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.editor.Editor;
import com.jmanc3.kakounebrain.KakOnFileOpen;
import com.jmanc3.kakounebrain.input.implementation.KakImplementationKt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class InterceptedAction extends AnAction {

    private final String originalID;
    private final State.Mode applicableMode;
    private final List<String> targetActions;

    public InterceptedAction(String originalID, State.Mode applicableMode, List<String> targetActions) {
//        super(ActionManager.getInstance().getAction(originalID));
        this.originalID = originalID;
        this.applicableMode = applicableMode;
        this.targetActions = targetActions;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.update(e);
//            ActionUtil.performDumbAwareUpdate(action, e, false);
            return;
        }
        State editorState = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (editorState == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.update(e);
//            ActionUtil.performDumbAwareUpdate(action, e, false);
            return;
        }

        if (applicableMode != editorState.mode || applicableMode == State.Mode.ALL) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.update(e);
//            ActionUtil.performDumbAwareUpdate(action, e, false);
        }
    }

    @Override
    public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.beforeActionPerformedUpdate(e);
//            ActionUtil.performDumbAwareUpdate(action, e, true);
            return;
        }
        State editorState = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (editorState == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.beforeActionPerformedUpdate(e);
//            ActionUtil.performDumbAwareUpdate(action, e, true);
            return;
        }

        if (applicableMode != editorState.mode || applicableMode == State.Mode.ALL) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.beforeActionPerformedUpdate(e);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.actionPerformed(e);
            return;
        }
        State editorState = editor.getUserData(KakOnFileOpen.kakStateKey);
        if (editorState == null) {
            e.getPresentation().setEnabled(true);
            AnAction action = ActionManagerEx.getInstanceEx().getAction(originalID);
            action.actionPerformed(e);
            return;
        }

        if (applicableMode == editorState.mode || applicableMode == State.Mode.ALL) {
            if (targetActions != null) {
                for (String targetAction : targetActions) {
                    executeAction(editor, targetAction);
                }
            }
        } else {
            // Default action
            executeAction(editor, originalID);
        }
    }

    public static void executeAction(@NotNull Editor editor, @NotNull String actionId) {
        executeAction(editor, actionId, false);
    }

    public static void executeAction(@NotNull Editor editor, @NotNull String actionId, boolean assertActionIsEnabled) {
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        AnAction action = actionManager.getAction(actionId);
        assertNotNull(action);
        executeAction(editor, assertActionIsEnabled, action);
    }

    public static void executeAction(@NotNull Editor editor, boolean assertActionIsEnabled, @NotNull AnAction action) {
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, "", createEditorContext(editor));
        if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
            ActionUtil.performActionDumbAwareWithCallbacks(action, event);
        } else if (assertActionIsEnabled) {
            fail("Action " + action + " is disabled");
        }
    }

    @NotNull
    private static DataContext createEditorContext(@NotNull Editor editor) {
        Editor hostEditor = editor instanceof EditorWindow ? ((EditorWindow) editor).getDelegate() : editor;
        DataContext parent = DataManager.getInstance().getDataContext(editor.getContentComponent());
        return SimpleDataContext.builder()
                .setParent(parent)
                .add(CommonDataKeys.HOST_EDITOR, hostEditor)
                .add(CommonDataKeys.EDITOR, editor)
                .build();
    }
}
