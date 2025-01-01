package com.jmanc3.kakounebrain.input.implementation.other;

public class State {


    public enum Mode {
        NORMAL,
        INSERT,
        ALL
    }

    public Mode mode = Mode.NORMAL;

    // Used by KAK_SAVE_MODE/KAK_RELOAD_MODE (for f6 refactoring)
    public Mode savedMode = Mode.NORMAL;

    public int previousTextChange = 0;

}
