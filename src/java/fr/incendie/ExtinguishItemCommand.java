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

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /" + label + " <materialId>");
            return true;
        }
        Material mat;
        try {
            mat = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + "Material invalide.");
            return true;
        }
        plugin.setExtinguisherMaterial(mat);
        player.sendMessage(ChatColor.GREEN + "Matériel d'extinction défini sur " + mat.name() + ".");
        return true;
    }
}