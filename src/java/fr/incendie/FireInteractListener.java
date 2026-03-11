package fr.incendie;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FireInteractListener implements Listener {
    private Main plugin;
    private boolean pendingExtinguish;

    /** Joueurs ayant actuellement une action de pare-feu en attente. */
    private final Set<UUID> pendingFirebreak = new HashSet<>();

    /** Blocs sol valides pour l'outil pare-feu (1.12.2 Material names). */
    private static final Set<Material> FIREBREAK_VALID_BLOCKS = EnumSet.of(
            Material.GRASS,      // Bloc herbe (id 2)
            Material.DIRT,       // Terre / coarse dirt / podzol (id 3, data 0-2)
            Material.MYCEL,      // Mycelium (id 110)
            Material.GRASS_PATH  // Chemin de terre (id 208)
    );

    /** Duree en minutes avant restauration automatique du bloc pare-feu. */
    private static final int FIREBREAK_RESTORE_MINUTES = 15;

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

        Material extingMat = plugin.getExtinguisherMaterial();
        Material firebreakMat = plugin.getFirebreakMaterial();

        boolean isExtinguishTool = extingMat != null && hand.getType() == extingMat;
        boolean isFirebreakTool  = firebreakMat != null && hand.getType() == firebreakMat;

        if (!isExtinguishTool && !isFirebreakTool) {
            return;
        }

        Action action = event.getAction();

        if (isExtinguishTool && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            handleExtinguish(event, p);
        }

        if (isFirebreakTool && action == Action.RIGHT_CLICK_BLOCK) {
            handleFirebreak(event, p);
        }
    }

    // -------------------------------------------------------------------------
    // Extinction
    // -------------------------------------------------------------------------

    private void handleExtinguish(PlayerInteractEvent event, Player p) {
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
                p.sendMessage("La flamme de la zone '" + matchedZone.getName() + "' s'eteindra dans " + delaySeconds + " seconde(s).");
            }
        } else {
            if (delaySeconds == 0) {
                p.sendMessage("La flamme est eteinte instantanement.");
            } else {
                p.sendMessage("La flamme s'eteindra dans " + delaySeconds + " seconde(s).");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pare-feu
    // -------------------------------------------------------------------------

    private void handleFirebreak(PlayerInteractEvent event, Player p) {
        Block clicked = event.getClickedBlock();

        if (clicked == null || !FIREBREAK_VALID_BLOCKS.contains(clicked.getType())) {
            p.sendMessage(ChatColor.YELLOW + "Cet outil ne fonctionne que sur la terre et l'herbe.");
            return;
        }

        event.setCancelled(true);

        UUID playerId = p.getUniqueId();
        if (pendingFirebreak.contains(playerId)) {
            p.sendMessage(ChatColor.RED + "Tu as deja une action de pare-feu en cours. Attends la fin avant d'en lancer une autre.");
            return;
        }
        pendingFirebreak.add(playerId);

        final Block targetBlock = clicked;
        final Material originalType = clicked.getType();
        final int delaySeconds = plugin.getFirebreakDelaySeconds();
        final long delayTicks = delaySeconds * 20L;

        final Runnable firebreakTask = () -> {
            try {
                if (!FIREBREAK_VALID_BLOCKS.contains(targetBlock.getType())) {
                    // Le bloc a change entre temps, on abandonne
                    return;
                }
                targetBlock.setType(Material.SOIL);
                plugin.addFirebreakBlock(targetBlock.getLocation());

                // Restaurer le bloc d'origine apres 15 minutes
                long restoreTicks = FIREBREAK_RESTORE_MINUTES * 60L * 20L;
                final org.bukkit.Location loc = targetBlock.getLocation();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.removeFirebreakBlock(loc);
                    if (loc.getBlock().getType() == Material.SOIL) {
                        loc.getBlock().setType(originalType);
                    }
                }, restoreTicks);

                Player online = plugin.getServer().getPlayer(playerId);
                if (online != null) {
                    online.sendMessage(ChatColor.GREEN + "Pare-feu installe ! Le bloc sera restaure dans "
                            + FIREBREAK_RESTORE_MINUTES + " minutes.");
                }
            } finally {
                pendingFirebreak.remove(playerId);
            }
        };

        if (delayTicks <= 0L) {
            firebreakTask.run();
        } else {
            p.sendMessage(ChatColor.GREEN + "Pare-feu en cours d'installation... (" + delaySeconds + " seconde(s))");
            plugin.getServer().getScheduler().runTaskLater(plugin, firebreakTask, delayTicks);
        }
    }

    // -------------------------------------------------------------------------
    // Blocage du sechage et pietinement des blocs pare-feu (Farmland)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getType() != Material.SOIL) return;
        if (plugin.isFirebreakBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() != Material.SOIL) return;
        if (event.getTo() != Material.DIRT) return;
        if (plugin.isFirebreakBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Blocage de la propagation naturelle du feu (Minecraft vanilla) via les pare-feux
    // -------------------------------------------------------------------------

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() != Material.FIRE) return;
        Block target = event.getBlock();
        if (plugin.isFirebreakColumn(target.getX(), target.getZ())) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

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
