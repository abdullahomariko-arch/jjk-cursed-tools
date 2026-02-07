package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

public final class AbilityService {

    private final JJKCursedToolsPlugin plugin;
    private final ConfigManager cfg;
    private final TechniqueManager techniqueManager;
    private final CursedEnergyManager ce;
    private final CooldownManager cooldowns;
    private final RegenLockManager regenLock;
    private final NullifyManager nullify;
    private final CursedToolFactory tools;
    private final ActionbarUI actionbar;
    private final BossbarUI bossbar;

    private final Random rng = new Random();

    public AbilityService(
            JJKCursedToolsPlugin plugin,
            ConfigManager cfg,
            TechniqueManager techniqueManager,
            CursedEnergyManager ce,
            CooldownManager cooldowns,
            RegenLockManager regenLock,
            NullifyManager nullify,
            CursedToolFactory tools,
            ActionbarUI actionbar,
            BossbarUI bossbar
    ) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.techniqueManager = techniqueManager;
        this.ce = ce;
        this.cooldowns = cooldowns;
        this.regenLock = regenLock;
        this.nullify = nullify;
        this.tools = tools;
        this.actionbar = actionbar;
        this.bossbar = bossbar;
    }

    /**
     * Called by your interact listener (right-click).
     * IMPORTANT:
     * - Dragon Bone: right-click dash
     * - Kamutoke: right-click storm
     * - Split Soul: ON-HIT ONLY (do nothing here)
     * - ISOH: ON-HIT ONLY (do nothing here)
     */
    public void tryUseAbility(Player p, ToolId toolId, ItemStack itemInHand) {
        if (toolId == null) return;

        switch (toolId) {
            case DRAGON_BONE -> useDragonBone(p);
            case KAMUTOKE -> useKamutoke(p);
            case SPLIT_SOUL_KATANA -> {
                // ON-HIT ONLY
            }
            case INVERTED_SPEAR -> {
                // ON-HIT ONLY
            }
        }
    }

    // ---------- Dragon Bone ----------
    private void useDragonBone(Player p) {
        String key = "dragon_bone.propel_slash";
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.dragon_bone");
        if (sec == null) return;

        long cd = sec.getLong("cooldownSeconds", 25);
        int ceCost = sec.getInt("ceCost", 2);

        if (cooldowns.isOnCooldown(p.getUniqueId(), key)) {
            long rem = cooldowns.remainingSeconds(p.getUniqueId(), key);
            actionbar.setTimer(p.getUniqueId(), key, "■", "§5", rem);
            return;
        }

        if (!ce.tryConsume(p.getUniqueId(), ceCost)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }

        double speed = sec.getDouble("dash.speed", 1.55);
        int durationTicks = sec.getInt("dash.durationTicks", 14);
        double bonusHearts = sec.getDouble("dash.bonusDamageHearts", 3.0);

        Vector dir = p.getLocation().getDirection().clone().normalize();
        p.setVelocity(dir.multiply(speed));
        p.setFallDistance(0);

        long dashUntil = System.currentTimeMillis() + (durationTicks * 50L);
        DashState.set(p.getUniqueId(), dashUntil, bonusHearts);

        Sound s = safeSound(sec.getString("visuals.sound", "ENTITY_IRON_GOLEM_STEP"), Sound.ENTITY_IRON_GOLEM_STEP);
        Particle particle = safeParticle(sec.getString("visuals.particle", "CRIT"), Particle.CRIT);

        p.getWorld().playSound(p.getLocation(), s, 1.0f, 0.8f);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!p.isOnline()) { task.cancel(); return; }
            if (System.currentTimeMillis() > dashUntil) { task.cancel(); return; }
            Location loc = p.getLocation().clone().add(0, 1.0, 0);
            p.getWorld().spawnParticle(particle, loc, 10, 0.15, 0.15, 0.15, 0.02);
        }, 0L, 1L);

        cooldowns.setCooldown(p.getUniqueId(), key, cd);
        actionbar.setTimer(p.getUniqueId(), key, "■", "§5", cd);
        if (cd >= cfg.cooldownBossbarThreshold()) {
            bossbar.showCooldownBossbar(p, "§5Propel Slash §8|§r {time}", BarColor.PURPLE, cd);
        }
    }

    // ---------- Split Soul (ON-HIT ONLY) ----------
    public void handleSplitSoulHit(Player attacker, LivingEntity target) {
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.split_soul_katana");
        if (sec == null) return;

        String key = "split_soul.true_slash";
        long cd = sec.getLong("cooldownSeconds", 65);

        if (cooldowns.isOnCooldown(attacker.getUniqueId(), key)) {
            long rem = cooldowns.remainingSeconds(attacker.getUniqueId(), key);
            actionbar.setTimer(attacker.getUniqueId(), key, "■", "§f", rem);
            return;
        }

        double trueDamageHearts = sec.getDouble("trueDamageHearts", 4.0);
        int noRegen = sec.getInt("noRegenSeconds", 15);

        // ✅ TRUE DAMAGE to players: direct health reduction bypasses armor.
        // ✅ Mobs: normal damage() so it behaves naturally.
        if (target instanceof Player tp) {
            double dmg = trueDamageHearts * 2.0;
            tp.setHealth(Math.max(0.0, tp.getHealth() - dmg));
            regenLock.lock(tp.getUniqueId(), noRegen);
        } else {
            target.damage(trueDamageHearts * 2.0, attacker);
        }

        Particle particle = safeParticle(sec.getString("visuals.particle", "SWEEP_ATTACK"), Particle.SWEEP_ATTACK);
        Sound sound = safeSound(sec.getString("visuals.sound", "ENTITY_WITHER_HURT"), Sound.ENTITY_WITHER_HURT);

        Location mid = target.getLocation().clone().add(0, 1.0, 0);
        target.getWorld().spawnParticle(particle, mid, 12, 0.25, 0.25, 0.25, 0.01);
        target.getWorld().playSound(target.getLocation(), sound, 1.0f, 0.9f);

        cooldowns.setCooldown(attacker.getUniqueId(), key, cd);
        actionbar.setTimer(attacker.getUniqueId(), key, "■", "§f", cd);
        if (cd >= cfg.cooldownBossbarThreshold()) {
            bossbar.showCooldownBossbar(attacker, "§fSplit Soul §8|§r {time}", BarColor.WHITE, cd);
        }
    }

    // ---------- Kamutoke ----------
    private void useKamutoke(Player p) {
        String key = "kamutoke.storm_call";
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.kamutoke");
        if (sec == null) return;

        long cd = sec.getLong("cooldownSeconds", 120);
        int ceCost = sec.getInt("ceCost", 3);

        if (cooldowns.isOnCooldown(p.getUniqueId(), key)) {
            long rem = cooldowns.remainingSeconds(p.getUniqueId(), key);
            actionbar.setTimer(p.getUniqueId(), key, "■", "§b", rem);
            return;
        }

        if (!ce.tryConsume(p.getUniqueId(), ceCost)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }

        int strikes = sec.getInt("lightning.strikes", 7);
        double radius = sec.getDouble("lightning.radius", 7.0);
        double minDamageHearts = sec.getDouble("lightning.minDamageHearts", 3.0);
        int delayTicks = sec.getInt("lightning.strikeDelayTicks", 6);

        Particle cloud = safeParticle(sec.getString("visuals.cloudParticle", "CLOUD"), Particle.CLOUD);
        Sound thunder = safeSound(sec.getString("visuals.sound", "ENTITY_LIGHTNING_BOLT_THUNDER"), Sound.ENTITY_LIGHTNING_BOLT_THUNDER);

        p.getWorld().playSound(p.getLocation(), thunder, 1.0f, 0.8f);

        Location cloudLoc = p.getLocation().clone().add(0, 4.0, 0);
        p.getWorld().spawnParticle(cloud, cloudLoc, 80, 1.2, 0.3, 1.2, 0.02);

        for (int i = 0; i < strikes; i++) {
            int tickDelay = i * delayTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) return;

                Location center = p.getLocation();
                Location strikeLoc = center.clone().add(rand(-radius, radius), 0, rand(-radius, radius));
                strikeLoc.setY(center.getY());

                p.getWorld().strikeLightningEffect(strikeLoc);
                p.getWorld().playSound(strikeLoc, thunder, 0.9f, 1.0f);

                for (Player victim : p.getWorld().getPlayers()) {
                    if (victim.equals(p)) continue;
                    if (victim.getLocation().distanceSquared(strikeLoc) <= 2.5 * 2.5) {
                        victim.damage(minDamageHearts * 2.0, p);
                    }
                }
            }, tickDelay);
        }

        cooldowns.setCooldown(p.getUniqueId(), key, cd);
        actionbar.setTimer(p.getUniqueId(), key, "■", "§b", cd);
        if (cd >= cfg.cooldownBossbarThreshold()) {
            bossbar.showCooldownBossbar(p, "§bKamutoke §8|§r {time}", BarColor.BLUE, cd);
        }
    }

    // ---------- ISOH On-hit ----------
    public void handleIsohHit(Player attacker, Player victim) {
        ConfigurationSection sec = cfg.c().getConfigurationSection("tools.inverted_spear");
        if (sec == null) return;

        int nullifySeconds = sec.getInt("nullifySeconds", 600);

        if (!plugin.techniqueManager().isEnabled(victim.getUniqueId())) return;
        if (plugin.techniqueManager().getAssigned(victim.getUniqueId()) == null) return;

        nullify.applyNullify(victim, attacker, nullifySeconds);

        bossbar.showNullifiedTitle(victim);

        Particle particle = safeParticle(sec.getString("visuals.particle", "ENCHANT"), Particle.ENCHANT);
        Sound sound = safeSound(sec.getString("visuals.sound", "BLOCK_END_PORTAL_SPAWN"), Sound.BLOCK_END_PORTAL_SPAWN);

        victim.getWorld().spawnParticle(particle, victim.getLocation().clone().add(0, 1.0, 0), 80, 0.7, 0.4, 0.7, 0.02);
        victim.getWorld().playSound(victim.getLocation(), sound, 1.0f, 0.8f);

        actionbar.setTimer(victim.getUniqueId(), "nullified.timer", "■", "§c", nullifySeconds);
        actionbar.setTimer(attacker.getUniqueId(), "isoh.reapply", "■", "§6", nullifySeconds);
    }

    private double rand(double min, double max) {
        return min + (max - min) * rng.nextDouble();
    }

    /**
     * ✅ Paper-safe lookup
     */
    private Particle safeParticle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;

        String lowered = name.trim().toLowerCase();
        String keyStr = lowered.contains(":") ? lowered : "minecraft:" + lowered;

        NamespacedKey key = NamespacedKey.fromString(keyStr);
        if (key == null) return fallback;

        Particle p = Registry.PARTICLE_TYPE.get(key);
        return (p != null) ? p : fallback;
    }

    /**
     * ✅ Paper-safe lookup
     */
    private Sound safeSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) return fallback;

        String lowered = name.trim().toLowerCase();

        String keyStr;
        if (lowered.contains(":")) {
            keyStr = lowered;
        } else if (lowered.contains("_")) {
            keyStr = "minecraft:" + lowered.replace('_', '.');
        } else {
            keyStr = "minecraft:" + lowered;
        }

        NamespacedKey key = NamespacedKey.fromString(keyStr);
        if (key == null) return fallback;

        Sound s = Registry.SOUNDS.get(key);
        return (s != null) ? s : fallback;
    }
}
