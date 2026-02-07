package me.axebanz.jJK;

import org.bukkit.Bukkit;

import java.util.UUID;

public final class CursedEnergyManager {

    private final JJKCursedToolsPlugin plugin;
    private final PlayerDataStore store;
    private int taskId = -1;

    public CursedEnergyManager(JJKCursedToolsPlugin plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public int get(UUID uuid) {
        return store.get(uuid).ce;
    }

    public void ensureInitialized(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        int max = plugin.cfg().ceMax();
        if (prof.ce <= 0) prof.ce = max;
        if (prof.ce > max) prof.ce = max;
        store.save(uuid);
    }

    public boolean tryConsume(UUID uuid, int amount) {
        if (amount <= 0) return true;
        PlayerProfile prof = store.get(uuid);
        if (prof.ce < amount) return false;
        prof.ce -= amount;
        store.save(uuid);
        return true;
    }

    public void add(UUID uuid, int amount) {
        if (amount <= 0) return;
        int max = plugin.cfg().ceMax();
        PlayerProfile prof = store.get(uuid);
        prof.ce = Math.min(max, prof.ce + amount);
        store.save(uuid);
    }

    public void startRegenTask() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);

        int seconds = plugin.cfg().ceRegenTickSeconds();
        int amt = plugin.cfg().ceRegenAmount();

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!plugin.cfg().ceBossbarEnabled()) return;
                add(p.getUniqueId(), amt);
            });
        }, 20L * seconds, 20L * seconds);
    }
}
