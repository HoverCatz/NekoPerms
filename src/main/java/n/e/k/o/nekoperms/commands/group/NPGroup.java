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

public class NPGroup implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroup(NekoPermsAPI api, Config config, Logger logger)
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

        // '/np group'

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] commands:"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId>"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np group create <groupId> <groupName>"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np group delete <groupId>"), true);

        return SINGLE_SUCCESS;
    }

    public static class GroupParam implements Command<CommandSource>
    {

        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public GroupParam(NekoPermsAPI api, Config config, Logger logger)
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

            // '/np group groupName'

            String groupId = ctx.getArgument("groupId", String.class);

            NekoGroup group = api.getGroup(groupId);
            if (group == null)
            {
                source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' not found."), true);
                return SINGLE_SUCCESS;
            }

            source.sendFeedback(StringColorUtils.getColoredString("Group: " + group), true);

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> info"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> permission"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> check <node>"), true);

            return SINGLE_SUCCESS;
        }

    }

}