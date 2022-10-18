package n.e.k.o.nekoperms.api;

import com.mojang.authlib.GameProfile;
import n.e.k.o.nekoperms.NekoPerms;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import n.e.k.o.nekoperms.utils.PermissionHelper;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class NekoUser
{

    private final static ExecutorService executor = Executors.newFixedThreadPool(10);

    private boolean hasLoadedData = false;
    private File uuidFile = null;

    private String username = null;
    private final UUID uuid;
    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;
    private String backupUsername;

    private final List<NekoGroup> userGroups = new ArrayList<>();
    private final List<String> userPermissions = new ArrayList<>();

    NekoUser(UUID uuid, Config config, Logger logger, NekoPermsAPI api)
    {
        this(uuid, config, logger, api, null);
    }

    NekoUser(UUID uuid, Config config, Logger logger, NekoPermsAPI api, String backupUsername)
    {
        this.uuid = uuid;
        this.api = api;
        this.config = config;
        this.logger = logger;
        this.backupUsername = backupUsername;
    }

    public boolean hasPermission(String node)
    {
        if (!hasLoadedData)
            return false;

        if (node.isEmpty() || node.endsWith("."))
            return false;

        for (String perm : userPermissions)
        {
            // All permissions
            if (perm.equals("*"))
                return true;

            // Specific permission
            if (perm.equals(node))
                return true;

            if (perm.endsWith("."))
                return false;

            // Check if 'node' is part of 'perm'
            if (PermissionHelper.checkPermission(node, perm))
                return true;
        }

        for (NekoGroup group : userGroups)
            if (group.hasPermission(node))
                return true;

        return false;
    }

    /* Set permission for user (not group) */
    public void setPermission(String node)
    {
        NekoPerms.allPermissionNodes.put(node.toLowerCase(), null);
        userPermissions.add(node.toLowerCase());
    }

    /* Set permission for user (not group), including description */
    public void setPermission(String node, String desc)
    {
        throw new UnsupportedOperationException("This method isn't implemented yet.");
//        NekoPerms.allPermissionNodes.put(node.toLowerCase(), desc);
//        userPermissions.add(node.toLowerCase());
    }

    /* Unset (remove) permission for user (not group) */
    public void unsetPermission(String node)
    {
        NekoPerms.allPermissionNodes.remove(node.toLowerCase());

        if (!userPermissions.remove(node.toLowerCase()))
            userPermissions.removeIf(n -> n.equalsIgnoreCase(node));
    }

    public boolean loadBlock()
    {
        if (hasLoadedData)
            return true;
        try
        {
            String uuidString = uuid.toString().toLowerCase().replace("-", "");
            String first = uuidString.substring(0, 1);

            File folder = new File(config.userdataFolder, first);
            if (!folder.exists() && !folder.mkdirs())
                throw new Throwable("Couldn't create folder: '/config/userdata/" + first + "/'");

            GameProfile profile = ServerLifecycleHooks.getCurrentServer().getPlayerProfileCache().getProfileByUUID(uuid);
            String offlineUsername;
            if (profile != null)
            {
                offlineUsername = profile.getName();
                if (offlineUsername == null)
                    offlineUsername = backupUsername;
            }
            else offlineUsername = backupUsername;
            if (offlineUsername == null)
                offlineUsername = Utils.fetchFromAPI(uuid);
            if (offlineUsername == null)
            {
                logger.error("Couldn't fetch offlineUsername for UUID '" + uuid + "'.");
                return false;
            }
            uuidFile = new File(folder, uuidString + ".yml");

            Map<String, Object> map;
            if (uuidFile.exists())
            {
                Yaml yaml = new Yaml();
                // Load data from file
                try (FileInputStream fis = new FileInputStream(uuidFile))
                {
                    map = yaml.loadAs(fis, Map.class);
                }
                if (map != null && !map.isEmpty())
                {
                    String username = (String) map.getOrDefault("username", null);
                    boolean usernameChange = username == null || username.isEmpty() || !username.equals(offlineUsername);
                    this.username = usernameChange ? offlineUsername : username;

                    List<String> groups = (List<String>) map.getOrDefault("groups", new ArrayList<>());
                    if (groups == null) groups = new ArrayList<>();
                    List<String> permissions = (List<String>) map.getOrDefault("permissions", new ArrayList<>());
                    if (permissions == null) permissions = new ArrayList<>();

                    // Make groupId and all permission nodes lowercase
                    groups = groups.stream().map(String::toLowerCase).collect(Collectors.toList());
                    permissions = permissions.stream().map(String::toLowerCase).collect(Collectors.toList());

                    if (usernameChange)
                    {
                        map.put("username", offlineUsername);
                        try (OutputStream out = Files.newOutputStream(uuidFile.toPath()))
                        {
                            try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8))
                            {
                                yaml.dump(map, osw);
                            }
                        }
                    }

                    userGroups.clear();
                    for (String group : groups)
                    {
                        NekoGroup nekoGroup = api.getGroup(group);
                        if (nekoGroup == null)
                        {
                            logger.warn("Group '" + group + "' not found when loading user '" + getUsernameOrUUID() + "'.");
                            continue;
                        }
                        if (nekoGroup.hasBeenDeleted())
                            continue;
                        userGroups.add(nekoGroup);
                    }

                    userPermissions.clear();
                    userPermissions.addAll(permissions);

                    permissions.forEach(key -> NekoPerms.allPermissionNodes.putIfAbsent(key, null));

                    return hasLoadedData = true;
                }
            }

            // Create new file, insert data, save file
            final String finalOfflineUsername = offlineUsername;
            map = new HashMap<String, Object>()
            {{
                put("uuid", uuid.toString());
                put("username", finalOfflineUsername);
            }};
            this.username = offlineUsername;
            this.userGroups.clear();
            this.userPermissions.clear();
            try (OutputStream out = Files.newOutputStream(uuidFile.toPath()))
            {
                try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8))
                {
                    new Yaml().dump(map, osw);
                }
            }

        }
        catch (Throwable t)
        {
            t.printStackTrace();
            return false;
        }
        hasLoadedData = true;
        return true;
    }

    public CompletableFuture<Boolean> loadAsync(boolean force)
    {
        if (force)
            hasLoadedData = false;
        return loadAsync();
    }

    public CompletableFuture<Boolean> loadAsync()
    {
        if (hasLoadedData)
            return CompletableFuture.completedFuture(true);
        CompletableFuture<Boolean> async = new CompletableFuture<>();
        executor.execute(() -> async.complete(loadBlock()));
        return async;
    }

    public boolean hasLoadedData()
    {
        return hasLoadedData;
    }

    public String getUsernameOrUUID()
    {
        return username != null ? username : (backupUsername != null ? backupUsername : uuid.toString());
    }

    @Override
    public String toString()
    {
        return "NekoUser{" +
                    "uuid=" + uuid +
                    ", username='" + getUsernameOrUUID() + "'" +
                    (hasLoadedData ?
                        (", userGroups=" + userGroups +
                        ", userPermissions=" + userPermissions) :
                    ", hasLoadedData=false") +
                '}';
    }

    public CompletableFuture<Boolean> saveAsync()
    {
        if (!hasLoadedData)
            return CompletableFuture.completedFuture(false);
        CompletableFuture<Boolean> async = new CompletableFuture<>();
        executor.execute(() ->
        {
            byte[] existingData = new byte[0];
            try
            {

                StringJoiner sj = new StringJoiner("\n");
                sj.add("uuid: " + uuid.toString());
                sj.add("username: " + getUsernameOrUUID());

                sj.add("groups:");
                userGroups.removeIf(g -> g == null || g.hasBeenDeleted());
                for (NekoGroup group : userGroups)
                    sj.add("  - " + group.getId());

                sj.add("permissions:");
                for (String permNode : userPermissions)
                    sj.add("  - " + (permNode.equals("*") ? ("\"*\"") : permNode));

                try
                {
                    existingData = Files.readAllBytes(uuidFile.toPath());
                }
                catch (Throwable ignored)
                {
                }

                try (OutputStream out = Files.newOutputStream(uuidFile.toPath()))
                {
                    try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8))
                    {
                        osw.write(sj.toString());
                    }
                }

                async.complete(true);
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                if (existingData.length > 0)
                    try
                    {
                        Files.write(uuidFile.toPath(), existingData);
                    }
                    catch (Throwable ignored)
                    {
                    }
                async.complete(false);
            }
        });
        return async;
    }

    public List<NekoGroup> getGroups()
    {
        return userGroups;
    }

    public void addGroup(NekoGroup group)
    {
        userGroups.add(group);
    }

    public void removeGroup(NekoGroup group)
    {
        if (!userGroups.remove(group))
            userGroups.removeIf(g -> g == null || g.getId().equals(group.getId()));
        userGroups.removeIf(g -> g == null || g.hasBeenDeleted());
    }

    public String getUUID()
    {
        return uuid == null ? null : uuid.toString();
    }

    public List<String> getUserPermissionNodes()
    {
        return userPermissions;
    }

    public void getAllPermissionNodes(List<String> nodes)
    {
        nodes.addAll(userPermissions);
        List<NekoGroup> groupsChecked = new ArrayList<>();
        for (NekoGroup group : getGroups())
            group.getAllPermissionNodes(nodes, groupsChecked);
    }

    public boolean updateBackupUsername(@Nonnull String backupUsername)
    {
        if (!backupUsername.equals(this.backupUsername))
        {
            if (!hasLoadedData)
            {
                this.backupUsername = backupUsername;
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Used to determine whether the user making this call is subject to
     * teleportations.
     * @return whether the user making this call is a goat
     */
    public boolean isUserAGoat()
    {
        return false;
    }

}
