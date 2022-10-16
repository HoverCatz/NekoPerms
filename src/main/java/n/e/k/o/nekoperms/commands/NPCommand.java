package n.e.k.o.nekoperms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoUser;
import n.e.k.o.nekoperms.commands.group.*;
import n.e.k.o.nekoperms.commands.group.permission.NPGroupPermissionList;
import n.e.k.o.nekoperms.commands.user.*;
import n.e.k.o.nekoperms.commands.user.permission.NPUserPermissionUnSet;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import n.e.k.o.nekoperms.NekoPerms;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.commands.group.permission.NPGroupPermission;
import n.e.k.o.nekoperms.commands.group.permission.NPGroupPermissionSet;
import n.e.k.o.nekoperms.commands.group.permission.NPGroupPermissionUnSet;
import n.e.k.o.nekoperms.commands.user.permission.NPUserPermission;
import n.e.k.o.nekoperms.commands.user.permission.NPUserPermissionList;
import n.e.k.o.nekoperms.commands.user.permission.NPUserPermissionSet;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.Logger;

public class NPCommand implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPCommand(NekoPermsAPI api, Config config, Logger logger)
    {
        this.api = api;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public int run(CommandContext<CommandSource> ctx) throws CommandSyntaxException
    {
        CommandSource source = ctx.getSource();
        if (!Utils.hasPermissionForCommand(source, api, config))
            return SINGLE_SUCCESS;

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms] Sub commands:"), true);
        source.sendFeedback(StringColorUtils.getColoredString("&a/np user <username> &6# Get the user object"), true);
        source.sendFeedback(StringColorUtils.getColoredString("&a/np group <groupId> &6# Get the group object"), true);
        source.sendFeedback(StringColorUtils.getColoredString("&a/np groups &6# Get all group objects"), true);
        source.sendFeedback(StringColorUtils.getColoredString("&a/np reload &6# Reload the permissions plugin"), true);
        source.sendFeedback(StringColorUtils.getColoredString("&a/np permissions &6# List all permission nodes"), true);

        return SINGLE_SUCCESS;
    }

    public static void register(NekoPerms plugin, CommandDispatcher<CommandSource> dispatcher, NekoPermsAPI api, Config config, Logger logger)
    {

        /* Register main command */
        LiteralArgumentBuilder<CommandSource> np = Commands.literal("np")
            .requires(s -> checkAccess(s, api, config))
            .executes(new NPCommand(api, config, logger));

        /* Create sub-commands */
        LiteralArgumentBuilder<CommandSource> user = Commands.literal("user"); // np user
        {
            user.requires(s -> checkAccess(s, api, config))
                .executes(new NPUser(api, config, logger))

            .then(Commands.argument("username", StringArgumentType.string()) // np user <username>
                .requires(s -> checkAccess(s, api, config))
                .suggests((ctx, builder) -> NPUser.UsernameParam.suggests())
                .executes(new NPUser.UsernameParam(api, config, logger))
// {
                    .then(Commands.literal("info") // np user <username> info
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPUserInfo(api, config, logger))
                    )

                    .then(Commands.literal("load") // np user <username> load
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPUserLoad(api, config, logger))
                    )

                    .then(Commands.literal("permission") // np user <username> permission
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPUserPermission(api, config, logger))
                        .then(Commands.literal("set")        // np user <username> permission set
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserPermissionSet.NoArgument(api, config, logger))
                            .then(Commands.argument("node", StringArgumentType.string())// np user <username> permission set <node>
                                .requires(s -> checkAccess(s, api, config))
                                .executes(new NPUserPermissionSet(api, config, logger))))
                        .then(Commands.literal("unset")        // np user <username> permission unset
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserPermissionUnSet.NoArgument(api, config, logger))
                            .then(Commands.argument("node", StringArgumentType.string())// np user <username> permission unset <node>
                                .requires(s -> checkAccess(s, api, config))
                                .executes(new NPUserPermissionUnSet(api, config, logger))))
                        .then(Commands.literal("list")        // np user <username> permission list
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserPermissionList(api, config, logger)))
                    )

                    .then(Commands.literal("check") // np user <username> check
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPUserCheck.NoArgument(api, config, logger))
                        .then(Commands.argument("node", StringArgumentType.string()) // np user <username> check <node>
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserCheck(api, config, logger)))
                    )

                    .then(Commands.literal("groups") // np user <username> groups
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPUserGroups.NoArgument(api, config, logger))
                        .then(Commands.literal("add") // np user <username> groups add
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserGroups.Add.NoArgument(api, config, logger))
                            .then(Commands.argument("groupId", StringArgumentType.string()) // np user <username> groups add <groupId>
                                .requires(s -> checkAccess(s, api, config))
                                .executes(new NPUserGroups.Add(api, config, logger)))
                        )
                        .then(Commands.literal("remove") // np user <username> groups remove
                            .requires(s -> checkAccess(s, api, config))
                            .executes(new NPUserGroups.Remove.NoArgument(api, config, logger))
                            .then(Commands.argument("groupId", StringArgumentType.string()) // np user <username> groups remove <groupId>
                                .requires(s -> checkAccess(s, api, config))
                                .executes(new NPUserGroups.Remove(api, config, logger))))
                    )
// }
            );
        }
        LiteralArgumentBuilder<CommandSource> group = Commands.literal("group");
        {
            group.requires(s -> checkAccess(s, api, config))
                 .executes(new NPGroup(api, config, logger))

                 .then(Commands.argument("groupId", StringArgumentType.string())
                     .requires(s -> checkAccess(s, api, config))
                     .executes(new NPGroup.GroupParam(api, config, logger))

                     .then(Commands.literal("permission")
                     .requires(s -> checkAccess(s, api, config))
                     .executes(new NPGroupPermission(api, config, logger))
                         .then(Commands.literal("set")
                         .requires(s -> checkAccess(s, api, config))
                         .executes(new NPGroupPermissionSet.NoArgument(api, config, logger))
                             .then(Commands.argument("node", StringArgumentType.string())
                             .requires(s -> checkAccess(s, api, config))
                             .executes(new NPGroupPermissionSet(api, config, logger)))
                         )
                         .then(Commands.literal("unset")
                         .requires(s -> checkAccess(s, api, config))
                         .executes(new NPGroupPermissionUnSet.NoArgument(api, config, logger))
                             .then(Commands.argument("node", StringArgumentType.string())
                             .requires(s -> checkAccess(s, api, config))
                             .executes(new NPGroupPermissionUnSet(api, config, logger)))
                         )
                         .then(Commands.literal("list")
                             .requires(s -> checkAccess(s, api, config))
                             .executes(new NPGroupPermissionList(api, config, logger))
                         )
                     )

                     .then(Commands.literal("check")
                     .requires(s -> checkAccess(s, api, config))
                     .executes(new NPGroupCheck.NoArgument(api, config, logger))
                         .then(Commands.argument("node", StringArgumentType.string())
                         .requires(s -> checkAccess(s, api, config))
                         .executes(new NPGroupCheck(api, config, logger)))
                     )
                 )

                 .then(Commands.literal("create")
                     .requires(s -> checkAccess(s, api, config))
                     .executes(new NPGroupCreate.NoArgument(api, config, logger))

                     .then(Commands.argument("groupId", StringArgumentType.string())
                         .then(Commands.argument("groupName", StringArgumentType.string())
                             .requires(s -> checkAccess(s, api, config))
                             .executes(new NPGroupCreate(api, config, logger)))
                     )
                 )

                .then(Commands.literal("delete")
                    .requires(s -> checkAccess(s, api, config))
                    .executes(new NPGroupDelete.NoArgument(api, config, logger))

                    .then(Commands.argument("groupId", StringArgumentType.string())
                        .requires(s -> checkAccess(s, api, config))
                        .executes(new NPGroupDelete(api, config, logger))
                    )
                )
            ;
        }
        LiteralArgumentBuilder<CommandSource> groups = Commands.literal("groups");
        {
            groups.requires(s -> checkAccess(s, api, config))
                  .executes(new NPGroups(api, config, logger));
        }
        LiteralArgumentBuilder<CommandSource> reload = Commands.literal("reload");
        {
            reload.requires(s -> checkAccess(s, api, config))
                  .executes(new NPReloadCommand(plugin, api, config, logger));
        }
        LiteralArgumentBuilder<CommandSource> permissions = Commands.literal("permissions");
        {
            permissions.requires(s -> checkAccess(s, api, config))
                  .executes(new NPCommand.ListAllPermissions(plugin, api, config, logger));
        }

        /* Register sub-commands:
         *     np user
         *     np group
         *     np groups
         *     np reload
         *     np permissions
         */
        np.then(user);
        np.then(group);
        np.then(groups);
        np.then(reload);
        np.then(permissions);

        dispatcher.register(np);

        PermissionAPI.registerNode("minecraft.command.np", DefaultPermissionLevel.ALL, "Neko Permissions");
    }

    private static boolean checkAccess(CommandSource source, NekoPermsAPI api, Config config)
    {
        boolean isServer = source.getName().equals("Server") && source.hasPermissionLevel(4);
        if (!isServer && source.getEntity() instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            NekoUser user = api.getOrCreateUser(player.getUniqueID(), player.getName().getString());
            if (user != null && user.loadBlock())
                return user.hasPermission(config.permissions.access);
        }
        return isServer;
    }

    private static class ListAllPermissions implements Command<CommandSource>
    {
        private final NekoPerms plugin;
        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public ListAllPermissions(NekoPerms plugin, NekoPermsAPI api, Config config, Logger logger)
        {
            this.plugin = plugin;
            this.api = api;
            this.config = config;
            this.logger = logger;
        }

        @Override
        public int run(CommandContext<CommandSource> context) throws CommandSyntaxException
        {
            CommandSource source = context.getSource();
            if (!Utils.hasPermissionForCommand(source, api, config))
                return SINGLE_SUCCESS;

            StringTextComponent text = new StringTextComponent("Test");
            text.setStyle(Style.EMPTY.setColor(Color.fromHex("RED")));
            StringTextComponent text2 = new StringTextComponent(" Test2");
            text2.setStyle(Style.EMPTY.setColor(Color.fromHex("PINK")));
            source.sendFeedback(text.appendSibling(text2), true);

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms] &bListing &lALL &r&bpermission nodes:"), true);
            for (String node : plugin.getRegisteredNodes())
                source.sendFeedback(StringColorUtils.getColoredString("&2- &6" + node), true);

            return SINGLE_SUCCESS;
        }

    }
}
