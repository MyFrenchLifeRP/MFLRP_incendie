package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FirebreakStatusCommand implements CommandExecutor {
    private final Main plugin;

    public FirebreakStatusCommand(Main plugin) {
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

        Material mat = plugin.getFirebreakMaterial();
        int delay = plugin.getFirebreakDelaySeconds();

        if (mat == null || mat == Material.AIR) {
            player.sendMessage(ChatColor.YELLOW
                    + "Aucun outil de pare-feu configure. Utilise /firebreakitem <materialId> <delaiSecondes>.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Configuration de pare-feu actuelle :");
        player.sendMessage(ChatColor.GRAY + "- Item: " + ChatColor.WHITE + mat.name()
                + ChatColor.GRAY + " (id " + mat.getId() + ")");
        if (delay == 0) {
            player.sendMessage(ChatColor.GRAY + "- Delai: " + ChatColor.WHITE + "instantane (0 seconde)");
        } else {
            player.sendMessage(ChatColor.GRAY + "- Delai: " + ChatColor.WHITE + delay + " seconde(s)");
        }
        return true;
    }
}
