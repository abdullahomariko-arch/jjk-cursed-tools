package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionbarUI {

    private final JJKCursedToolsPlugin plugin;
    private int taskId = -1;

    // Multiple timers possible; we keep a map of "key -> display"
    private final Map<UUID, Map<String, ActionbarTimer>> timers = new ConcurrentHashMap<>();

    public ActionbarUI(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!plugin.cfg().cooldownActionbarEnabled()) return;

            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID u = p.getUniqueId();
                Map<String, ActionbarTimer> map = timers.get(u);
                if (map == null || map.isEmpty()) continue;

                // Remove expired
                map.values().removeIf(t -> t.endsAtMs <= now);

                if (map.isEmpty()) continue;

                ActionbarTimer chosen = choose(map.values(), plugin.cfg().cooldownPreferShortest(), now);
                long remSec = Math.max(0, (chosen.endsAtMs - now) / 1000L);
                String msg = chosen.color + chosen.icon + " §f" + TimeFmt.mmss(remSec);
                p.sendActionBar(msg);
            }
        }, 20L, 20L);
    }

    private ActionbarTimer choose(Collection<ActionbarTimer> values, boolean preferShortest, long now) {
        ActionbarTimer best = null;
        for (ActionbarTimer t : values) {
            if (best == null) { best = t; continue; }
            long remT = t.endsAtMs - now;
            long remB = best.endsAtMs - now;

            if (preferShortest) {
                if (remT < remB) best = t;
            } else {
                if (t.createdAtMs > best.createdAtMs) best = t;
            }
        }
        return best;
    }

    public void setTimer(UUID uuid, String key, String icon, String color, long seconds) {
        timers.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(key, new ActionbarTimer(icon, color, System.currentTimeMillis() + seconds * 1000L));
    }

    public void clear(UUID uuid) {
        timers.remove(uuid);
    }

    private static final class ActionbarTimer {
        final String icon;
        final String color;
        final long createdAtMs;
        final long endsAtMs;

        ActionbarTimer(String icon, String color, long endsAtMs) {
            this.icon = icon;
            this.color = color;
            this.createdAtMs = System.currentTimeMillis();
            this.endsAtMs = endsAtMs;
        }
    }
}
