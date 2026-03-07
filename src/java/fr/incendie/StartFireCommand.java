package fr.incendie;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class StartFireCommand implements CommandExecutor {

    private Main plugin;

    public StartFireCommand(Main plugin) {
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

        // Obtenir la sélection WorldEdit
        WorldEditPlugin we = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            player.sendMessage("WorldEdit n'est pas installé.");
            return true;
        }

        try {
            LocalSession session = we.getSession(player);
            Region region = session.getSelection(new BukkitWorld(player.getWorld()));

            if (region == null) {
                player.sendMessage("Veuillez sélectionner une zone avec WorldEdit.");
                return true;
            }

            // Calculer le centre de la région
            double centerX = (region.getMinimumPoint().getX() + region.getMaximumPoint().getX()) / 2.0;
            double centerY = (region.getMinimumPoint().getY() + region.getMaximumPoint().getY()) / 2.0;
            double centerZ = (region.getMinimumPoint().getZ() + region.getMaximumPoint().getZ()) / 2.0;
            Location center = new Location(player.getWorld(), centerX, centerY, centerZ);

            // Créer la zone d'incendie
            FireZone fireZone = new FireZone(center, 10);
            plugin.addFireZone(fireZone);

            // Alerte
            plugin.getServer().broadcastMessage("§cAlerte incendie ! Coordonnées : " + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());

            player.sendMessage("Incendie démarré dans la zone sélectionnée.");

        } catch (Exception e) {
            player.sendMessage("Erreur lors de la récupération de la sélection WorldEdit.");
            e.printStackTrace();
        }

        return true;
    }
}