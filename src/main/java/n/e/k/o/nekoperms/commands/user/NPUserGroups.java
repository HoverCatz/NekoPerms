package n.e.k.o.nekoperms.commands.user;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoGroup;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.api.NekoUser;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class NPUserGroups
{

    public static class NoArgument implements Command<CommandSource>
    {

        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public NoArgument(NekoPermsAPI api, Config config, Logger logger)
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

            String username = ctx.getArgument("username", String.class);
            NekoUser user = Utils.getNekoUserByUsername(username, api);
            if (user == null || !user.loadBlock())
            {
                source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                return SINGLE_SUCCESS;
            }

            List<NekoGroup> groups = user.getGroups();
            groups.removeIf(NekoGroup::hasBeenDeleted);

            if (groups.isEmpty())
                source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] No groups found for this user."), true);
            else
            {
                source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Groups for " + user.getUsernameOrUUID() + ":"), true);
                source.sendFeedback(StringColorUtils.getColoredString("- " + groups.stream().map(NekoGroup::getId).collect(Collectors.joining("\n- "))), true);
            }

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Groups commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> groups add <groupName>"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> groups remove <groupName>"), true);

            return SINGLE_SUCCESS;
        }

    }

    public static class Add implements Command<CommandSource>
    {

        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public Add(NekoPermsAPI api, Config config, Logger logger)
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

            String username = ctx.getArgument("username", String.class);
            NekoUser user = Utils.getNekoUserByUsername(username, api);
            if (user == null || !user.loadBlock())
            {
                source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                return SINGLE_SUCCESS;
            }

            String groupId = ctx.getArgument("groupId", String.class);
            NekoGroup group = api.getGroup(groupId);
            if (group == null)
            {
                source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' not found."), true);
                return SINGLE_SUCCESS;
            }

            List<NekoGroup> groups = user.getGroups();
            groups.removeIf(NekoGroup::hasBeenDeleted);

            if (groups.contains(group))
                source.sendFeedback(StringColorUtils.getColoredString("User is already in that group."), true);
            else
            {
                user.addGroup(group);
                source.sendFeedback(StringColorUtils.getColoredString("Added group to user, saving user data - please wait..."), true);
                user.saveAsync().thenAcceptAsync(success ->
                {
                    if (success)
                        source.sendFeedback(StringColorUtils.getColoredString("Saving succeeded :)"), true);
                    else
                        source.sendFeedback(StringColorUtils.getColoredString("Failed saving data..."), true);
                });
            }

            return SINGLE_SUCCESS;
        }

        public static class NoArgument implements Command<CommandSource>
        {

            private final NekoPermsAPI api;
            private final Config config;
            private final Logger logger;

            public NoArgument(NekoPermsAPI api, Config config, Logger logger)
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

                String username = ctx.getArgument("username", String.class);
                NekoUser user = Utils.getNekoUserByUsername(username, api);
                if (user == null || !user.loadBlock())
                {
                    source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                    return SINGLE_SUCCESS;
                }

                source.sendFeedback(StringColorUtils.getColoredString("User.Groups.Add: " + user), true);

                source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Groups Add commands:"), true);
                source.sendFeedback(StringColorUtils.getColoredString("/np user <username> groups add <groupName>"), true);

                return SINGLE_SUCCESS;
            }

        }

    }

    public static class Remove implements Command<CommandSource>
    {

        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public Remove(NekoPermsAPI api, Config config, Logger logger)
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

            String username = ctx.getArgument("username", String.class);
            NekoUser user = Utils.getNekoUserByUsername(username, api);
            if (user == null || !user.loadBlock())
            {
                source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                return SINGLE_SUCCESS;
            }

            String groupId = ctx.getArgument("groupId", String.class);
            NekoGroup group = api.getGroup(groupId);
            if (group == null)
            {
                source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' not found."), true);
                List<NekoGroup> groups = user.getGroups();
                if (groups.removeIf(NekoGroup::hasBeenDeleted))
                    source.sendFeedback(StringColorUtils.getColoredString("Found and removed non-existing user-group(s)."), true);
                return SINGLE_SUCCESS;
            }

            List<NekoGroup> groups = user.getGroups();
            groups.removeIf(NekoGroup::hasBeenDeleted);

            if (!groups.contains(group))
                source.sendFeedback(StringColorUtils.getColoredString("User is not in that group."), true);
            else
            {
                user.removeGroup(group);
                source.sendFeedback(StringColorUtils.getColoredString("Removed group from user, saving user data - please wait..."), true);
                user.saveAsync().thenAcceptAsync(success ->
                {
                    if (success)
                        source.sendFeedback(StringColorUtils.getColoredString("Saving succeeded :)"), true);
                    else
                        source.sendFeedback(StringColorUtils.getColoredString("Failed saving data..."), true);
                });
            }

            return SINGLE_SUCCESS;
        }

        public static class NoArgument implements Command<CommandSource>
        {

            private final NekoPermsAPI api;
            private final Config config;
            private final Logger logger;

            public NoArgument(NekoPermsAPI api, Config config, Logger logger)
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

                String username = ctx.getArgument("username", String.class);
                NekoUser user = Utils.getNekoUserByUsername(username, api);
                if (user == null || !user.loadBlock())
                {
                    source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                    return SINGLE_SUCCESS;
                }

                source.sendFeedback(StringColorUtils.getColoredString("User.Groups.Remove: " + user), true);

                source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Groups Remove commands:"), true);
                source.sendFeedback(StringColorUtils.getColoredString("/np user <username> groups remove <groupName>"), true);

                return SINGLE_SUCCESS;
            }

        }

    }

}
