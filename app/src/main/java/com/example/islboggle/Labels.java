package com.example.islboggle;

/**
 * Maps model output indices to word labels for the sequence model.
 */
public final class Labels {
    private Labels() {}

    public static final String[] LABELS = new String[] {
            "GIRL", "FOOD", "DEER",   // Level 1
            "BOOK", "RAIN", "LOVE",   // Level 2 & 3
            "BALL", "CALL", "BEAR",   // Level 4
            "COOL", "DOOR", "GIFT"    // Level 5 (Optional)
    };

    public static String forIndex(int idx) {
        if (idx < 0 || idx >= LABELS.length) return "";
        return LABELS[idx];
    }
}
