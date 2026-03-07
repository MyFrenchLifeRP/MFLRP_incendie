package fr.incendie;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class FireZone {
    private Location center;
    private int radius;
    private long startTime;
    private Set<Location> fireBlocks = new HashSet<>();

    public FireZone(Location center, int initialRadius) {
        this.center = center;
        this.radius = initialRadius;
        this.startTime = System.currentTimeMillis();
        placeFire();
    }

    public void placeFire() {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius) {
                    // Trouver le bloc le plus haut solide
                    for (int y = cy + 10; y >= cy - 10; y--) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType().isSolid()) {
                            Block above = world.getBlockAt(x, y + 1, z);
                            if (above.getType() == Material.AIR) {
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
        extinguish();
        this.radius = newRadius;
        placeFire();
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
}