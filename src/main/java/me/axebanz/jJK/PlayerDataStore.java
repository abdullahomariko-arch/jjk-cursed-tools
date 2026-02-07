package me.axebanz.jJK;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataStore {

    private final JJKCursedToolsPlugin plugin;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public PlayerDataStore(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
        ensureFolders();
    }

    private void ensureFolders() {
        File dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    public PlayerProfile get(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> new PlayerProfile(u));
    }

    public void load(UUID uuid) {
        ensureFolders();
        File file = file(uuid);
        PlayerProfile prof = get(uuid);
        if (!file.exists()) return;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        prof.ce = y.getInt("ce", 10);
        prof.techniqueId = y.getString("technique.id", null);
        prof.techniqueEnabled = y.getBoolean("technique.enabled", false);

        prof.nullifiedUntilMs = y.getLong("nullified.untilMs", 0L);

        prof.cooldowns.clear();
        if (y.isConfigurationSection("cooldowns")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("cooldowns")).getKeys(false)) {
                long v = y.getLong("cooldowns." + k, 0L);
                if (v > 0) prof.cooldowns.put(k, v);
            }
        }

        prof.regenLockedUntilMs = y.getLong("regenLock.untilMs", 0L);

        prof.isohReapplyUntilMs = y.getLong("isoh.reapplyUntilMs", 0L);
        prof.isohReapplyTargetUuid = y.getString("isoh.reapplyTargetUuid", null);

        // ===== Divine Wheel =====
        prof.wheelMode = y.getString("wheel.mode", "MELEE");
        prof.wheelStacks.clear();
        if (y.isConfigurationSection("wheel.stacks")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("wheel.stacks")).getKeys(false)) {
                int v = y.getInt("wheel.stacks." + k, 0);
                if (v > 0) prof.wheelStacks.put(k, v);
            }
        }
    }

    public void save(UUID uuid) {
        ensureFolders();
        PlayerProfile prof = get(uuid);
        File file = file(uuid);

        YamlConfiguration y = new YamlConfiguration();
        y.set("ce", prof.ce);

        y.set("technique.id", prof.techniqueId);
        y.set("technique.enabled", prof.techniqueEnabled);

        y.set("nullified.untilMs", prof.nullifiedUntilMs);

        for (Map.Entry<String, Long> e : prof.cooldowns.entrySet()) {
            y.set("cooldowns." + e.getKey(), e.getValue());
        }

        y.set("regenLock.untilMs", prof.regenLockedUntilMs);

        y.set("isoh.reapplyUntilMs", prof.isohReapplyUntilMs);
        y.set("isoh.reapplyTargetUuid", prof.isohReapplyTargetUuid);

        // ===== Divine Wheel =====
        y.set("wheel.mode", prof.wheelMode);
        for (Map.Entry<String, Integer> e : prof.wheelStacks.entrySet()) {
            y.set("wheel.stacks." + e.getKey(), e.getValue());
        }

        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed saving player data for " + uuid + ": " + ex.getMessage());
        }
    }

    private File file(UUID uuid) {
        return new File(new File(plugin.getDataFolder(), "players"), uuid.toString() + ".yml");
    }
}
