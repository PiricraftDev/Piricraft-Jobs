package fr.piricraft.piricraftJobs.models;

import org.bukkit.Material;

public enum JobType {

    MINER("Mineur", Material.DIAMOND_PICKAXE),
    WOODCUTTER("Bûcheron", Material.DIAMOND_AXE),
    FARMER("Fermier", Material.DIAMOND_HOE),
    HUNTER("Chasseur", Material.DIAMOND_SWORD),
    FISHERMAN("Pêcheur", Material.FISHING_ROD);

    private final String displayName;
    private final Material icon;

    JobType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

}
