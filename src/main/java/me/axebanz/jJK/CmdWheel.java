package me.axebanz.jJK;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CmdWheel implements CommandExecutor, TabCompleter {

    private final JJKCursedToolsPlugin plugin;
    private final WheelAdaptationManager wheel;
    private final WheelUI ui;

    public CmdWheel(JJKCursedToolsPlugin plugin, WheelAdaptationManager wheel, WheelUI ui) {
        this.plugin = plugin;
        this.wheel = wheel;
        this.ui = ui;
    }

    private boolean noPerm(CommandSender sender) {
        if (sender.hasPermission("jjk.wheel")) return false;
        sender.sendMessage(plugin.cfg().prefix() + "§cNo permission.");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (noPerm(sender)) return true;
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.cfg().prefix() + "§cPlayers only.");
            return true;
        }

        String sub = (args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT));

        switch (sub) {
            case "next" -> {
                if (!wheel.isWearingWheel(p)) { ui.showNotWearing(p); return true; }
                AdaptationCategory next = wheel.nextMode(p.getUniqueId());
                wheel.setMode(p.getUniqueId(), next);
                wheel.showWheelHud(p);
                return true;
            }
            case "set" -> {
                if (!wheel.isWearingWheel(p)) { ui.showNotWearing(p); return true; }
                if (args.length < 2) {
                    p.sendMessage(plugin.cfg().prefix() + "§cUsage: /wheel set <category>");
                    return true;
                }
                AdaptationCategory cat = AdaptationCategory.from(args[1]);
                wheel.setMode(p.getUniqueId(), cat);
                wheel.showWheelHud(p);
                return true;
            }
            case "status" -> {
                wheel.showWheelHud(p);
                return true;
            }
            default -> {
                p.sendMessage(plugin.cfg().prefix() + "§cUsage: /wheel <next|set|status>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (!sender.hasPermission("jjk.wheel")) return List.of();

        if (args.length == 1) {
            return List.of("next", "set", "status");
        }
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            List<String> out = new ArrayList<>();
            for (AdaptationCategory c : AdaptationCategory.values()) out.add(c.name().toLowerCase(Locale.ROOT));
            return out;
        }
        return List.of();
    }
}
