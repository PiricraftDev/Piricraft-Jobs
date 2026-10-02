package fr.piricraft.piricraftJobs.managers;

import fr.piricraft.piricraftCore.api.PiricraftCoreAPI;
import fr.piricraft.piricraftCore.utils.TextUtils;
import fr.piricraft.piricraftJobs.PiricraftJobs;
import fr.piricraft.piricraftJobs.models.JobProfile;
import fr.piricraft.piricraftJobs.models.JobReward;
import fr.piricraft.piricraftJobs.models.JobType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class JobsManager {

    private final PiricraftJobs plugin;
    private final JobsDatabaseManager dbManager;
    private final Map<JobType, Map<Material, JobReward>> blockRewards = new EnumMap<>(JobType.class);
    private final Map<JobType, Map<EntityType, JobReward>> mobRewards = new EnumMap<>(JobType.class);

    public JobsManager(PiricraftJobs plugin, JobsDatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        loadRewardsFromConfig();
    }

    public void loadRewardsFromConfig() {
        blockRewards.clear();
        mobRewards.clear();

        for (JobType job : JobType.values()) {
            blockRewards.put(job, new HashMap<>());
            mobRewards.put(job, new HashMap<>());
        }

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");

        if (jobsSection == null) return;

        for (String jobKey : jobsSection.getKeys(false)) {
            JobType jobType;
            try {
                jobType = JobType.valueOf(jobKey.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Métier inconnu dans la configuration : " + jobKey);
                continue;
            }

            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobKey);
            if (jobSection == null) continue;

            ConfigurationSection blocksSection = jobSection.getConfigurationSection("blocks");
            if (blocksSection != null) {
                for (String matKey : blocksSection.getKeys(false)) {
                    Material mat = Material.matchMaterial(matKey);
                    if (mat != null) {
                        double money = blocksSection.getDouble(matKey + ".money", 0.0);
                        double xp = blocksSection.getDouble(matKey + ".xp", 0.0);
                        blockRewards.get(jobType).put(mat, new JobReward(money, xp));
                    } else {
                        plugin.getLogger().warning("Matériau inconnu dans la configuration métiers : " + matKey);
                    }
                }
            }

            ConfigurationSection mobsSection = jobSection.getConfigurationSection("mobs");
            if (mobsSection != null) {
                for (String mobKey : mobsSection.getKeys(false)) {
                    try {
                        EntityType entity = EntityType.valueOf(mobKey.toUpperCase());
                        double money = mobsSection.getDouble(mobKey + ".money", 0.0);
                        double xp = mobsSection.getDouble(mobKey + ".xp", 0.0);
                        mobRewards.get(jobType).put(entity, new JobReward(money, xp));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Entité inconnue dans la configuration métiers : " + mobKey);
                    }
                }
            }
        }
    }

    public double getRequiredExperience(int level) {
        return 100.0 * Math.pow(level, 1.5);
    }

    public void processBlockBreak(Player player, Material material) {
        JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
        if (profile == null) return;

        JobType activeJob = profile.getActiveJob();
        if (activeJob == null) return;

        Map<Material, JobReward> rewards = blockRewards.get(activeJob);
        if (rewards == null || !rewards.containsKey(material)) return;

        giveReward(player, profile, activeJob, rewards.get(material));
    }

    public void processMobKill(Player player, EntityType entityType) {
        JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
        if (profile == null) return;

        JobType activeJob = profile.getActiveJob();
        if (activeJob == null) return;

        Map<EntityType, JobReward> rewards = mobRewards.get(activeJob);
        if (rewards == null || !rewards.containsKey(entityType)) return;

        giveReward(player, profile, activeJob, rewards.get(entityType));
    }

    private void giveReward(Player player, JobProfile profile, JobType job, JobReward reward) {
        PiricraftCoreAPI.getEconomy().depositMoney(player.getUniqueId(), reward.getMoney());

        boolean levelUp = profile.addExperience(job, reward.getExperience(), this::getRequiredExperience);

        if (levelUp) {
            int newLevel = profile.getLevel(job);
            player.sendMessage(TextUtils.color(
                    "<bold><green>[Métier]</green></bold> Félicitations ! Tu es passé niveau <yellow>"
                            + newLevel + "</yellow> dans le métier <gold>" + job.getDisplayName() + "</gold> !"
            ));
        }
    }
}