package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TechniqueManager {

    private final JJKCursedToolsPlugin plugin;
    private final TechniqueRegistry registry;
    private final PlayerDataStore store;

    public TechniqueManager(JJKCursedToolsPlugin plugin, TechniqueRegistry registry, PlayerDataStore store) {
        this.plugin = plugin;
        this.registry = registry;
        this.store = store;
    }

    public Technique getAssigned(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return null;
        return registry.get(prof.techniqueId);
    }

    public String getAssignedId(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        return prof == null ? null : prof.techniqueId;
    }

    public void setTechnique(UUID uuid, String id) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return;
        prof.techniqueId = id;
        store.save(uuid);
    }

    public boolean isEnabled(UUID uuid) {
        PlayerProfile prof = store.get(uuid);
        return prof != null && prof.techniqueEnabled;
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        PlayerProfile prof = store.get(uuid);
        if (prof == null) return;
        prof.techniqueEnabled = enabled;
        store.save(uuid);
    }

    public boolean canUseTechnique(Player p) {
        UUID uuid = p.getUniqueId();
        if (plugin.nullify().isNullified(uuid)) return false;
        if (!isEnabled(uuid)) return false;

        Technique t = getAssigned(uuid);
        return t != null && t.canUse(p);
    }

    public void cast(Player player, AbilitySlot slot) {
        if (slot == null) return;
        if (!canUseTechnique(player)) return;

        Technique t = getAssigned(player.getUniqueId());
        if (t == null) return;

        t.castAbility(player, slot);
    }

    public String techniqueColorHex(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t == null ? "#AAAAAA" : t.hexColor();
    }

    public String techniqueName(UUID uuid) {
        Technique t = getAssigned(uuid);
        return t == null ? "None" : t.displayName();
    }

    public void notifyTechniqueState(Player p) {
        String prefix = plugin.cfg().prefix();
        p.sendMessage(prefix + "§7Technique: §f" + techniqueName(p.getUniqueId()) +
                " §8|§r " + (isEnabled(p.getUniqueId()) ? "§aENABLED" : "§cDISABLED"));
    }

    public Player getOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }
}
