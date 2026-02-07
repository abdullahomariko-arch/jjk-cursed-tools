package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;

public final class WheelDamageClassifier {

    public AdaptationCategory classify(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            return AdaptationCategory.FIRE;
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return AdaptationCategory.EXPLOSION;
        }

        if (cause == EntityDamageEvent.DamageCause.LIGHTNING) {
            return AdaptationCategory.LIGHTNING;
        }

        if (e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEnt) {
            Entity damager = byEnt.getDamager();
            if (damager instanceof Projectile) return AdaptationCategory.PROJECTILE;
        }

        return AdaptationCategory.MELEE;
    }
}
