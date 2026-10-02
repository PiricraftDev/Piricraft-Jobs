package fr.piricraft.piricraftJobs.models;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class JobProfile {

    private final UUID playerUuid;
    private final Map<JobType, Integer> jobLevels;
    private final Map<JobType, Double> jobExperience;
    private JobType activeJob;

    public JobProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.jobLevels = new EnumMap<>(JobType.class);
        this.jobExperience = new EnumMap<>(JobType.class);
        this.activeJob = null;

        for (JobType job : JobType.values()) {
            this.jobLevels.put(job, 1);
            this.jobExperience.put(job, 0.0);
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

    public int getLevel(JobType jobType) {
        return jobLevels.getOrDefault(jobType, 1);
    }

    public double getExperience(JobType jobType) {
        return jobExperience.getOrDefault(jobType, 0.0);
    }

    public void setLevel(JobType jobType, int level) {
            jobLevels.put(jobType, level);
    }

    public void setExperience(JobType jobType, double experience) {
        jobExperience.put(jobType, experience);
    }

    public boolean addExperience(JobType jobType, double amount, double expRequiredForNextLevel) {
        double currentExp = getExperience(jobType) + amount;

        if (currentExp >= expRequiredForNextLevel) {
            int currentLevel = getLevel(jobType);
            this.jobLevels.put(jobType, currentLevel + 1);
            this.jobExperience.put(jobType, currentExp - expRequiredForNextLevel);
            return true;
        }

        this.jobExperience.put(jobType, currentExp);
        return false;
    }
}
