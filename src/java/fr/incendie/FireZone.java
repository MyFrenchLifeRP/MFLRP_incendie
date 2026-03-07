package fr.incendie;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class FireZone {
    private String name;
    private Location center;
    private int minHeight;
    private int maxHeight;
    private int maxSize;
    private int radius;
    private long startTime;
    private Set<Location> fireBlocks = new HashSet<>();

    /**
     * Create a new fire zone.
     * @param name unique name of the zone
     * @param center center location (usually player position)
     * @param minHeight minimum vertical level where fire may appear
     * @param maxHeight maximum vertical level where fire may appear
     * @param maxSize final radius after propagation (blocks)
     */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.radius = 3; // always start at 3
        this.startTime = System.currentTimeMillis();
        placeFire(radius);
    }

    /**
     * Place or refresh the fire circle with the given radius.
     * This method respects the configured height range when searching for
     * a valid block to ignite.
     */
    public void placeFire(int radius) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius) {
                    // rechercher un bloc solide entre minHeight et maxHeight
                    for (int y = maxHeight; y >= minHeight; y--) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType().isSolid()) {
                            Block above = world.getBlockAt(x, y + 1, z);
                            // ne pas dépasser la hauteur max pour le feu lui-même
                            if (above.getType() == Material.AIR && above.getY() <= maxHeight) {
                                above.setType(Material.FIRE);
                                fireBlocks.add(above.getLocation());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public void extinguish() {
        for (Location loc : fireBlocks) {
            if (loc.getBlock().getType() == Material.FIRE) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        fireBlocks.clear();
    }

    public void expand(int newRadius) {
        if (newRadius <= radius) {
            return;
        }
        extinguish();
        this.radius = newRadius;
        placeFire(radius);
    }

    public long getStartTime() {
        return startTime;
    }

    public int getRadius() {
        return radius;
    }

    public Location getCenter() {
        return center;
    }

    public String getName() {
        return name;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    /**
     * Number of current fire blocks tracked in this zone.
     */
    public int getFireCount() {
        return fireBlocks.size();
    }
}