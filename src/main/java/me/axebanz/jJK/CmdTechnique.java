package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class CmdTechnique implements SubCommand {

    private final JJKCursedToolsPlugin plugin;
    public CmdTechnique(JJKCursedToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "technique"; }
    @Override public String permission() { return "jjk.technique"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String pref = plugin.cfg().prefix();

        // PLAYER CAST: /technique 1|2|3
        if (sender instanceof Player player && args.length == 2) {
            try {
                int num = Integer.parseInt(args[1]);
                AbilitySlot slot = AbilitySlot.fromInt(num);
                if (slot == null) return true;

                plugin.techniqueManager().cast(player, slot);
                return true;
            } catch (NumberFormatException ignored) {
            }
        }

        // ADMIN COMMANDS
        if (args.length < 3) {
            sender.sendMessage(pref + "§cUsage: /jjk technique <set|enable|disable|toggle> <player> [technique]");
            return true;
        }

        String mode = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(pref + "§cPlayer not found.");
            return true;
        }

        switch (mode) {
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(pref + "§cUsage: /jjk technique set <player> <technique>");
                    return true;
                }
                String techId = args[3];
                if (plugin.techniques().get(techId) == null) {
                    sender.sendMessage(pref + "§cUnknown technique. Available: " +
                            String.join(", ", plugin.techniques().ids()));
                    return true;
                }
                plugin.techniqueManager().setTechnique(target.getUniqueId(), techId);
                sender.sendMessage(pref + "§aSet technique of §f" + target.getName() + " §ato §f" + techId + "§a.");
            }
            case "enable" -> {
                plugin.techniqueManager().setEnabled(target.getUniqueId(), true);
                plugin.techniqueManager().notifyTechniqueState(target);
            }
            case "disable" -> {
                plugin.techniqueManager().setEnabled(target.getUniqueId(), false);
                plugin.techniqueManager().notifyTechniqueState(target);
            }
            case "toggle" -> {
                boolean now = !plugin.techniqueManager().isEnabled(target.getUniqueId());
                plugin.techniqueManager().setEnabled(target.getUniqueId(), now);
                plugin.techniqueManager().notifyTechniqueState(target);
            }
            default -> sender.sendMessage(pref + "§cUnknown technique command.");
        }

        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("set", "enable", "disable", "toggle");
        }
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            return new ArrayList<>(plugin.techniques().ids());
        }
        return List.of();
    }
}
