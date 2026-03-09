package fr.incendie;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class FireListener implements Listener {

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        // Annuler l'événement pour éviter que les blocs soient cassés par le feu
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
        // Empêcher le feu de se propager naturellement vers les blocs inflammables (bois, planches, etc.)
        if (event.getNewState().getType() == Material.FIRE) {
            event.setCancelled(true);
        }
    }
}