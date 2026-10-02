package fr.piricraft.piricraftJobs.gui;

import fr.piricraft.piricraftCore.models.PiricraftMenu;
import fr.piricraft.piricraftCore.utils.TextUtils;
import fr.piricraft.piricraftJobs.managers.JobsDatabaseManager;
import fr.piricraft.piricraftJobs.managers.JobsManager;
import fr.piricraft.piricraftJobs.models.JobProfile;
import fr.piricraft.piricraftJobs.models.JobType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static fr.piricraft.piricraftCore.managers.MenuManager.createItem;

public class JobsMainMenu extends PiricraftMenu {

    private final JobsManager jobsManager;
    private final JobsDatabaseManager dbManager;

    public JobsMainMenu(JobsManager jobsManager, JobsDatabaseManager dbManager) {
        super(TextUtils.color("<bold><green>Sélection des Métiers</green></bold>"), 27);
        this.jobsManager = jobsManager;
        this.dbManager = dbManager;
    }

    @Override
    public void setMenuItems(Player player) {
        inventory.clear();

        JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(TextUtils.color("<red>Chargement de ton profil en cours, réessaie dans un instant...</red>"));
            return;
        }

        JobType activeJob = profile.getActiveJob();
        JobType[] jobs = JobType.values();

        int[] slots = {10, 12, 14, 16};
        int index = 0;

        for (JobType job : jobs) {
            if (index >= slots.length) break;

            boolean isActive = (activeJob == job);
            int level = profile.getLevel(job);
            double xp = profile.getExperience(job);
            double reqXp = jobsManager.getRequiredExperience(level);

            Material icon = matchJobIcon(job);
            Component displayName = TextUtils.color("<bold><gold>" + job.getDisplayName() + "</gold></bold>");

            List<Component> lore = new ArrayList<>();
            lore.add(TextUtils.color("<gray>Niveau : </gray><yellow>" + level + "</yellow>"));
            lore.add(TextUtils.color("<gray>XP : </gray><green>" + String.format("%.1f", xp) + "</green><gray>/</gray><green>" + String.format("%.1f", reqXp) + "</green>"));
            lore.add(Component.empty());

            if (isActive) {
                lore.add(TextUtils.color("<green>✔ Métier actuel</green>"));
                lore.add(TextUtils.color("<red>Clic pour quitter ce métier</red>"));
            } else {
                lore.add(TextUtils.color("<yellow>Clic pour rejoindre ce métier</yellow>"));
            }

            ItemStack item = createItem(icon, displayName, lore);
            inventory.setItem(slots[index], item);

            index++;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of()));
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getSlot();
        JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
        if (profile == null) return;

        JobType selectedJob = null;
        if (slot == 10) selectedJob = JobType.values().length > 0 ? JobType.values()[0] : null;
        else if (slot == 12) selectedJob = JobType.values().length > 1 ? JobType.values()[1] : null;
        else if (slot == 14) selectedJob = JobType.values().length > 2 ? JobType.values()[2] : null;
        else if (slot == 16) selectedJob = JobType.values().length > 3 ? JobType.values()[3] : null;

        if (selectedJob == null) return;

        if (profile.getActiveJob() == selectedJob) {
            profile.setActiveJob(null);
            player.sendMessage(TextUtils.color("<bold><red>[Métier]</red></bold> Tu as quitté ton métier de <gold>" + selectedJob.getDisplayName() + "</gold>."));
        } else {
            profile.setActiveJob(selectedJob);
            player.sendMessage(TextUtils.color("<bold><green>[Métier]</green></bold> Tu es désormais <gold>" + selectedJob.getDisplayName() + "</gold> !"));
        }

        dbManager.saveProfileAsync(profile);
        setMenuItems(player);
    }

    private Material matchJobIcon(JobType job) {
        return switch (job.name()) {
            case "MINER" -> Material.DIAMOND_PICKAXE;
            case "FARMER" -> Material.DIAMOND_HOE;
            case "HUNTER" -> Material.DIAMOND_SWORD;
            case "WOODCUTTER" -> Material.DIAMOND_AXE;
            default -> Material.BOOK;
        };
    }
}