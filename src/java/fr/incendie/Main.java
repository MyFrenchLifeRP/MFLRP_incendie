package fr.incendie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {

    private List<FireZone> fireZones = new ArrayList<>();

    @Override
    public void onEnable() {
        // Code exécuté lors de l'activation du plugin
        getLogger().info("Plugin MFLRP activé avec succès !");

        saveDefaultConfig();
        loadPersistentState();

        // Enregistrement des commandes
        this.getCommand("startfire").setExecutor(new StartFireCommand(this));
        this.getCommand("listfires").setExecutor(new ListFireCommand(this));
        this.getCommand("removefire").setExecutor(new RemoveFireCommand(this));
        this.getCommand("extinguishitem").setExecutor(new ExtinguishItemCommand(this));
        this.getCommand("extinguishstatus").setExecutor(new ExtinguishStatusCommand(this));

        // Listener d'interaction pour l'outil d'extinction
        getServer().getPluginManager().registerEvents(new FireInteractListener(this), this);

        // Enregistrement des events
        getServer().getPluginManager().registerEvents(new FireListener(), this);

        // Scheduler pour la propagation et le maintien du feu (1 seconde = 20 ticks)
        new BukkitRunnable() {
            private int tickCount = 0;

            @Override
            public void run() {
                tickCount++;
                long currentTime = System.currentTimeMillis();
                for (FireZone zone : fireZones) {
                    if (zone.canResumeAfterControlled(currentTime)) {
                        zone.resumeAfterControlled();
                    }
                    if (zone.isControlled(currentTime)) {
                        continue;
                    }

                    // Propager une flamme si le delai est ecoule
                    long msSinceLastSpread = currentTime - zone.getLastSpreadTime();
                    if (!zone.isFullySpread() && msSinceLastSpread >= zone.getPropagationSpeedSeconds() * 1000L) {
                        zone.spreadOneFire();
                        zone.setLastSpreadTime(currentTime);
                    }

                    // Rafraichir le feu toutes les 60 secondes pour le maintenir actif
                    if (tickCount % 60 == 0) {
                        zone.refreshFire();
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L); // toutes les secondes (20 ticks)
    }

    @Override
    public void onDisable() {
        // Code exécuté lors de la désactivation du plugin
        getLogger().info("Plugin MFLRP désactivé !");

        savePersistentState();
    }

    public void addFireZone(FireZone zone) {
        fireZones.add(zone);
        savePersistentState();
    }

    public List<FireZone> getFireZones() {
        return fireZones;
    }

    /**
        * Supprime une zone d'incendie par nom et l'eteint. Retourne true si supprimee.
     */
    public boolean removeFireZone(String name) {
        Iterator<FireZone> it = fireZones.iterator();
        while (it.hasNext()) {
            FireZone z = it.next();
            if (z.getName().equalsIgnoreCase(name)) {
                z.extinguish();
                it.remove();
                savePersistentState();
                return true;
            }
        }
        return false;
    }

    // Material utilise comme outil d'extinction
    private org.bukkit.Material extinguisherMaterial;
    private int extinguishDelaySeconds = 2;

    public void setExtinguisherMaterial(org.bukkit.Material mat) {
        this.extinguisherMaterial = mat;
        savePersistentState();
    }

    public org.bukkit.Material getExtinguisherMaterial() {
        return extinguisherMaterial;
    }

    public int getExtinguishDelaySeconds() {
        return extinguishDelaySeconds;
    }

    public void setExtinguishDelaySeconds(int extinguishDelaySeconds) {
        this.extinguishDelaySeconds = Math.max(0, extinguishDelaySeconds);
        savePersistentState();
    }

    public void saveStateNow() {
        savePersistentState();
    }

    private void loadPersistentState() {
        fireZones.clear();

        String materialName = getConfig().getString("extinguisher.material");
        if (materialName != null && !materialName.isEmpty()) {
            Material loadedMaterial = Material.getMaterial(materialName.toUpperCase());
            if (loadedMaterial != null && loadedMaterial != Material.AIR) {
                this.extinguisherMaterial = loadedMaterial;
            }
        }

        this.extinguishDelaySeconds = Math.max(0, getConfig().getInt("extinguisher.delaySeconds", 2));

        ConfigurationSection zones = getConfig().getConfigurationSection("fireZones");
        if (zones == null) {
            return;
        }

        int loadedCount = 0;
        for (String key : zones.getKeys(false)) {
            String base = "fireZones." + key;
            String name = getConfig().getString(base + ".name");
            String worldName = getConfig().getString(base + ".world");
            if (name == null || worldName == null) {
                continue;
            }

            World world = getServer().getWorld(worldName);
            if (world == null) {
                getLogger().warning("Zone '" + name + "' ignorée: monde introuvable ('" + worldName + "').");
                continue;
            }

            double x = getConfig().getDouble(base + ".x");
            double y = getConfig().getDouble(base + ".y");
            double z = getConfig().getDouble(base + ".z");
            int minHeight = getConfig().getInt(base + ".minHeight");
            int maxHeight = getConfig().getInt(base + ".maxHeight");
            int maxSize = getConfig().getInt(base + ".maxSize");
            int propagationSpeedSeconds = getConfig().getInt(base + ".propagationSpeedSeconds", 30);
            long lastSpreadTime = getConfig().getLong(base + ".lastSpreadTime", System.currentTimeMillis());
            List<String> litFireKeys = getConfig().getStringList(base + ".litFireBlocks");
            List<String> suppressedKeys = getConfig().getStringList(base + ".suppressedFireBlocks");
            long controlledUntil = getConfig().getLong(base + ".controlledUntil", 0L);
            boolean hasSnapshot = litFireKeys != null && !litFireKeys.isEmpty();

            Location center = new Location(world, x, y, z);
            FireZone zone = new FireZone(name, center, minHeight, maxHeight, maxSize, propagationSpeedSeconds, lastSpreadTime);

            if (hasSnapshot) {
                for (String keyLoc : litFireKeys) {
                    String[] parts = keyLoc.split(",");
                    if (parts.length != 3) {
                        continue;
                    }
                    try {
                        int bx = Integer.parseInt(parts[0]);
                        int by = Integer.parseInt(parts[1]);
                        int bz = Integer.parseInt(parts[2]);
                        Location fireLoc = new Location(world, bx, by, bz);
                        zone.trackExistingLitFire(fireLoc);

                        if (fireLoc.getBlock().getType() == Material.AIR) {
                            fireLoc.getBlock().setType(Material.FIRE);
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore malformed snapshot entry
                    }
                }
                // Reconstruire la frontiere de propagation a partir du snapshot
                zone.rebuildFrontier();
            } else {
                // Pas de snapshot : placer le feu initial au centre
                zone.rebuildFrontier();
            }

            if (suppressedKeys != null) {
                for (String suppressed : suppressedKeys) {
                    zone.addSuppressedKey(suppressed);
                }
            }
            zone.setControlledUntil(controlledUntil);

            fireZones.add(zone);
            loadedCount++;
        }

        if (loadedCount > 0) {
            getLogger().info(loadedCount + " zone(s) d'incendie restauree(s) depuis la configuration.");
        }
    }

    private void savePersistentState() {
        if (extinguisherMaterial != null) {
            getConfig().set("extinguisher.material", extinguisherMaterial.name());
        } else {
            getConfig().set("extinguisher.material", null);
        }
        getConfig().set("extinguisher.delaySeconds", extinguishDelaySeconds);

        getConfig().set("fireZones", null);
        int index = 0;
        for (FireZone zone : fireZones) {
            if (zone.getCenter() == null || zone.getCenter().getWorld() == null) {
                continue;
            }

            String base = "fireZones." + index;
            getConfig().set(base + ".name", zone.getName());
            getConfig().set(base + ".world", zone.getCenter().getWorld().getName());
            getConfig().set(base + ".x", zone.getCenter().getX());
            getConfig().set(base + ".y", zone.getCenter().getY());
            getConfig().set(base + ".z", zone.getCenter().getZ());
            getConfig().set(base + ".minHeight", zone.getMinHeight());
            getConfig().set(base + ".maxHeight", zone.getMaxHeight());
            getConfig().set(base + ".maxSize", zone.getMaxSize());
            getConfig().set(base + ".propagationSpeedSeconds", zone.getPropagationSpeedSeconds());
            getConfig().set(base + ".lastSpreadTime", zone.getLastSpreadTime());
            getConfig().set(base + ".litFireBlocks", zone.getLitFireBlockKeys());
            getConfig().set(base + ".suppressedFireBlocks", zone.getSuppressedFireKeys());
            getConfig().set(base + ".controlledUntil", zone.getControlledUntil());
            index++;
        }

        saveConfig();
    }
}
