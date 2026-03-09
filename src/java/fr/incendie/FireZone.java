package fr.incendie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class FireZone {
    private static final long CONTROLLED_COOLDOWN_MS = 30L * 60L * 1000L;

    private String name;
    private Location center;
    private int minHeight;
    private int maxHeight;
    private int maxSize;
    private int radius;
    private long startTime;
    private Set<Location> fireBlocks = new HashSet<>();
    private Set<String> suppressedFireKeys = new HashSet<>();
    private long controlledUntil;

    /**
        * Cree une nouvelle zone d'incendie.
        * @param name nom unique de la zone
        * @param center position centrale (souvent la position du joueur)
        * @param minHeight niveau vertical minimal ou le feu peut apparaitre
        * @param maxHeight niveau vertical maximal ou le feu peut apparaitre
        * @param maxSize rayon final apres propagation (blocs)
     */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize) {
        this(name, center, minHeight, maxHeight, maxSize, 3, System.currentTimeMillis(), true);
    }

    /**
        * Cree une zone d'incendie a partir de donnees persistees.
     */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int radius, long startTime) {
        this(name, center, minHeight, maxHeight, maxSize, radius, startTime, true);
    }

    /**
        * Cree une zone d'incendie a partir de donnees persistees.
     */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int radius, long startTime,
            boolean placeFireNow) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.radius = Math.max(1, radius);
        this.startTime = startTime;
        if (placeFireNow) {
            placeFire(this.radius);
        }
    }

    /**
        * Place ou rafraichit le cercle de feu avec le rayon donne.
        * Cette methode respecte la plage de hauteurs configuree lors de la
        * recherche d'un bloc valide a enflammer.
     */
    public void placeFire(int radius) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if ((x - cx) * (x - cx) + (z - cz) * (z - cz) <= radius * radius) {
                    // rechercher un bloc solide entre minHeight et maxHeight
                    for (int y = maxHeight; y >= minHeight; y--) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType().isSolid()) {
                            Block above = world.getBlockAt(x, y + 1, z);
                            String key = toBlockKey(above.getLocation());
                            if (suppressedFireKeys.contains(key)) {
                                break;
                            }
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

    public void trackExistingLitFire(Location location) {
        if (location == null) {
            return;
        }
        fireBlocks.add(location.getBlock().getLocation());
    }

    public void untrackFire(Location location) {
        if (location == null) {
            return;
        }
        fireBlocks.remove(location.getBlock().getLocation());
    }

    /**
        * Marque une flamme comme eteinte manuellement.
        * @return true quand toute la zone devient maitrisee (toutes les flammes eteintes)
     */
    public boolean registerManualExtinguish(Location location, long nowMs) {
        if (location == null) {
            return false;
        }

        Location blockLoc = location.getBlock().getLocation();
        suppressedFireKeys.add(toBlockKey(blockLoc));
        fireBlocks.remove(blockLoc);

        if (fireBlocks.isEmpty()) {
            controlledUntil = Math.max(controlledUntil, nowMs + CONTROLLED_COOLDOWN_MS);
            return true;
        }
        return false;
    }

    public boolean isControlled(long nowMs) {
        return controlledUntil > nowMs;
    }

    public boolean canResumeAfterControlled(long nowMs) {
        return controlledUntil > 0 && nowMs >= controlledUntil;
    }

    public void resumeAfterControlled() {
        controlledUntil = 0L;
        suppressedFireKeys.clear();
    }

    public long getControlledUntil() {
        return controlledUntil;
    }

    public void setControlledUntil(long controlledUntil) {
        this.controlledUntil = Math.max(0L, controlledUntil);
    }

    public void addSuppressedKey(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        suppressedFireKeys.add(key);
    }

    public List<String> getSuppressedFireKeys() {
        return new ArrayList<>(suppressedFireKeys);
    }

    /**
        * Snapshot des flammes actuellement allumees dans le monde uniquement.
     */
    public List<String> getLitFireBlockKeys() {
        List<String> result = new ArrayList<>();
        for (Location loc : fireBlocks) {
            if (loc.getBlock().getType() == Material.FIRE) {
                result.add(loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
        return result;
    }

    private String toBlockKey(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
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

    /**
        * Verifie si une position est dans cette zone sur le plan horizontal.
     */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || center.getWorld() == null) {
            return false;
        }
        if (!center.getWorld().equals(location.getWorld())) {
            return false;
        }
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
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
        * Nombre de blocs de feu actuellement suivis dans cette zone.
     */
    public int getFireCount() {
        return fireBlocks.size();
    }
}