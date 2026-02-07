package me.axebanz.jJK;

public enum AdaptationCategory {
    MELEE,
    PROJECTILE,
    EXPLOSION,
    FIRE,
    LIGHTNING,
    TECHNIQUE,
    TRUE_DAMAGE;

    public static AdaptationCategory from(String s) {
        if (s == null) return MELEE;
        try { return AdaptationCategory.valueOf(s.toUpperCase()); }
        catch (Exception ignored) { return MELEE; }
    }
}
