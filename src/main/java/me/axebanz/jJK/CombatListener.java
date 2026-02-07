package me.axebanz.jJK;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;

public final class CombatListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final AbilityService abilities;
    private final CursedToolFactory tools;
    private final RegenLockManager regenLock;
    private final NullifyManager nullify;

    public CombatListener(JJKCursedToolsPlugin plugin, AbilityService abilities, CursedToolFactory tools, RegenLockManager regenLock, NullifyManager nullify) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.tools = tools;
        this.regenLock = regenLock;
        this.nullify = nullify;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        if (e.getEntity() instanceof Player victim) {
            plugin.wheel().handleIncomingDamage(e, victim);
        }

        if (!(e.getDamager() instanceof Player attacker)) return;

        ItemStack it = attacker.getInventory().getItemInMainHand();
        ToolId id = tools.identify(it);

        Entity victimEnt = e.getEntity();

        DashState.DashInfo dash = DashState.get(attacker.getUniqueId());
        if (dash != null && System.currentTimeMillis() <= dash.untilMs) {
            double bonus = dash.bonusHearts * 2.0;
            e.setDamage(e.getDamage() + bonus);
            DashState.clear(attacker.getUniqueId());
        }

        // ✅ Split Soul Katana ON-HIT
        if (id == ToolId.SPLIT_SOUL_KATANA && victimEnt instanceof LivingEntity le) {
            e.setCancelled(true);
            abilities.handleSplitSoulHit(attacker, le);
            return;
        }

        if (id == ToolId.INVERTED_SPEAR && victimEnt instanceof Player vp) {
            abilities.handleIsohHit(attacker, vp);
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!regenLock.isLocked(p.getUniqueId())) return;
        e.setCancelled(true);
    }
}
