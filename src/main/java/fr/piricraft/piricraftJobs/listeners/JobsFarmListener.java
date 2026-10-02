package fr.piricraft.piricraftJobs.listeners;

import fr.piricraft.piricraftJobs.PiricraftJobs;
import fr.piricraft.piricraftJobs.managers.JobsManager;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class JobsFarmListener implements Listener {

    private final PiricraftJobs plugin;
    private final JobsManager jobsManager;
    private static final String PLACED_KEY = "jobs_placed_by_player";

    public JobsFarmListener(PiricraftJobs plugin, JobsManager jobsManager) {
        this.plugin = plugin;
        this.jobsManager = jobsManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        block.setMetadata(PLACED_KEY, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        boolean placedByPlayer = block.hasMetadata(PLACED_KEY);
        if (placedByPlayer) {
            block.removeMetadata(PLACED_KEY, plugin);
        }

        BlockData blockData = block.getBlockData();
        if (blockData instanceof Ageable ageable
                && ageable.getAge() != ageable.getMaximumAge()) {
            return;
        }

        jobsManager.processBlockBreak(player, block.getType(), placedByPlayer);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            jobsManager.processMobKill(killer, event.getEntityType());
        }
    }
}
