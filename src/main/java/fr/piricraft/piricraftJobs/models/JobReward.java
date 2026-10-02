package fr.piricraft.piricraftJobs.models;

public class JobReward {

    private final double money;
    private final double experience;

    public JobReward(double money, double experience) {
        this.money = money;
        this.experience = experience;
    }

    public double getMoney() {
        return money;
    }

    public double getExperience() {
        return experience;
    }
}
