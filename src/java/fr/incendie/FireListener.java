package fr.incendie;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;

public class FireListener implements Listener {

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        // Annuler l'événement pour éviter que les blocs soient cassés par le feu
        event.setCancelled(true);
    }
}