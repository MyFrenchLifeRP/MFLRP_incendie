package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // Parser les arguments : <nom> <hauteurMin> <hauteurMax> <tailleMax> <vitessePropagation>
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <nom> <hauteurMin> <hauteurMax> <tailleMax> <vitessePropagation>");
            return true;
        }

        String name = args[0];
        int minH, maxH, maxR, propSpeed;
        try {
            minH = Integer.parseInt(args[1]);
            maxH = Integer.parseInt(args[2]);
            maxR = Integer.parseInt(args[3]);
            propSpeed = Integer.parseInt(args[4]);
        } catch (NumberFormatException nfe) {
            player.sendMessage(ChatColor.RED + "Les hauteurs, la taille et la vitesse doivent être des nombres entiers.");
            return true;
        }

        if (minH > maxH) {
            player.sendMessage(ChatColor.RED + "La hauteur minimale doit être inférieure ou égale à la hauteur maximale.");
            return true;
        }

        if (maxR < 3) {
            player.sendMessage(ChatColor.RED + "La taille maximale doit être au moins 3.");
            return true;
        }

        if (propSpeed < 1) {
            player.sendMessage(ChatColor.RED + "La vitesse de propagation doit être au moins 1 seconde.");
            return true;
        }

        Location center = player.getLocation();

        // Créer la zone d'incendie
        FireZone fireZone = new FireZone(name, center, minH, maxH, maxR, propSpeed);
        plugin.addFireZone(fireZone);

        // Alerte globale
        plugin.getServer().broadcastMessage(ChatColor.RED + "Alerte incendie ! Zone '" + name + "' au centre "
                + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());

        // Retour d'information au joueur
        if (fireZone.getFireCount() == 0) {
            player.sendMessage(ChatColor.YELLOW + "Aucun feu n'a pu être placé : vérifiez que la plage de hauteurs (min/max) contient le terrain autour de vous.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Incendie démarré, zone '" + name + "' (taille max " + maxR + ", 1 flamme/" + propSpeed + "s).");
        }

        return true;
    }
}

