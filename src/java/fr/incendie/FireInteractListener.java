package fr.incendie;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FireInteractListener implements Listener {
    private Main plugin;

    public FireInteractListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }
        Material exting = plugin.getExtinguisherMaterial();
        if (exting == null) {
            p.sendMessage("Aucun outil d'extinction configuré. Utilisez /extinguishitem <material>.");
            return;
        }
        if (hand.getType() != exting) {
            return;
        }
        // only extinguish when right-clicking on fire
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
            && event.getClickedBlock() != null
            && event.getClickedBlock().getType() == Material.FIRE) {
            // find zone containing player
            for (FireZone z : plugin.getFireZones()) {
                if (z.getCenter().distance(p.getLocation()) <= z.getRadius()) {
                    z.extinguish();
                    p.sendMessage("Zone '" + z.getName() + "' éteinte.");
                    break;
                }
            }
        }
    }
}