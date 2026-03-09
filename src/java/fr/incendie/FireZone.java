package fr.incendie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class FireZone {
    private static final long CONTROLLED_COOLDOWN_MS = 30L * 60L * 1000L;
    private static final Random RANDOM = new Random();
    // 8 directions horizontales pour la propagation
    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DZ = {0, 0, 1, -1, 1, -1, 1, -1};

    private String name;
    private Location center;
    private int minHeight;
    private int maxHeight;
    private int maxSize;
    private int propagationSpeedSeconds;
    private long lastSpreadTime;
    private int currentRadius = 0;

    private Set<Location> fireBlocks = new HashSet<>();
    private Set<String> fireBlockKeys = new HashSet<>();
    private Set<String> suppressedFireKeys = new HashSet<>();
    private Set<String> frontierXZKeys = new HashSet<>();
    private long controlledUntil;

    /** Cree une nouvelle zone d'incendie. */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int propagationSpeedSeconds) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.propagationSpeedSeconds = Math.max(1, propagationSpeedSeconds);
        this.lastSpreadTime = System.currentTimeMillis();
        placeInitialFire();
    }

    /** Cree une zone d'incendie a partir de donnees persistees (sans placement initial). */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int propagationSpeedSeconds, long lastSpreadTime) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.propagationSpeedSeconds = Math.max(1, propagationSpeedSeconds);
        this.lastSpreadTime = lastSpreadTime;
    }

    private void placeInitialFire() {
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        Location fireLoc = findValidFireLocation(center.getWorld(), cx, cz);
        if (fireLoc != null) {
            fireLoc.getBlock().setType(Material.FIRE);
            trackFireBlock(fireLoc);
            addNeighborsToFrontier(fireLoc);
        }
    }

    private Location findValidFireLocation(World world, int x, int z) {
        for (int y = maxHeight; y >= minHeight; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid()) {
                Block above = world.getBlockAt(x, y + 1, z);
                String key = toBlockKey(above.getLocation());
                if (!suppressedFireKeys.contains(key) && above.getType() == Material.AIR && above.getY() <= maxHeight) {
                    return above.getLocation();
                }
                break;
            }
        }
        return null;
    }

    private void trackFireBlock(Location loc) {
        String key = toBlockKey(loc);
        fireBlocks.add(loc);
        fireBlockKeys.add(key);
        int dx = loc.getBlockX() - center.getBlockX();
        int dz = loc.getBlockZ() - center.getBlockZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        if (dist > currentRadius) currentRadius = dist;
    }

    private void addNeighborsToFrontier(Location fireLoc) {
        World world = fireLoc.getWorld();
        int fx = fireLoc.getBlockX();
        int fz = fireLoc.getBlockZ();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        for (int i = 0; i < DX.length; i++) {
            int nx = fx + DX[i];
            int nz = fz + DZ[i];
            double dist2 = (double)(nx - cx) * (nx - cx) + (double)(nz - cz) * (nz - cz);
            if (dist2 > (double) maxSize * maxSize) continue;

            String xzKey = nx + "," + nz;
            if (frontierXZKeys.contains(xzKey)) continue;

            Location candidate = findValidFireLocation(world, nx, nz);
            if (candidate == null) continue;
            if (fireBlockKeys.contains(toBlockKey(candidate))) continue;

            frontierXZKeys.add(xzKey);
        }
    }

    /**
     * Reconstruit la frontiere a partir des blocs de feu existants.
     * A appeler apres chargement du snapshot de persistance.
     */
    public void rebuildFrontier() {
        frontierXZKeys.clear();
        for (Location loc : fireBlocks) {
            addNeighborsToFrontier(loc);
        }
    }

    /**
     * Propage le feu vers un bloc aleatoire adjacent.
     * @return true si un nouveau feu a ete place
     */
    public boolean spreadOneFire() {
        if (frontierXZKeys.isEmpty()) return false;

        List<String> frontierList = new ArrayList<>(frontierXZKeys);
        while (!frontierList.isEmpty()) {
            int idx = RANDOM.nextInt(frontierList.size());
            String xzKey = frontierList.remove(idx);
            frontierXZKeys.remove(xzKey);

            String[] parts = xzKey.split(",");
            int nx = Integer.parseInt(parts[0]);
            int nz = Integer.parseInt(parts[1]);

            Location candidate = findValidFireLocation(center.getWorld(), nx, nz);
            if (candidate == null) continue;
            String locKey = toBlockKey(candidate);
            if (fireBlockKeys.contains(locKey)) continue;
            if (suppressedFireKeys.contains(locKey)) continue;

            candidate.getBlock().setType(Material.FIRE);
            trackFireBlock(candidate);
            addNeighborsToFrontier(candidate);
            return true;
        }
        return false;
    }

    /** Re-allume les blocs de feu suivis qui se sont eteints naturellement. */
    public void refreshFire() {
        for (Location loc : fireBlocks) {
            if (loc.getBlock().getType() == Material.AIR) {
                loc.getBlock().setType(Material.FIRE);
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
        fireBlockKeys.clear();
        frontierXZKeys.clear();
        currentRadius = 0;
    }

    public void trackExistingLitFire(Location location) {
        if (location == null) return;
        trackFireBlock(location.getBlock().getLocation());
    }

    public void untrackFire(Location location) {
        if (location == null) return;
        Location blockLoc = location.getBlock().getLocation();
        fireBlocks.remove(blockLoc);
        fireBlockKeys.remove(toBlockKey(blockLoc));
    }

    /**
     * Marque une flamme comme eteinte manuellement.
     * @return true quand toute la zone devient maitrisee (toutes les flammes eteintes)
     */
    public boolean registerManualExtinguish(Location location, long nowMs) {
        if (location == null) return false;

        Location blockLoc = location.getBlock().getLocation();
        String key = toBlockKey(blockLoc);
        suppressedFireKeys.add(key);
        fireBlocks.remove(blockLoc);
        fireBlockKeys.remove(key);

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
        frontierXZKeys.clear();
        currentRadius = 0;
        placeInitialFire();
    }

    public long getControlledUntil() {
        return controlledUntil;
    }

    public void setControlledUntil(long controlledUntil) {
        this.controlledUntil = Math.max(0L, controlledUntil);
    }

    public void addSuppressedKey(String key) {
        if (key == null || key.isEmpty()) return;
        suppressedFireKeys.add(key);
    }

    public List<String> getSuppressedFireKeys() {
        return new ArrayList<>(suppressedFireKeys);
    }

    /** Snapshot des flammes actuellement allumees dans le monde uniquement. */
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

    // --- Getters ---

    public String getName() { return name; }
    public Location getCenter() { return center; }
    public int getMinHeight() { return minHeight; }
    public int getMaxHeight() { return maxHeight; }
    public int getMaxSize() { return maxSize; }
    public int getRadius() { return currentRadius; }
    public int getPropagationSpeedSeconds() { return propagationSpeedSeconds; }
    public long getLastSpreadTime() { return lastSpreadTime; }
    public void setLastSpreadTime(long t) { this.lastSpreadTime = t; }
    public int getFireCount() { return fireBlocks.size(); }

    /** Retourne true si toute la zone a ete couverte (frontiere vide). */
    public boolean isFullySpread() { return frontierXZKeys.isEmpty(); }

    /** Verifie si une position est dans cette zone sur le plan horizontal. */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || center.getWorld() == null) return false;
        if (!center.getWorld().equals(location.getWorld())) return false;
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= ((double) maxSize * maxSize);
    }
}