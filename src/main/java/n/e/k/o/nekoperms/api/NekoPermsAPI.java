package n.e.k.o.nekoperms.api;

import n.e.k.o.nekoperms.utils.Config;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NekoPermsAPI
{

    public static boolean doReload = false;
    private static NekoPermsAPI instance = null;
    private static final Object instanceLock = new Object();
    private static final List<CompletableFuture<NekoPermsAPI>> asyncs = new ArrayList<>();
    private final Config config;
    private final Logger logger;

    private final Map<UUID, NekoUser> usersCache = new HashMap<>();
    private final Map<String, NekoGroup> groupsCache = new HashMap<>();

    private NekoPermsAPI(Config config, Logger logger)
    {
        this.config = config;
        this.logger = logger;
    }

    public static NekoPermsAPI init(Config config, Logger logger)
    {
        synchronized (instanceLock)
        {
            if (doReload)
            {
                instance.loadGroups();
                instance.loadOnlinePlayers();
                doReload = false;
            }
            else
            {
                if (instance != null)
                    throw new RuntimeException("ApiNekoPerms has already been initialized!");
                if (!correctCaller())
                    throw new RuntimeException("init() was called from the wrong class");
                instance = new NekoPermsAPI(config, logger);
                instance.loadGroups();
                if (!asyncs.isEmpty())
                {
                    for (CompletableFuture<NekoPermsAPI> async : asyncs)
                        async.complete(instance);
                    asyncs.clear();
                }
            }
            return instance;
        }
    }

    private void loadOnlinePlayers()
    {
        for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
        {
            try
            {
                final UUID uuid = player.getUniqueID();
                final NekoUser user = getOrCreateUser(uuid, player.getName().getString());
                user.loadAsync();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    public void clear()
    {
        usersCache.clear();
        groupsCache.clear();
    }

    private void loadGroups()
    {
        File[] files = config.groupsFolder.listFiles(f -> f.isFile() && f.getName().endsWith(".yml"));
        if (files == null || files.length == 0)
            return;

        for (File yml : files)
        {
            try
            {
                Map<String, Object> map;
                try (FileInputStream fis = new FileInputStream(yml))
                {
                    map = new Yaml().loadAs(fis, Map.class);
                }
                if (map.isEmpty())
                    continue;

                String id = map.get("id").toString();
                String name = map.get("name").toString();
                List<String> subGroups = (List<String>) map.getOrDefault("groups", new ArrayList<>());
                if (subGroups == null) subGroups = new ArrayList<>();
                List<String> permissions = (List<String>) map.getOrDefault("permissions", new ArrayList<>());
                if (permissions == null) permissions = new ArrayList<>();

                // Make groupId and all permission nodes lowercase
                subGroups = subGroups.stream().map(String::toLowerCase).collect(Collectors.toList());
                permissions = permissions.stream().map(String::toLowerCase).collect(Collectors.toList());

                NekoGroup group = new NekoGroup(id, name, subGroups, permissions, this, config);
                groupsCache.put(id, group);

                logger.info("Loaded " + group);
            }
            catch (Throwable t)
            {
                logger.error(t);
                t.printStackTrace();
            }
        }

        for (NekoGroup nekoGroup : new ArrayList<>(groupsCache.values()))
            nekoGroup.loadSubGroups();

        logger.info("Successfully loaded a total of " + groupsCache.size() + " group" + (groupsCache.size() == 1 ? "" : "s") + ".");
    }

    private static boolean correctCaller()
    {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length < 3)
            return false;
        String className = elements[2].getClassName();
        if ("n.e.k.o.nekoperms.NekoPerms".equals(className))
            return true;
        for (StackTraceElement elem : elements)
            if ("n.e.k.o.nekoperms.NekoPerms".equals(elem.getClassName()))
                return true;
        return false;
    }

    public static NekoPermsAPI get()
    {
        synchronized (instanceLock)
        {
            return instance;
        }
    }

    public static CompletableFuture<NekoPermsAPI> getAsync()
    {
        synchronized (instanceLock)
        {
            if (instance != null)
                return CompletableFuture.completedFuture(instance);
        }
        CompletableFuture<NekoPermsAPI> async = new CompletableFuture<>();
        asyncs.add(async);
        return async;
    }

    public NekoUser getOrCreateUser(UUID uuid)
    {
        return getOrCreateUser(uuid, null);
    }

    public NekoUser getOrCreateUser(UUID uuid, String backupUsername)
    {
        NekoUser nekoUser;
        if (usersCache.containsKey(uuid))
        {
            nekoUser = usersCache.get(uuid);
            if (backupUsername != null && nekoUser.updateBackupUsername(backupUsername))
                nekoUser.saveAsync();
            return nekoUser;
        }
        nekoUser = new NekoUser(uuid, config, logger, this, backupUsername);
        usersCache.put(uuid, nekoUser);
        return nekoUser;
    }

    public NekoGroup getGroup(String id)
    {
        NekoGroup group = groupsCache.get(id);
        if (group == null)
            return null;
        if (group.hasBeenDeleted())
        {
            groupsCache.remove(id);
            return null;
        }
        return group;
    }

    public String[] getGroupIds()
    {
        if (groupsCache.isEmpty())
            return new String[0];
        return groupsCache.keySet().toArray(new String[0]);
    }

    public String[] getGroupNames()
    {
        if (groupsCache.isEmpty())
            return new String[0];
        String[] names = new String[groupsCache.size()];
        int i = 0;
        for (String id : groupsCache.keySet())
            names[i++] = groupsCache.get(id).getName();
        return names;
    }

    public Map<String, String> getGroups()
    {
        if (groupsCache.isEmpty())
            return new HashMap<>();
        Map<String, String> map = new HashMap<>();
        for (String id : groupsCache.keySet())
            map.put(id, groupsCache.get(id).getName());
        return map;
    }

    public NekoGroup getOrCreateGroup(String id, String name)
    {
        NekoGroup group;
        if (groupsCache.containsKey(id))
        {
            group = groupsCache.get(id);
            if (group.hasBeenDeleted())
                groupsCache.remove(id);
            else
                return groupsCache.get(id);
        }
        group = new NekoGroup(id, name, new ArrayList<>(), new ArrayList<>(), this, config);
        groupsCache.put(id, group);
        return group;
    }

    public void deleteGroup(NekoGroup group)
    {
        groupsCache.remove(group.getId());
        group.markDeleted();
    }

}
