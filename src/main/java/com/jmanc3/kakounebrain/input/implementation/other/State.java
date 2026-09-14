package com.jmanc3.kakounebrain.input.implementation.other;

import com.intellij.openapi.editor.CaretVisualAttributes;
import com.intellij.ui.JBColor;

public class State {

    public static final CaretVisualAttributes NORMAL_CARET = new CaretVisualAttributes(JBColor.RED,
            CaretVisualAttributes.Weight.HEAVY, CaretVisualAttributes.Shape.BLOCK, 1F);
    public static final CaretVisualAttributes INSERT_CARET = new CaretVisualAttributes(JBColor.GREEN,
            CaretVisualAttributes.Weight.HEAVY, CaretVisualAttributes.Shape.BLOCK, 1F);

    public enum Mode {
        NORMAL,
        INSERT,
        ALL
    }

    public Mode mode = Mode.NORMAL;

    // Used by KAK_SAVE_MODE/KAK_RELOAD_MODE (for f6 refactoring)
    public Mode savedMode = Mode.NORMAL;

    public int previousTextChange = 0;

    // Kakoune's search buffer, populated by the * command.
    public String searchBuffer = "";

    public int pendingJOffset = -1;
    public long pendingJStamp = -1;

    public void resetTyping() {
        pendingJOffset = -1;
        pendingJStamp = -1;
    }

}
