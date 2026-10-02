package fr.piricraft.piricraftJobs.models;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class JobProfile {

    private final UUID playerUuid;
    private JobType activeJob;
    private final Map<JobType, Integer> levels = new EnumMap<>(JobType.class);
    private final Map<JobType, Double> experience = new EnumMap<>(JobType.class);

    public JobProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        for (JobType job : JobType.values()) {
            levels.put(job, 1);
            experience.put(job, 0.0);
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public JobType getActiveJob() {
        return activeJob;
    }

    public void setActiveJob(JobType activeJob) {
        this.activeJob = activeJob;
    }

    public int getLevel(JobType job) {
        return levels.getOrDefault(job, 1);
    }

    public double getExperience(JobType job) {
        return experience.getOrDefault(job, 0.0);
    }

    public boolean addExperience(JobType job, double amount, Function<Integer, Double> reqExpCalculator) {
        double currentXp = getExperience(job) + amount;
        int currentLevel = getLevel(job);
        boolean leveledUp = false;

        double requiredXp = reqExpCalculator.apply(currentLevel);

        while (currentXp >= requiredXp) {
            currentXp -= requiredXp;
            currentLevel++;
            leveledUp = true;
            requiredXp = reqExpCalculator.apply(currentLevel);
        }

        experience.put(job, currentXp);
        levels.put(job, currentLevel);

        return leveledUp;
    }
}