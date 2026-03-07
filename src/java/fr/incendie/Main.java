package fr.incendie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin {

    private List<FireZone> fireZones = new ArrayList<>();

    @Override
    public void onEnable() {
        // Code exécuté lors de l'activation du plugin
        getLogger().info("Plugin MFLRP activé avec succès !");

        // Enregistrement des commandes
        this.getCommand("startfire").setExecutor(new StartFireCommand(this));

        // Enregistrement des events
        getServer().getPluginManager().registerEvents(new FireListener(), this);

        // Scheduler pour la propagation du feu
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                Iterator<FireZone> iterator = fireZones.iterator();
                while (iterator.hasNext()) {
                    FireZone zone = iterator.next();
                    long elapsed = currentTime - zone.getStartTime();
                    // calculate a linear radius growth from 3 to maxSize over 30 minutes
                    int targetRadius = 3;
                    long duration = 30 * 60 * 1000; // 30 minutes in ms
                    if (elapsed >= duration) {
                        targetRadius = zone.getMaxSize();
                    } else {
                        double fraction = (double) elapsed / (double) duration;
                        targetRadius = 3 + (int) Math.round((zone.getMaxSize() - 3) * fraction);
                        if (targetRadius > zone.getMaxSize()) {
                            targetRadius = zone.getMaxSize();
                        }
                    }

                    if (targetRadius > zone.getRadius()) {
                        zone.expand(targetRadius);
                    }

                    // refresh fire every tick interval to keep it alive (without extinguishing)
                    zone.placeFire(zone.getRadius());
                }
            }
        }.runTaskTimer(this, 0L, 1200L); // toutes les 60 secondes
    }

    @Override
    public void onDisable() {
        // Code exécuté lors de la désactivation du plugin
        getLogger().info("Plugin MFLRP désactivé !");

        // Éteindre tous les feux
        for (FireZone zone : fireZones) {
            zone.extinguish();
        }
        fireZones.clear();
    }

    public void addFireZone(FireZone zone) {
        fireZones.add(zone);
    }

    public List<FireZone> getFireZones() {
        return fireZones;
    }
}
