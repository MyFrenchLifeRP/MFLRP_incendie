package fr.incendie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class FireZone {
    private static final Random RANDOM = new Random();

    private String name;
    private Location center;
    private int minHeight;
    private int maxHeight;
    private int maxSize;
    private int propagationSpeedSeconds;
    private int reIgnitionDelaySeconds;
    private long lastSpreadTime;
    private int currentRadius = 0;

    // Suivi des blocs de feu
    private Set<Location> fireBlocks = new HashSet<>();
    private Set<String> fireBlockKeys  = new HashSet<>(); // "x,y,z"
    private Set<String> fireXZKeys     = new HashSet<>(); // "x,z"  pour le comptage d'adjacence
    private Set<String> suppressedFireKeys = new HashSet<>(); // blocs bloques quand la zone est maitrisee
    private Set<String> frontierBlockKeys = new HashSet<>(); // cases candidates pour la propagation ("x,y,z")
    private long controlledUntil;

    /** Reference partagee avec Main vers les colonnes XZ de pare-feu actives. */
    private Set<String> blockedXZColumns = Collections.emptySet();

    public void setBlockedXZColumns(Set<String> cols) {
        this.blockedXZColumns = (cols != null) ? cols : Collections.<String>emptySet();
    }

    /** Cree une nouvelle zone d'incendie. */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int propagationSpeedSeconds, int reIgnitionDelaySeconds) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.propagationSpeedSeconds = Math.max(1, propagationSpeedSeconds);
        this.reIgnitionDelaySeconds = Math.max(1, reIgnitionDelaySeconds);
        this.lastSpreadTime = System.currentTimeMillis();
        placeInitialFire();
    }

    /** Cree une zone d'incendie a partir de donnees persistees (sans placement initial). */
    public FireZone(String name, Location center, int minHeight, int maxHeight, int maxSize, int propagationSpeedSeconds, int reIgnitionDelaySeconds, long lastSpreadTime) {
        this.name = name;
        this.center = center;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
        this.propagationSpeedSeconds = Math.max(1, propagationSpeedSeconds);
        this.reIgnitionDelaySeconds = Math.max(1, reIgnitionDelaySeconds);
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
        fireBlocks.add(loc);
        fireBlockKeys.add(toBlockKey(loc));
        fireXZKeys.add(loc.getBlockX() + "," + loc.getBlockZ());
        int dx = loc.getBlockX() - center.getBlockX();
        int dz = loc.getBlockZ() - center.getBlockZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        if (dist > currentRadius) currentRadius = dist;
    }

    private void addNeighborsToFrontier(Location fireLoc) {
        World world = fireLoc.getWorld();
        int fx = fireLoc.getBlockX();
        int fy = fireLoc.getBlockY();
        int fz = fireLoc.getBlockZ();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        // Voisins 3D: horizontal + differences de hauteur, pour permettre la propagation entre etages.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                int nx = fx + dx;
                int nz = fz + dz;
                double dist2 = (double) (nx - cx) * (nx - cx) + (double) (nz - cz) * (nz - cz);
                if (dist2 > (double) maxSize * maxSize) continue;

                String xzKey = nx + "," + nz;
                if (blockedXZColumns.contains(xzKey)) continue;

                int minY = Math.max(minHeight, fy - 1);
                int maxY = Math.min(maxHeight + 1, fy + 1);
                for (int ny = minY; ny <= maxY; ny++) {
                    if (!canHostFireAt(world, nx, ny, nz)) continue;

                    String key = nx + "," + ny + "," + nz;
                    if (suppressedFireKeys.contains(key)) continue;
                    if (fireBlockKeys.contains(key)) continue;

                    frontierBlockKeys.add(key);
                }
            }
        }
    }

    /**
     * Reconstruit la frontiere et fireXZKeys a partir des blocs de feu existants.
     * A appeler apres chargement du snapshot de persistance.
     */
    public void rebuildFrontier() {
        frontierBlockKeys.clear();
        fireXZKeys.clear();
        for (Location loc : fireBlocks) {
            fireXZKeys.add(loc.getBlockX() + "," + loc.getBlockZ());
        }
        for (Location loc : fireBlocks) {
            addNeighborsToFrontier(loc);
        }
    }

    /**
     * Propage le feu vers un bloc adjacent de facon ponderee et realiste.
     * Les cases voisines de plusieurs flammes ont plus de chances de s'enflammer.
     * Poids d'une case = 1 + 2 * (nb de feux adjacents).
     * Les colonnes XZ presentes dans blockedXZColumns (pare-feux) sont ignorees.
     * @param blockedXZColumns colonnes XZ bloquees par les pare-feux ("x,z")
     * @return true si un nouveau feu a ete place
     */
    public boolean spreadOneFire() {
        if (frontierBlockKeys.isEmpty()) return false;

        // Construire la liste ponderee des cases candidates
        List<String> weightedPool = new ArrayList<>();
        for (String blockKey : frontierBlockKeys) {
            String[] p = blockKey.split(",");
            int nx = Integer.parseInt(p[0]);
            int ny = Integer.parseInt(p[1]);
            int nz = Integer.parseInt(p[2]);

            if (blockedXZColumns.contains(nx + "," + nz)) continue; // securite supplementaire
            int adj = countAdjacentFire3D(nx, ny, nz);
            int weight = 1 + adj * 2;
            for (int w = 0; w < weight; w++) weightedPool.add(blockKey);
        }
        Collections.shuffle(weightedPool, RANDOM);

        Set<String> tried = new HashSet<>();
        for (String blockKey : weightedPool) {
            if (!tried.add(blockKey)) continue; // deja essaye cette case
            frontierBlockKeys.remove(blockKey);

            String[] parts = blockKey.split(",");
            int nx = Integer.parseInt(parts[0]);
            int ny = Integer.parseInt(parts[1]);
            int nz = Integer.parseInt(parts[2]);

            World world = center.getWorld();
            if (!canHostFireAt(world, nx, ny, nz)) continue;

            Location candidate = world.getBlockAt(nx, ny, nz).getLocation();
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

    /** Nombre de feux adjacents en 3D (utilise pour le poids de propagation). */
    private int countAdjacentFire3D(int x, int y, int z) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (fireBlockKeys.contains((x + dx) + "," + (y + dy) + "," + (z + dz))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * En 1.12, le feu peut tenir au-dessus d'un bloc solide ou au contact d'un bloc inflammable.
     * Cette regle permet une propagation naturelle sur plusieurs etages (murs/toits/arbres) dans la zone.
     */
    private boolean canHostFireAt(World world, int x, int y, int z) {
        if (y < minHeight || y > maxHeight + 1) return false;

        Block target = world.getBlockAt(x, y, z);
        if (target.getType() != Material.AIR) return false;

        if (world.getBlockAt(x, y - 1, z).getType().isSolid()) {
            return true;
        }

        return world.getBlockAt(x + 1, y, z).getType().isFlammable()
                || world.getBlockAt(x - 1, y, z).getType().isFlammable()
                || world.getBlockAt(x, y, z + 1).getType().isFlammable()
                || world.getBlockAt(x, y, z - 1).getType().isFlammable()
                || world.getBlockAt(x, y + 1, z).getType().isFlammable();
    }

    /**
     * Rafraichit les flammes et detecte si toute la zone a ete eteinte (eau, commandes...).
     * Si toutes les flammes suivies sont eteintes, la zone passe en mode "maitrise".
     * @return true si au moins une flamme brule encore, false si la zone vient d'etre maitrisee
     */
    public boolean refreshFire(long nowMs) {
        if (fireBlocks.isEmpty()) return false;

        boolean anyBurning = false;
        List<Location> naturallyOut = new ArrayList<>();

        for (Location loc : fireBlocks) {
            if (loc.getBlock().getType() == Material.FIRE) {
                anyBurning = true;
            } else {
                naturallyOut.add(loc);
            }
        }

        if (!anyBurning) {
            // Tous les feux sont eteints (eau, commande externe...)
            fireBlocks.clear();
            fireBlockKeys.clear();
            fireXZKeys.clear();
            frontierBlockKeys.clear();
            currentRadius = 0;
            controlledUntil = Math.max(controlledUntil, nowMs + (long) reIgnitionDelaySeconds * 1000L);
            return false;
        }

        // Re-allumer les blocs eteints naturellement (pluie legere, etc.)
        for (Location loc : naturallyOut) {
            loc.getBlock().setType(Material.FIRE);
        }
        return true;
    }

    public void extinguish() {
        for (Location loc : fireBlocks) {
            if (loc.getBlock().getType() == Material.FIRE) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        fireBlocks.clear();
        fireBlockKeys.clear();
        fireXZKeys.clear();
        frontierBlockKeys.clear();
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
     * - Si d'autres flammes brulent encore : la case est remise en frontiere (peut se re-enflammer).
     * - Si c'etait la derniere flamme : la zone passe en mode "maitrise".
     * @return true si la zone est desormais entierement maitrisee
     */
    public boolean registerManualExtinguish(Location location, long nowMs) {
        if (location == null) return false;

        Location blockLoc = location.getBlock().getLocation();
        String key = toBlockKey(blockLoc);
        String xzKey = blockLoc.getBlockX() + "," + blockLoc.getBlockZ();

        fireBlocks.remove(blockLoc);
        fireBlockKeys.remove(key);
        fireXZKeys.remove(xzKey);

        if (fireBlocks.isEmpty()) {
            // Zone entierement maitrisee
            suppressedFireKeys.add(key);
            frontierBlockKeys.clear();
            controlledUntil = Math.max(controlledUntil, nowMs + (long) reIgnitionDelaySeconds * 1000L);
            return true;
        }

        // Il reste d'autres flammes : cette case peut se re-enflammer depuis ses voisins
        double dx2 = blockLoc.getBlockX() - center.getBlockX();
        double dz2 = blockLoc.getBlockZ() - center.getBlockZ();
        if (dx2 * dx2 + dz2 * dz2 <= (double) maxSize * maxSize) {
            frontierBlockKeys.add(key);
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
        fireBlocks.clear();
        fireBlockKeys.clear();
        fireXZKeys.clear();
        frontierBlockKeys.clear();
        currentRadius = 0;
        placeInitialFire();
    }

    public int getReIgnitionDelaySeconds() { return reIgnitionDelaySeconds; }

    public long getControlledUntil() { return controlledUntil; }

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
    public boolean isFullySpread() { return frontierBlockKeys.isEmpty(); }

    /** Verifie si une position est dans cette zone sur le plan horizontal. */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || center.getWorld() == null) return false;
        if (!center.getWorld().equals(location.getWorld())) return false;
        double dx = location.getX() - center.getX();
        double dz = location.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= ((double) maxSize * maxSize);
    }

    /**
     * Verifie si une position est dans la zone horizontale ET dans la plage de hauteur [minHeight, maxHeight+1].
     * Le +1 couvre les flammes posees au-dessus du bloc solide le plus haut.
     */
    public boolean containsWithHeight(Location location) {
        if (!contains(location)) return false;
        int y = location.getBlockY();
        return y >= minHeight && y <= maxHeight + 1;
    }
}