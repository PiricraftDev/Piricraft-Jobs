package fr.piricraft.piricraftJobs.listeners;

import fr.piricraft.piricraftJobs.PiricraftJobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JobsConnectionListener implements Listener {

    private final PiricraftJobs plugin;

    public JobsConnectionListener(PiricraftJobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            plugin.getDatabaseManager().loadProfileAsync(event.getUniqueId()).get();
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible de charger le profil de " + event.getName() + " lors de la connexion.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDatabaseManager().unloadProfile(event.getPlayer().getUniqueId());
    }
}