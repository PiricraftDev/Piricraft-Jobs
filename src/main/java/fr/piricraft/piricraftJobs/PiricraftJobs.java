package fr.piricraft.piricraftJobs;

import org.bukkit.plugin.java.JavaPlugin;

public final class PiricraftJobs extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Piricraft-Jobs has started !");
    }

    @Override
    public void onDisable() {
        getLogger().info("Piricraft-Jobs has stopped !");
    }
}
