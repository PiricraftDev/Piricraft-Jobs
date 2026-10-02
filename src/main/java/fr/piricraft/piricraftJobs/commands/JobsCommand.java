package fr.piricraft.piricraftJobs.commands;

import fr.piricraft.piricraftJobs.gui.JobsMainMenu;
import fr.piricraft.piricraftJobs.managers.JobsDatabaseManager;
import fr.piricraft.piricraftJobs.managers.JobsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JobsCommand implements CommandExecutor {

    private final JobsManager jobsManager;
    private final JobsDatabaseManager dbManager;

    public JobsCommand(JobsManager jobsManager, JobsDatabaseManager dbManager) {
        this.jobsManager = jobsManager;
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only player can execute this command.");
            return true;
        }

        JobsMainMenu menu = new JobsMainMenu(jobsManager, dbManager);
        menu.open(player);

        return true;
    }
}
