package fr.piricraft.piricraftJobs.managers;

import fr.piricraft.piricraftCore.PiricraftCore;
import fr.piricraft.piricraftCore.utils.TextUtils;
import fr.piricraft.piricraftJobs.PiricraftJobs;
import fr.piricraft.piricraftJobs.models.JobProfile;
import fr.piricraft.piricraftJobs.models.JobReward;
import fr.piricraft.piricraftJobs.models.JobType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobsManager {

    private final PiricraftJobs plugin;
    private final JobsDatabaseManager dbManager;
    private final Map<JobType, Map<Material, JobReward>> blockRewards = new EnumMap<>(JobType.class);
    private final Map<JobType, Map<EntityType, JobReward>> mobRewards = new EnumMap<>(JobType.class);

    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> hideTasks = new ConcurrentHashMap<>();

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

    public void processBlockBreak(Player player, Material material, boolean placedByPlayer) {
        JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
        if (profile == null) return;

        JobType activeJob = profile.getActiveJob();
        if (activeJob == null) return;
        if (placedByPlayer && activeJob != JobType.FARMER) return;

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
        PiricraftCore corePlugin = plugin.getServer().getPluginManager().getPlugin("PiricraftCore")
                instanceof PiricraftCore core ? core : null;
        if (corePlugin == null) {
            plugin.getLogger().severe("PiricraftCore est indisponible : impossible de créditer la récompense d'argent.");
        } else {
            corePlugin.getEconomyManager().depositMoney(player.getUniqueId(), reward.getMoney());
        }

        boolean levelUp = profile.addExperience(job, reward.getExperience(), this::getRequiredExperience);
        dbManager.saveProfileAsync(profile);

        player.sendActionBar(TextUtils.color(
                "<gray>+<green>" + String.format("%.1f", reward.getExperience()) + " XP</green> | +<yellow>"
                        + String.format("%.2f", reward.getMoney()) + " $</yellow> (" + job.getDisplayName() + ")</gray>"
        ));

        if (profile.isShowBossBar()) {
            showTemporaryBossBar(player, profile, job, 3);
        }

        if (levelUp) {
            int newLevel = profile.getLevel(job);
            player.sendMessage(TextUtils.color(
                    "<bold><green>[Métier]</green></bold> Félicitations ! Tu es passé niveau <yellow>"
                            + newLevel + "</yellow> dans le métier <gold>" + job.getDisplayName() + "</gold> !"
            ));

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            spawnHarmlessFirework(player.getLocation().add(0, 1, 0));
        }
    }

    public void showTemporaryBossBar(Player player, JobProfile profile, JobType job, int displaySeconds) {
        UUID uuid = player.getUniqueId();

        if (!profile.isShowBossBar() || job == null) {
            removeBossBar(player);
            return;
        }

        int level = profile.getLevel(job);
        double currentXp = profile.getExperience(job);
        double reqXp = getRequiredExperience(level);

        float progress = (float) Math.min(1.0, Math.max(0.0, currentXp / reqXp));
        Component title = TextUtils.color("<gold>" + job.getDisplayName() + "</gold> <gray>- Niv. <yellow>" + level + "</yellow> (" + (int) currentXp + "/" + (int) reqXp + " XP)</gray>");

        BossBar bossBar = activeBossBars.computeIfAbsent(uuid, k -> {
            BossBar bar = BossBar.bossBar(
                    title,
                    progress,
                    BossBar.Color.GREEN,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bar);
            return bar;
        });

        bossBar.name(title);
        bossBar.progress(progress);

        BukkitTask existingTask = hideTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask newTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> removeBossBar(player), displaySeconds * 20L);
        hideTasks.put(uuid, newTask);
    }

    public void removeBossBar(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = hideTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        BossBar bar = activeBossBars.remove(uuid);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void spawnHarmlessFirework(Location loc) {
        if (loc.getWorld() == null) return;

        Location safeLoc = loc.clone().add(2.0, 2.5, 0.0);

        Firework fw = safeLoc.getWorld().spawn(safeLoc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.GREEN, Color.YELLOW, Color.LIME)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build();

        meta.addEffect(effect);
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        plugin.getServer().getScheduler().runTaskLater(plugin, fw::detonate, 1L);
    }
}