package n.e.k.o.nekoperms.api;

import n.e.k.o.nekoperms.NekoPerms;
import n.e.k.o.nekoperms.utils.PermissionHelper;
import n.e.k.o.nekoperms.utils.Config;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NekoGroup
{

    private final static ExecutorService executor = Executors.newFixedThreadPool(10);

    private final String id;
    private final String name;
    private final List<String> strGroups;
    private final List<String> groupPermissions;
    private final NekoPermsAPI api;
    private final Config config;
    private final List<NekoGroup> subGroups;

    private boolean hasBeenDeleted = false;

    NekoGroup(String id, String name, List<String> strGroups, List<String> groupPermissions, NekoPermsAPI api, Config config)
    {
        this.id = id;
        this.name = name;
        this.strGroups = strGroups;
        this.groupPermissions = groupPermissions;
        this.api = api;
        this.config = config;
        this.subGroups = new ArrayList<>();
    }

    public void loadSubGroups()
    {
        for (String group : strGroups)
        {
            NekoGroup nekoGroup = api.getGroup(group);
            if (nekoGroup == null)
                continue;
            if (nekoGroup == this)
                continue;
            subGroups.add(nekoGroup);
        }
    }

    public boolean hasPermission(String node, List<String> alreadyCheckedGroups)
    {
        if (hasBeenDeleted)
            return false;

        if (node.isEmpty() || node.endsWith("."))
            return false;

        for (String perm : groupPermissions)
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

        for (NekoGroup group : subGroups)
        {
            if (alreadyCheckedGroups.contains(group.id))
            {
                System.out.println("Already checked group: " + group.id + " (detected possibly infinite-loop)");
                continue;
            }
            alreadyCheckedGroups.add(group.id);
            if (group.hasPermission(node, alreadyCheckedGroups))
                return true;
        }

        return false;
    }

    public boolean hasPermission(String node)
    {
        if (hasBeenDeleted)
            return false;
        return hasPermission(node, new ArrayList<String>(){{ add(id); }});
    }

    public void setPermission(String node)
    {
        if (hasBeenDeleted)
            return;
        NekoPerms.allPermissionNodes.put(node.toLowerCase(), null);
        groupPermissions.add(node.toLowerCase());
    }

    public void removePermission(String node)
    {
        NekoPerms.allPermissionNodes.remove(node.toLowerCase());

        if (!groupPermissions.remove(node.toLowerCase()))
            groupPermissions.removeIf(perm -> perm.equalsIgnoreCase(node));
    }

    @Override
    public String toString()
    {
        return "NekoGroup{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", strGroups=" + (strGroups != null ? strGroups : "[]") +
                    ", strPermissions=" + (groupPermissions != null ? groupPermissions : "[]") +
                '}';
    }

    public String getId()
    {
        if (hasBeenDeleted)
            return null;
        return id;
    }

    public String getName()
    {
        if (hasBeenDeleted)
            return null;
        return name;
    }

    public CompletableFuture<Integer> saveAsync()
    {
        CompletableFuture<Integer> async = new CompletableFuture<>();
        executor.submit(() ->
        {
            try
            {
                File file = new File(config.groupsFolder, id + ".yml");
                if (hasBeenDeleted)
                {
                    // Delete file
                    if (file.exists() && !file.delete())
                    {
                        file.deleteOnExit(); // Delete file when server stops perhaps?
                        async.complete(1); // Failed deleting file
                    }
                    async.complete(0);
                    return;
                }

                // Write to file
                byte[] existingData = new byte[0];
                try
                {

                    StringJoiner sj = new StringJoiner("\n");
                    sj.add("id: " + id);
                    sj.add("name: " + name);

                    sj.add("groups:");
                    for (NekoGroup group : subGroups)
                        if (group != null && !group.hasBeenDeleted)
                            sj.add("  - " + group.getId());

                    sj.add("permissions:");
                    for (String permNode : groupPermissions)
                        sj.add("  - " + (permNode.equals("*") ? ("\"*\"") : permNode));

                    try
                    {
                        if (file.exists())
                            existingData = Files.readAllBytes(file.toPath());
                    }
                    catch (Throwable ignored)
                    {
                    }

                    try (OutputStream out = Files.newOutputStream(file.toPath());
                         OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8))
                    {
                        osw.write(sj.toString());
                    }

                    async.complete(0);
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                    if (existingData.length > 0)
                        try
                        {
                            Files.write(file.toPath(), existingData);
                        }
                        catch (Throwable ignored)
                        {
                        }
                    async.complete(-1);
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                async.complete(-1);
            }
        });
        return async;
    }

    /**
     * Mark group for deletion
     */
    void markDeleted()
    {
        this.hasBeenDeleted = true;
    }

    /**
     * @return boolean true/false if the group has been marked for deletion or has been deleted
     */
    public boolean hasBeenDeleted()
    {
        return hasBeenDeleted;
    }

    /**
     * @return A list of all permission nodes for this single group (no sub-groups).
     */
    public Collection<String> getLocalPermissionNodes()
    {
        return groupPermissions;
    }

    /**
     * @return A list of all permission nodes for this single group, INCLUDING all sub-groups!
     */
    public List<String> getAllPermissionNodes()
    {
        List<String> list = new ArrayList<>();
        List<NekoGroup> groupsChecked = new ArrayList<>();
        getAllPermissionNodes(list, groupsChecked);
        return list;
    }

    /**
     * Recursively fetch all permission nodes from this group,
     * including all subgroups, and store them in the {@link List<String> list} variable.
     * @param list The {@link List<String>} instance which will be filled with nodes
     * @param groupsChecked The {@link List<NekoGroup> groupsChecked} instance which will be filled with all the groups which has already been checked, to stop an infinite loop
     */
    void getAllPermissionNodes(List<String> list, List<NekoGroup> groupsChecked)
    {
        if (groupsChecked.contains(this))
            return;
        groupsChecked.add(this);
        list.addAll(groupPermissions);
        for (NekoGroup subGroup : subGroups)
        {
            if (groupsChecked.contains(subGroup))
                continue;
            subGroup.getAllPermissionNodes(list, groupsChecked);
        }
    }

}
