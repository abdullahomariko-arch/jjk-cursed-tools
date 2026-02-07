package me.axebanz.jJK;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class ToolUseListener implements Listener {

    private final JJKCursedToolsPlugin plugin;
    private final AbilityService abilities;
    private final CursedToolFactory tools;

    public ToolUseListener(JJKCursedToolsPlugin plugin, AbilityService abilities, CursedToolFactory tools) {
        this.plugin = plugin;
        this.abilities = abilities;
        this.tools = tools;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack it = p.getInventory().getItemInMainHand();

        ToolId id = tools.identify(it);
        if (id == null) return;

        // Use ability
        abilities.tryUseAbility(p, id, it);

        // prevent accidental block interaction spam for swords etc
        e.setCancelled(true);
    }
}

