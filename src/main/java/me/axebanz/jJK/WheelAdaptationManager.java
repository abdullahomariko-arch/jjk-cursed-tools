package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class WheelAdaptationManager {

    private final JJKCursedToolsPlugin plugin;
    private final WheelUI ui;
    private final PlayerDataStore store;
    private final CursedToolFactory tools;

    private final WheelDamageClassifier classifier = new WheelDamageClassifier();

    private final Map<UUID, Boolean> wearingCache = new ConcurrentHashMap<>();
    private int taskId = -1;

    public WheelAdaptationManager(JJKCursedToolsPlugin plugin, WheelUI ui, PlayerDataStore store, CursedToolFactory tools) {
        this.plugin = plugin;
        this.ui = ui;
        this.store = store;
        this.tools = tools;
    }

    public void start() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID u = p.getUniqueId();
                boolean wearing = isWearingWheel(p);

                Boolean prev = wearingCache.put(u, wearing);
                if (prev != null && prev && !wearing) {
                    clearStacks(u);
                    store.save(u);
                } else if (prev == null) {
                    wearingCache.put(u, wearing);
                }
            }
        }, 20L, 20L);
    }

    public boolean isWearingWheel(Player p) {
        ItemStack helmet = p.getInventory().getHelmet();
        return tools.identify(helmet) == ToolId.DIVINE_WHEEL;
    }

    public AdaptationCategory getMode(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        return AdaptationCategory.from(prof.wheelMode);
    }

    public void setMode(UUID uuid, AdaptationCategory mode) {
        PlayerProfile prof = store.get(uuid);
        prof.wheelMode = mode.name();
        store.save(uuid);
    }

    public List<AdaptationCategory> enabledCategories() {
        ConfigurationSection sec = plugin.cfg().c().getConfigurationSection("tools.divine_wheel.wheel");
        if (sec == null) return List.of(AdaptationCategory.MELEE, AdaptationCategory.PROJECTILE, AdaptationCategory.TECHNIQUE);

        List<String> list = sec.getStringList("categories");
        if (list == null || list.isEmpty()) {
            return List.of(AdaptationCategory.MELEE, AdaptationCategory.PROJECTILE, AdaptationCategory.EXPLOSION, AdaptationCategory.FIRE, AdaptationCategory.LIGHTNING, AdaptationCategory.TECHNIQUE);
        }

        List<AdaptationCategory> out = new ArrayList<>();
        for (String s : list) out.add(AdaptationCategory.from(s));
        return out;
    }

    public int maxStacks() {
        return plugin.cfg().c().getInt("tools.divine_wheel.wheel.maxStacks", 5);
    }

    public double reductionPerStack() {
        return plugin.cfg().c().getDouble("tools.divine_wheel.wheel.reductionPerStack", 0.10);
    }

    public double maxReduction() {
        return plugin.cfg().c().getDouble("tools.divine_wheel.wheel.maxReduction", 0.60);
    }

    public void clearStacks(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        prof.wheelStacks.clear();
    }

    public int getStacks(UUID uuid, AdaptationCategory cat) {
        PlayerProfile prof = store.get(uuid);
        return Math.max(0, prof.wheelStacks.getOrDefault(cat.name(), 0));
    }

    public void incrementStacks(UUID uuid, AdaptationCategory cat) {
        PlayerProfile prof = store.get(uuid);
        int cur = Math.max(0, prof.wheelStacks.getOrDefault(cat.name(), 0));
        int next = Math.min(maxStacks(), cur + 1);
        prof.wheelStacks.put(cat.name(), next);
    }

    public void handleIncomingDamage(EntityDamageByEntityEvent e, Player victim) {
        if (e.isCancelled()) return;

        UUID u = victim.getUniqueId();
        if (!isWearingWheel(victim)) return;

        AdaptationCategory incoming = classifier.classify(e);
        AdaptationCategory mode = getMode(u);

        if (incoming != mode) return;

        incrementStacks(u, mode);
        int stacks = getStacks(u, mode);

        double reduction = Math.min(maxReduction(), stacks * reductionPerStack());
        double newDamage = e.getDamage() * Math.max(0.0, 1.0 - reduction);
        e.setDamage(newDamage);

        ui.showStackGain(victim, mode, stacks, maxStacks());
    }

    public AdaptationCategory nextMode(UUID uuid) {
        List<AdaptationCategory> cats = enabledCategories();
        AdaptationCategory current = getMode(uuid);

        int idx = cats.indexOf(current);
        if (idx < 0) idx = 0;
        idx = (idx + 1) % cats.size();
        return cats.get(idx);
    }

    public void showWheelHud(Player p) {
        if (!isWearingWheel(p)) {
            ui.showNotWearing(p);
            return;
        }
        UUID u = p.getUniqueId();
        AdaptationCategory mode = getMode(u);
        int stacks = getStacks(u, mode);
        ui.showMode(p, mode, stacks, maxStacks());
    }
}
