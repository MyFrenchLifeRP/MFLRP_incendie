package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveFireCommand implements CommandExecutor {
    private Main plugin;

    public RemoveFireCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande ne peut être utilisée que par un joueur.");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("plugin.startfire")) {
            player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <nomZone>");
            return true;
        }
        String name = args[0];
        boolean removed = plugin.removeFireZone(name);
        if (removed) {
            player.sendMessage(ChatColor.GREEN + "Zone d'incendie '" + name + "' supprimée.");
        } else {
            player.sendMessage(ChatColor.RED + "Aucune zone d'incendie trouvée avec ce nom.");
        }
        return true;
    }
}