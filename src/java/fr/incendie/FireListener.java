package fr.incendie;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class FireListener implements Listener {

    private final Main plugin;

    public FireListener(Main plugin) {
        this.plugin = plugin;
    }

    /** Retourne true si la location est dans la zone horizontale ET dans la plage de hauteur d'au moins une zone d'incendie. */
    private boolean isInsideAnyZone(Location loc) {
        for (FireZone zone : plugin.getFireZones()) {
            if (zone.containsWithHeight(loc)) return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        // Autoriser la destruction des blocs inflammables (bois, feuilles...) à l'intérieur d'une zone d'incendie
        if (block.getType().isBurnable() && isInsideAnyZone(block.getLocation())) {
            return;
        }
        // Annuler pour éviter que les structures soient cassées hors zone
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        // Empêcher le feu de disparaître naturellement
        if (event.getBlock().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE) {
            // event.getBlock() est le bloc AIR qui va devenir du feu, pas le bois adjacent.
            // On autorise simplement si la position est dans une zone d'incendie.
            if (isInsideAnyZone(event.getBlock().getLocation())) {
                return;
            }
            // Bloquer toute propagation naturelle hors zone
            event.setCancelled(true);
        }
    }
}