package com.jmanc3.kakounebrain.input.implementation.other;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.jmanc3.kakounebrain.KeyboardBindings;
import com.jmanc3.kakounebrain.input.implementation.KakCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KakPromo implements ActionPromoter {
    public static List<AnAction> acl = new ArrayList<>();

    @Override
    public @Nullable List<AnAction> promote(@NotNull List<? extends AnAction> actions, @NotNull DataContext context) {
        acl.clear();

        for (AnAction action : actions) {
            if (action instanceof KakCommand) {
                KakCommand act = (KakCommand) action;
                if (act.getType().equals(KeyboardBindings.KakAction.NEXT_ITEM) ||
                        act.getType().equals(KeyboardBindings.KakAction.PREVIOUS_ITEM) ||
                        act.getType().equals(KeyboardBindings.KakAction.CLOSE_MENU) || act.getType().equals(KeyboardBindings.KakAction.ESCAPE_INSERT_MODE)) {
                    acl.add(0, action);
                }
            } else {
                acl.add(action);
            }
        }

        return acl;
    }
}
