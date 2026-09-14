package com.jmanc3.kakounebrain.input.implementation.other;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.jmanc3.kakounebrain.KakShortcutAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KakPromo implements ActionPromoter {
    @Override
    public @NotNull List<AnAction> promote(@NotNull List<? extends AnAction> actions, @NotNull DataContext context) {
        // Never retain action-system objects in a static list across plugin unloads.
        List<AnAction> ordered = new ArrayList<>(actions);
        ordered.sort(Comparator.comparingInt(action -> action instanceof KakShortcutAction shortcut ? shortcut.priority() : 2));
        return ordered;
    }
}
