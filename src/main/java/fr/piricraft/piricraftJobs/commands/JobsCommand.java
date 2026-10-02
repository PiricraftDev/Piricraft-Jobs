package fr.piricraft.piricraftJobs.commands;

import fr.piricraft.piricraftCore.utils.TextUtils;
import fr.piricraft.piricraftJobs.gui.JobsMainMenu;
import fr.piricraft.piricraftJobs.managers.JobsDatabaseManager;
import fr.piricraft.piricraftJobs.managers.JobsManager;
import fr.piricraft.piricraftJobs.models.JobProfile;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JobsCommand implements CommandExecutor, TabCompleter {

    private final JobsManager jobsManager;
    private final JobsDatabaseManager dbManager;

    public JobsCommand(JobsManager jobsManager, JobsDatabaseManager dbManager) {
        this.jobsManager = jobsManager;
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seul un joueur peut exécuter cette commande.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("togglebar")) {
            JobProfile profile = dbManager.getProfileFromCache(player.getUniqueId());
            if (profile == null) {
                player.sendMessage(TextUtils.color("<red>[Métier]</red> Impossible de charger votre profil."));
                return true;
            }

            boolean newState = !profile.isShowBossBar();
            profile.setShowBossBar(newState);
            dbManager.saveProfileAsync(profile);

            if (newState) {
                player.sendMessage(TextUtils.color("<green>[Métier]</green> La barre de progression est maintenant <bold>activée</bold>."));
            } else {
                player.sendMessage(TextUtils.color("<red>[Métier]</red> La barre de progression est maintenant <bold>désactivée</bold>."));

            }
            return true;
        }

        JobsMainMenu menu = new JobsMainMenu(jobsManager, dbManager);
        menu.open(player);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("togglebar".startsWith(args[0].toLowerCase())) {
                completions.add("togglebar");
            }
        }
        return completions;
    }
}