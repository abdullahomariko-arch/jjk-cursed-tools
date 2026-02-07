package me.axebanz.jJK;

public enum ToolId {
    DRAGON_BONE("dragon_bone"),
    SPLIT_SOUL_KATANA("split_soul_katana"),
    KAMUTOKE("kamutoke"),
    INVERTED_SPEAR("inverted_spear"),

    DIVINE_WHEEL("divine_wheel");

    public final String id;
    ToolId(String id) { this.id = id; }

    public static ToolId from(String s) {
        if (s == null) return null;
        for (ToolId t : values()) if (t.id.equalsIgnoreCase(s)) return t;
        return null;
    }
}
