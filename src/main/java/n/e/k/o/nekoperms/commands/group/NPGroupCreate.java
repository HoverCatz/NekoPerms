package n.e.k.o.nekoperms.commands.group;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoGroup;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import org.apache.logging.log4j.Logger;

public class NPGroupCreate implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupCreate(NekoPermsAPI api, Config config, Logger logger)
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

        // '/np group create <groupId> <groupName>'

        String groupId = ctx.getArgument("groupId", String.class);
        String groupName = ctx.getArgument("groupName", String.class);

        NekoGroup group = api.getGroup(groupId);
        if (group != null)
        {
            source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupName + "' already exists."), true);
            return SINGLE_SUCCESS;
        }

        group = api.getOrCreateGroup(groupId, groupName);
        group.saveAsync().thenAcceptAsync(ret ->
        {
            if (ret == 0)
                source.sendFeedback(StringColorUtils.getColoredString("Saving succeeded :)"), true);
            else
            if (ret == 1)
                // This shouldn't really happen, unless someone manually creates/moves a file with this name
                source.sendFeedback(StringColorUtils.getColoredString("Couldn't save new group, filename already exists..."), true);
            else
                source.sendFeedback(StringColorUtils.getColoredString("Failed saving group..."), true);
        });

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

            // '/np group create'

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] Create commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group create <groupId> <groupName>"), true);

            return SINGLE_SUCCESS;
        }

    }

}