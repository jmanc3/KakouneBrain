package com.jmanc3.kakounebrain;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.editor.CaretVisualAttributes;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.JBColor;
import com.jmanc3.kakounebrain.input.KakInput;
import com.jmanc3.kakounebrain.input.implementation.KakCommand;
import com.jmanc3.kakounebrain.input.implementation.other.InterceptedAction;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginStartup implements ProjectActivity, DumbAware, Disposable {

    public static ArrayList<ActionAndShortcut> defaultActionsAndShortcuts = new ArrayList<>();

    static boolean firstTime = true;

    public static CaretVisualAttributes NORMAL_CARET = new CaretVisualAttributes(JBColor.RED,
            CaretVisualAttributes.Weight.HEAVY, CaretVisualAttributes.Shape.BLOCK, 1F);

    public static CaretVisualAttributes INSERT_CARET = new CaretVisualAttributes(JBColor.GREEN,
            CaretVisualAttributes.Weight.HEAVY, CaretVisualAttributes.Shape.BLOCK, 1F);

    public static final CaretVisualAttributes DEFAULT_CARET = new CaretVisualAttributes(JBColor.WHITE,
            CaretVisualAttributes.Weight.HEAVY, CaretVisualAttributes.Shape.BLOCK, .1F);

    public static void startup() {
        // TODO: change firstTime back to true so this executes
        if (firstTime) {
            firstTime = false;

            EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
                @Override
                public void editorCreated(@NotNull EditorFactoryEvent event) {
                    Editor editor = event.getEditor();
                    editor.getContentComponent().addFocusListener(new FocusListener() {
                        @Override
                        public void focusGained(FocusEvent focusEvent) {
                            if (KakInput.getInstance().recordingMacro)
                                KakInput.getInstance().macroRenderer.showIt(editor);
                        }

                        @Override
                        public void focusLost(FocusEvent focusEvent) {
                            if (KakInput.getInstance().recordingMacro)
                                KakInput.getInstance().macroRenderer.hideIt(editor);
                        }
                    });
                }

                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                    EditorFactoryListener.super.editorReleased(event);
                }
            }, () -> {

            });

            /**
             * Who wants macro playback to be two hundred ms between keystrokes? Literally no one.
             *
             * (Maybe they thought if they made it that bad, no one would want to use it,
             * and they wouldn't have to fix any bugs associated with it)
             */
            Registry.get("actionSystem.playback.typecommand.delay").setValue(0);
            Registry.get("editor.block.caret.selection.vim-like").setValue(true);

            /**
             * This registers the custom kak actions, so they can be bound to shortcuts later
             * */
            for (Object[] kakAction : KeyboardBindings.CustomKakActions) {
                if (kakAction.length == 2 && kakAction[0] instanceof String && kakAction[1] instanceof AnAction) {
                    var actionName = (String) kakAction[0];
                    var shortcut = (AnAction) kakAction[1];
                    ActionManager.getInstance().registerAction(actionName, shortcut);
                }
            }

            /**
             * This overrides the default implementation of certain actions (in-place), and creates a backup of that original action
             */
            for (Object[] objects : KeyboardBindings.DefaultActionOverride) {
                if (objects.length >= 3) {
                    if (objects[0] instanceof String && objects[1] instanceof String && objects[2] instanceof String) {
                        String defaultActionName = (String) objects[0];
                        String defaultActionBackupName = (String) objects[1];
                        String implementationName = (String) objects[2];

                        ActionManager am = ActionManager.getInstance();
                        AnAction defaultAction = am.getAction(defaultActionName);

                        KakCommand implementationAction = new KakCommand(implementationName);
                        am.registerAction(implementationName, implementationAction);


                        ArrayList<String> alternativeAction = new ArrayList<>();
                        alternativeAction.add(implementationName);
                        InterceptedAction ia = new InterceptedAction(defaultActionBackupName, State.Mode.ALL, alternativeAction);

                        am.replaceAction(defaultActionName, ia);

                        am.registerAction(defaultActionBackupName, defaultAction);
                    }
                }
            }

            /**
             * Collects the currently set shortcuts and actions
             */
            var keymapManager = KeymapManager.getInstance();
            var act = keymapManager.getActiveKeymap();
            Set<Shortcut> uniqueShortcuts = new HashSet<>();
            for (String actionId : act.getActionIdList())
                uniqueShortcuts.addAll(List.of(act.getShortcuts(actionId)));
            for (Shortcut uniqueShortcut : uniqueShortcuts) {
                for (String actionId : act.getActionIds(uniqueShortcut)) {
                    var actionAndShortcut = new ActionAndShortcut();
                    actionAndShortcut.id = actionId;
                    actionAndShortcut.shortcut = uniqueShortcut;
                    defaultActionsAndShortcuts.add(actionAndShortcut);
                }
            }

            removeAllShortcuts();

            /**
             * Add back shortcuts, wrapping them with InterceptedAction if need be,
             * which will either execute nothing, or alternative actions when the original shortcut is pressed
             */
            for (ActionAndShortcut actionShortcut : defaultActionsAndShortcuts) {
                for (Object[] objects : KeyboardBindings.ShortcutActionRemap) {
                    String originalID = null;
                    State.Mode mode = State.Mode.NORMAL;

                    if (objects.length >= 2) {
                        if (objects[0] instanceof String && objects[1] instanceof State.Mode) {
                            originalID = (String) objects[0];
                            mode = (State.Mode) objects[1];
                        }
                    }

                    if (actionShortcut.id.equals(originalID)) {
                        List<String> targetActions = null;

                        if (objects.length >= 3) {
                            targetActions = new ArrayList<>();

                            for (int i = 2; i < objects.length; i++) {
                                if (objects[i] instanceof String) {
                                    targetActions.add((String) objects[i]);
                                }
                            }
                        }
                        // TODO: when a popup opens, we need to go into insert mode
                        ActionManager.getInstance().registerAction("___KAK___" + actionShortcut.id + actionShortcut.shortcut, new InterceptedAction(originalID, mode, targetActions));

                        actionShortcut.id = "___KAK___" + actionShortcut.id + actionShortcut.shortcut;
                    }
                }
            }

            removeAllShortcuts();
            addGlobalKakShortcuts();
            addNormalKakShortcuts();

            /**
             * Adds a handler for characters typed (which is different from shortcuts)
             */
            var typedAction = TypedAction.getInstance();
            var defaultInputHandler = typedAction.getRawHandler();
            typedAction.setupRawHandler(new KakInput((TypedActionHandlerEx) defaultInputHandler));
        }
    }

    public static void removeAllShortcuts() {
        var keymapManager = KeymapManager.getInstance();
        var act = keymapManager.getActiveKeymap();
        for (String actionId : act.getActionIds()) {
            act.removeAllActionShortcuts(actionId);
        }
    }

    public static void addGlobalKakShortcuts() {
        /**
         * First we add back the default actions and shortcuts we found when we booted
         */
        for (ActionAndShortcut actionAndShortcut : defaultActionsAndShortcuts) {
            KeymapManager.getInstance().getActiveKeymap().addShortcut(actionAndShortcut.id, actionAndShortcut.shortcut);
        }

        /**
         * We add all the global shortcuts, and if requested, remove any actions bound to those shortcut already
         */
        for (Object[] objects : KeyboardBindings.GlobalShortcuts) {
            if (objects.length >= 2 && objects[0] instanceof String && objects[1] instanceof KeyboardShortcut) {
                var actionName = (String) objects[0];
                var shortcut = (KeyboardShortcut) objects[1];

                if (objects.length >= 3) {
                    if (objects[2] instanceof Boolean) {
                        if ((Boolean) objects[2]) {
                            @NotNull String[] actionIds = KeymapManager.getInstance().getActiveKeymap().getActionIds(shortcut);
                            for (String actionId : actionIds) {
                                KeymapManager.getInstance().getActiveKeymap().removeAllActionShortcuts(actionId);
                            }
                        }
                    }
                }
                KeymapManager.getInstance().getActiveKeymap().addShortcut(actionName, shortcut);
            }
        }
    }

    public static void addInsertKakShortcuts() {
        /**
         * We add all the insert mode shortcuts, and if requested, remove any actions bound to those shortcut already
         */
        for (Object[] objects : KeyboardBindings.InsertShortcuts) {
            if (objects.length >= 2 && objects[0] instanceof String && objects[1] instanceof KeyboardShortcut) {
                var actionName = (String) objects[0];
                var shortcut = (KeyboardShortcut) objects[1];

                if (objects.length >= 3) {
                    if (objects[2] instanceof Boolean) {
                        if ((Boolean) objects[2]) {
                            @NotNull String[] actionIds = KeymapManager.getInstance().getActiveKeymap().getActionIds(shortcut);
                            for (String actionId : actionIds) {
                                KeymapManager.getInstance().getActiveKeymap().removeAllActionShortcuts(actionId);
                            }
                        }
                    }
                }
                KeymapManager.getInstance().getActiveKeymap().addShortcut(actionName, shortcut);
            }
        }
    }

    public static void addNormalKakShortcuts() {
        /**
         * We add all the normal mode shortcuts, and if requested, remove any actions bound to those shortcut already
         */
        for (Object[] objects : KeyboardBindings.NormalShortcuts) {
            if (objects.length >= 2 && objects[0] instanceof String && objects[1] instanceof KeyboardShortcut) {
                var actionName = (String) objects[0];
                var shortcut = (KeyboardShortcut) objects[1];

                if (objects.length >= 3) {
                    if (objects[2] instanceof Boolean) {
                        if ((Boolean) objects[2]) {
                            @NotNull String[] actionIds = KeymapManager.getInstance().getActiveKeymap().getActionIds(shortcut);
                            for (String actionId : actionIds) {
                                KeymapManager.getInstance().getActiveKeymap().removeAllActionShortcuts(actionId);
                            }
                        }
                    }
                }
                KeymapManager.getInstance().getActiveKeymap().addShortcut(actionName, shortcut);
            }
        }
    }

    public static void addMenuBind() {
        KeymapManager.getInstance().getActiveKeymap().addShortcut(KeyboardBindings.KakAction.CLOSE_MENU, new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null));
    }

    @Override
    public void dispose() {

    }

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        startup();

        return null;
    }

    static class ActionAndShortcut {
        String id;
        Shortcut shortcut;
    }
}
