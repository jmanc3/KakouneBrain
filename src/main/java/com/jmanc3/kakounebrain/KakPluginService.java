package com.jmanc3.kakounebrain;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.jmanc3.kakounebrain.input.KakInput;
import com.jmanc3.kakounebrain.input.implementation.KakCommand;
import com.jmanc3.kakounebrain.input.implementation.other.InterceptedAction;
import com.jmanc3.kakounebrain.input.implementation.other.State;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/** Owns one reversible activation across all projects. All mutations run on EDT. */
@Service(Service.Level.APP)
public final class KakPluginService implements Disposable {
    private Activation activation;
    private boolean disposed;

    public static KakPluginService getInstance() {
        return ApplicationManager.getApplication().getService(KakPluginService.class);
    }

    public void activate() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (disposed || activation != null) return;
        Activation next = new Activation();
        activation = next;
        try {
            WriteIntentReadAction.run(next::install);
        } catch (RuntimeException | Error failure) {
            deactivate();
            throw failure;
        }
    }

    public void attachEditor(Editor editor) {
        activate();
        if (activation != null) WriteIntentReadAction.run(() -> activation.attach(editor));
    }

    public void deactivate() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        Activation previous = activation;
        activation = null;
        if (previous != null) WriteIntentReadAction.run(() -> Disposer.dispose(previous));
    }

    @Override
    public void dispose() {
        // Services can also be disposed during application shutdown off EDT.
        Runnable cleanup = () -> {
            disposed = true;
            deactivate();
        };
        if (ApplicationManager.getApplication().isDispatchThread()) cleanup.run();
        else ApplicationManager.getApplication().invokeAndWait(cleanup);
    }

    private static final class Activation implements Disposable {
        private final Map<String, AnAction> registeredActions = new LinkedHashMap<>();
        private final Map<String, AnAction> originalActions = new LinkedHashMap<>();
        private final Map<String, AnAction> replacements = new LinkedHashMap<>();
        private final Map<Editor, KakOnFileOpen.EditorAttachment> editors = new IdentityHashMap<>();
        private TypedActionHandler originalHandler;
        private KakInput input;

        void install() {
            // Registry's disposable overload restores the value that preceded activation.
            Registry.get("actionSystem.playback.typecommand.delay").setValue(1, this);
            Registry.get("editor.block.caret.selection.vim-like").setValue(true, this);
            for (Object[] binding : KeyboardBindings.CustomKakActions) {
                register((String) binding[0], (AnAction) binding[1]);
            }
            ActionManager manager = ActionManager.getInstance();
            for (Object[] binding : KeyboardBindings.DefaultActionOverride) {
                String id = (String) binding[0];
                String backupId = (String) binding[1];
                String implementationId = (String) binding[2];
                AnAction original = manager.getAction(id);
                if (original == null) continue; // Some IDE products omit these actions.
                register(implementationId, new KakCommand(implementationId));
                // A proxy avoids registering the original object under a second ID.
                register(backupId, new KakShortcutAction(original));
                AnAction replacement = new InterceptedAction(original, State.Mode.ALL, List.of(implementationId));
                originalActions.put(id, original);
                replacements.put(id, replacement);
                manager.replaceAction(id, replacement);
            }
            originalHandler = TypedAction.getInstance().getRawHandler();
            input = new KakInput(originalHandler);
            Disposer.register(this, input);
            input.install();
            TypedAction.getInstance().setupRawHandler(input);
            EditorFactory factory = EditorFactory.getInstance();
            factory.addEditorFactoryListener(new EditorFactoryListener() {
                @Override
                public void editorCreated(@NotNull EditorFactoryEvent event) {
                    attach(event.getEditor());
                }

                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                    KakOnFileOpen.EditorAttachment attachment = editors.remove(event.getEditor());
                    if (attachment != null) Disposer.dispose(attachment);
                }
            }, this);
            for (Editor editor : factory.getAllEditors()) attach(editor);
        }

        private void register(String id, AnAction action) {
            ActionManager manager = ActionManager.getInstance();
            if (manager.getAction(id) != null) throw new IllegalStateException("Action already registered: " + id);
            manager.registerAction(id, action);
            registeredActions.put(id, action);
        }

        void attach(Editor editor) {
            if (input == null || editors.containsKey(editor) || !KakOnFileOpen.isSupported(editor)) return;
            KakOnFileOpen.EditorAttachment attachment = new KakOnFileOpen.EditorAttachment(editor, input);
            Disposer.register(this, attachment);
            editors.put(editor, attachment);
            attachment.install();
        }

        @Override
        public void dispose() {
            // Child disposables have already detached editor UI and listeners, and stopped input.
            TypedAction typedAction = TypedAction.getInstance();
            if (input != null && typedAction.getRawHandler() == input) typedAction.setupRawHandler(originalHandler);
            ActionManager manager = ActionManager.getInstance();
            originalActions.forEach((id, original) -> {
                if (manager.getAction(id) == replacements.get(id)) {
                    manager.replaceAction(id, original);
                    // Platform 262 caches the outgoing action in baseActions on every replacement,
                    // including restoration. Replace once more with the same original so that cache
                    // releases our InterceptedAction and its plugin classloader as well.
                    manager.replaceAction(id, original);
                }
            });
            registeredActions.forEach((id, action) -> {
                if (manager.getAction(id) == action) manager.unregisterAction(id);
            });
            editors.clear();
            registeredActions.clear();
            originalActions.clear();
            replacements.clear();
            input = null;
            originalHandler = null;
        }
    }
}
