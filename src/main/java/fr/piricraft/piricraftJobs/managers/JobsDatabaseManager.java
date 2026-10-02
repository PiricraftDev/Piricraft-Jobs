package fr.piricraft.piricraftJobs.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.piricraft.piricraftJobs.PiricraftJobs;
import fr.piricraft.piricraftJobs.models.JobProfile;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JobsDatabaseManager {

    private Connection connection;
    private final PiricraftJobs plugin;
    private final Map<UUID, JobProfile> profileCache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingLoads = ConcurrentHashMap.newKeySet();
    private final Gson gson = new GsonBuilder().create();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public JobsDatabaseManager(PiricraftJobs plugin) {
        this.plugin = plugin;
    }

    public boolean initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            String query = "CREATE TABLE IF NOT EXISTS piricraft_jobs ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "active_job VARCHAR(32), "
                    + "data_json TEXT"
                    + ");";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'initialisation de la BDD SQLite : " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<JobProfile> loadProfileAsync(UUID uuid) {
        pendingLoads.add(uuid);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String query = "SELECT data_json FROM piricraft_jobs WHERE uuid = ?;";

                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String json = rs.getString("data_json");
                        JobProfile profile = gson.fromJson(json, JobProfile.class);
                        profileCache.put(uuid, profile);
                        return profile;
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Erreur SQL lors du chargement du profil pour " + uuid + " : " + e.getMessage());
                    return null;
                }

                JobProfile newProfile = new JobProfile(uuid);
                profileCache.put(uuid, newProfile);
                return newProfile;
            } finally {
                pendingLoads.remove(uuid);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Boolean> saveProfileAsync(JobProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            if (profile == null) return false;

            String query = "REPLACE INTO piricraft_jobs (uuid, active_job, data_json) VALUES (?, ?, ?);";

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, profile.getPlayerUuid().toString());
                stmt.setString(2, profile.getActiveJob() != null ? profile.getActiveJob().name() : null);
                stmt.setString(3, gson.toJson(profile));
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la sauvegarde du profil pour " + profile.getPlayerUuid() + " : " + e.getMessage());
                return false;
            }
        }, dbExecutor);
    }

    public void unloadProfile(UUID uuid) {
        JobProfile profile = profileCache.get(uuid);
        if (profile != null) {
            saveProfileAsync(profile).thenAccept(success -> {
                if (success) {
                    profileCache.remove(uuid);
                } else {
                    plugin.getLogger().warning("Le profil de " + uuid + " est conservé en cache en raison d'un échec de sauvegarde.");
                }
            });
        }
    }

    public void closeDatabase() {
        for (JobProfile profile : profileCache.values()) {
            dbExecutor.submit(() -> {
                String query = "REPLACE INTO piricraft_jobs (uuid, active_job, data_json) VALUES (?, ?, ?);";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, profile.getPlayerUuid().toString());
                    stmt.setString(2, profile.getActiveJob() != null ? profile.getActiveJob().name() : null);
                    stmt.setString(3, gson.toJson(profile));
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Erreur lors de la sauvegarde finale pour " + profile.getPlayerUuid() + " : " + e.getMessage());
                }
            });
        }

        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
        }

        profileCache.clear();
        pendingLoads.clear();

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors de la fermeture de la connexion BDD : " + e.getMessage());
            }
        }
    }

    public JobProfile getProfileFromCache(UUID uuid) {
        return profileCache.get(uuid);
    }

    public boolean isProfileLoading(UUID uuid) {
        return pendingLoads.contains(uuid);
    }
}