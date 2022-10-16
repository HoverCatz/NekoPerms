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

public class NPGroupCheck implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupCheck(NekoPermsAPI api, Config config, Logger logger)
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

        String groupId = ctx.getArgument("groupId", String.class);
        NekoGroup group = api.getGroup(groupId);
        if (group == null)
        {
            source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' not found."), true);
            return SINGLE_SUCCESS;
        }

        source.sendFeedback(StringColorUtils.getColoredString("Group: " + group), true);

        String node = ctx.getArgument("node", String.class).toLowerCase();

        boolean perms = group.hasPermission(node);
        source.sendFeedback(StringColorUtils.getColoredString("Group " + group.getId() + " has permission node '" + node + "': " + perms), true);

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

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] Check commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission check <node>"), true);

            return SINGLE_SUCCESS;
        }

    }

}
