package fr.incendie;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FireInteractListener implements Listener {
    private Main plugin;
    private boolean pendingExtinguish;

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
            p.sendMessage("Aucun outil d'extinction configure. Utilisez /extinguishitem <materialId> <delaiSecondes>.");
            return;
        }
        if (hand.getType() != exting) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Block targetFire = findTargetFire(event, p);

        if (targetFire == null) {
            return;
        }

        event.setCancelled(true);

        if (pendingExtinguish) {
            p.sendMessage("Une extinction est deja en cours. Attends la fin avant d'en lancer une autre.");
            return;
        }
        pendingExtinguish = true;

        FireZone matchedZone = null;
        for (FireZone z : plugin.getFireZones()) {
            if (z.contains(targetFire.getLocation())) {
                matchedZone = z;
                break;
            }
        }

        final Block fireToExtinguish = targetFire;
        final FireZone zoneForExtinguish = matchedZone;
        final Runnable extinguishTask = () -> {
            try {
                if (fireToExtinguish.getType() == Material.FIRE) {
                    fireToExtinguish.setType(Material.AIR);
                }
                if (zoneForExtinguish != null) {
                    boolean controlledNow = zoneForExtinguish.registerManualExtinguish(fireToExtinguish.getLocation(),
                            System.currentTimeMillis());
                    if (controlledNow) {
                        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Feu maitrise dans la zone '"
                                + zoneForExtinguish.getName()
                                + "' : plus de propagation ni rallumage pendant 30 minutes.");
                    }
                }
                plugin.saveStateNow();
            } finally {
                pendingExtinguish = false;
            }
        };

        int delaySeconds = plugin.getExtinguishDelaySeconds();
        long delayTicks = delaySeconds * 20L;
        if (delayTicks <= 0L) {
            extinguishTask.run();
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, extinguishTask, delayTicks);
        }

        if (matchedZone != null) {
            if (delaySeconds == 0) {
                p.sendMessage("La flamme de la zone '" + matchedZone.getName() + "' est eteinte instantanement.");
            } else {
                p.sendMessage("La flamme de la zone '" + matchedZone.getName() + "' s'eteindra dans " + delaySeconds
                        + " seconde(s).");
            }
        } else {
            if (delaySeconds == 0) {
                p.sendMessage("La flamme est eteinte instantanement.");
            } else {
                p.sendMessage("La flamme s'eteindra dans " + delaySeconds + " seconde(s).");
            }
        }
    }

    private Block findTargetFire(PlayerInteractEvent event, Player player) {
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            if (clicked.getType() == Material.FIRE) {
                return clicked;
            }

            // Dans beaucoup de cas, le clic droit sur le feu retourne le bloc support.
            Block above = clicked.getRelative(BlockFace.UP);
            if (above.getType() == Material.FIRE) {
                return above;
            }

            if (event.getBlockFace() != null) {
                Block relative = clicked.getRelative(event.getBlockFace());
                if (relative.getType() == Material.FIRE) {
                    return relative;
                }
            }
        }

        // Solution de secours quand Bukkit remonte un clic droit dans l'air.
        Block lookedAt = player.getTargetBlock(null, 5);
        if (lookedAt != null && lookedAt.getType() == Material.FIRE) {
            return lookedAt;
        }

        return null;
    }
}
