package me.axebanz.jJK;

import org.bukkit.entity.Player;

public interface Technique {

    String id();
    String displayName();
    String hexColor(); // used by UI

    /**
     * Ability slots:
     * 1, 2, 3 mapped via /technique <slot>
     */
    void castAbility(Player player, AbilitySlot slot);

    default boolean canUse(Player p) {
        return true;
    }
}
