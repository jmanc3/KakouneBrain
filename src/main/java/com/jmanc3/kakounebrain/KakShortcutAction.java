package com.jmanc3.kakounebrain;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.jmanc3.kakounebrain.input.KakInput;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import org.jetbrains.annotations.NotNull;

/** A proxy keeps editor-local shortcuts off the delegate's global ShortcutSet. */
public final class KakShortcutAction extends AnAction implements DumbAware {
    private final AnAction delegate;
    private final State.Mode mode;
    private final String actionId;

    public KakShortcutAction(AnAction delegate) {
        this(delegate, null, null);
    }

    public KakShortcutAction(AnAction delegate, String actionId, State.Mode mode) {
        getTemplatePresentation().copyFrom(delegate.getTemplatePresentation());
        this.delegate = delegate;
        this.actionId = actionId;
        this.mode = mode;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        if (mode != null) {
            Editor editor = event.getData(CommonDataKeys.EDITOR);
            State state = editor == null ? null : editor.getUserData(KakOnFileOpen.kakStateKey);
            KakInput input = KakInput.getInstance();
            if (state == null || input == null || (mode != State.Mode.ALL && state.mode != mode)
                    || (input.hasPendingMenu(editor) && !KeyboardBindings.KakAction.CLOSE_MENU.equals(actionId))) {
                event.getPresentation().setEnabled(false);
                return;
            }
        }
        ActionUtil.updateAction(delegate, event);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ActionUtil.performAction(delegate, event);
    }

    public int priority() {
        return KeyboardBindings.KakAction.CLOSE_MENU.equals(actionId) ? 0 : 1;
    }
}
