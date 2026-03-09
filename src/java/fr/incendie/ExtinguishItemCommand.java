package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExtinguishItemCommand implements CommandExecutor {
    private Main plugin;

    public ExtinguishItemCommand(Main plugin) {
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

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <materialId> <delaiSecondes>");
            return true;
        }

        String raw = args[0];
        Material mat = null;

        // Support legacy 1.12 : autoriser les IDs numeriques de material (ex. 280 pour STICK).
        try {
            int numericId = Integer.parseInt(raw);
            mat = Material.getMaterial(numericId);
        } catch (NumberFormatException ignored) {
            // Pas un nombre : utiliser le nom de material en secours
        }

        if (mat == null) {
            try {
                mat = Material.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Gere plus bas
            }
        }

        if (mat == null || mat == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Material invalide. Utilise un nom (ex: STICK) ou un id numerique (ex: 280). ");
            return true;
        }

        int delaySeconds;
        try {
            delaySeconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Le delai doit etre un nombre entier de secondes (0 = instantane).");
            return true;
        }

        if (delaySeconds < 0) {
            player.sendMessage(ChatColor.RED + "Le delai ne peut pas etre negatif.");
            return true;
        }

        plugin.setExtinguisherMaterial(mat);
        plugin.setExtinguishDelaySeconds(delaySeconds);

        if (delaySeconds == 0) {
            player.sendMessage(ChatColor.GREEN + "Materiel d'extinction defini sur " + mat.name() + " (id " + mat.getId()
                    + "), extinction instantanee.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Materiel d'extinction defini sur " + mat.name() + " (id " + mat.getId()
                    + "), extinction en " + delaySeconds + " seconde(s).");
        }
        return true;
    }
}