package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExtinguishStatusCommand implements CommandExecutor {
    private final Main plugin;

    public ExtinguishStatusCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut etre utilisee que par un joueur.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("plugin.startfire")) {
            player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        Material mat = plugin.getExtinguisherMaterial();
        int delay = plugin.getExtinguishDelaySeconds();

        if (mat == null || mat == Material.AIR) {
            player.sendMessage(ChatColor.YELLOW
                    + "Aucun outil d'extinction configure. Utilise /extinguishitem <materialId> <delaiSecondes>.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Configuration d'extinction actuelle :");
        player.sendMessage(ChatColor.GRAY + "- Item: " + ChatColor.WHITE + mat.name() + ChatColor.GRAY + " (id "
                + mat.getId() + ")");
        if (delay == 0) {
            player.sendMessage(ChatColor.GRAY + "- Delai: " + ChatColor.WHITE + "instantane (0 seconde)");
        } else {
            player.sendMessage(ChatColor.GRAY + "- Delai: " + ChatColor.WHITE + delay + " seconde(s)");
        }
        return true;
    }
}
