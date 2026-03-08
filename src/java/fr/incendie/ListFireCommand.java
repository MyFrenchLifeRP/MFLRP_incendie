package fr.incendie;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ListFireCommand implements CommandExecutor {
    private Main plugin;

    public ListFireCommand(Main plugin) {
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

        if (plugin.getFireZones().isEmpty()) {
            player.sendMessage("Aucune zone d'incendie active.");
            return true;
        }

        player.sendMessage("Zones d'incendie actives :");
        for (FireZone z : plugin.getFireZones()) {
            player.sendMessage(" - " + z.getName() + " @ (" + z.getCenter().getBlockX() + "," + z.getCenter().getBlockY() + "," + z.getCenter().getBlockZ() + ") radius=" + z.getRadius() + " max=" + z.getMaxSize());
        }
        return true;
    }
}