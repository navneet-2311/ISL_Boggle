package com.example.islboggle;

/**
 * Maps model output indices to letter labels.
 */
public final class Labels {
    private Labels() {}

    public static final String[] LABELS = new String[] {
            "A","B","C","D","E","F","G","H","I","J",
            "K","L","M","N","O","P","Q","R","S","T",
            "U","V","W","X","Y","Z"
    };

    public static String forIndex(int idx) {
        if (idx < 0 || idx >= LABELS.length) return "?";
        return LABELS[idx];
    }
}
