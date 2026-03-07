package fr.incendie;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
                    if (elapsed > 20 * 60 * 1000) { // 20 minutes
                        if (zone.getRadius() < 30) {
                            zone.expand(30);
                        }
                    } else if (elapsed > 10 * 60 * 1000) { // 10 minutes
                        if (zone.getRadius() < 20) {
                            zone.expand(20);
                        }
                    }
                    // Replacer le feu toutes les minutes pour maintenir
                    zone.extinguish();
                    zone.placeFire();
                }
            }
        }.runTaskTimer(this, 0L, 1200L); // Toutes les 60 secondes (1200 ticks)
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
