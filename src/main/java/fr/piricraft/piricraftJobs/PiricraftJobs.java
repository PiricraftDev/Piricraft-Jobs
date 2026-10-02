package fr.piricraft.piricraftJobs;

import fr.piricraft.piricraftJobs.listeners.JobsConnectionListener;
import fr.piricraft.piricraftJobs.listeners.JobsFarmListener;
import fr.piricraft.piricraftJobs.managers.JobsDatabaseManager;
import fr.piricraft.piricraftJobs.managers.JobsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PiricraftJobs extends JavaPlugin {

    private JobsDatabaseManager databaseManager;
    private JobsManager jobsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new JobsDatabaseManager(this);
        if (!this.databaseManager.initDatabase()) {
            getLogger().severe("Désactivation du plugin en raison d'une erreur d'initialisation de la BDD.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.jobsManager = new JobsManager(this, databaseManager);

        getServer().getPluginManager().registerEvents(new JobsFarmListener(this, jobsManager), this);
        getServer().getPluginManager().registerEvents(new JobsConnectionListener(this), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.databaseManager.loadProfileAsync(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeDatabase();
        }
    }

    public JobsDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public JobsManager getJobsManager() {
        return jobsManager;
    }
}