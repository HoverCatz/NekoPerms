package n.e.k.o.nekoperms.commands.group.permission;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoGroup;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import org.apache.logging.log4j.Logger;

public class NPGroupPermissionUnSet implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupPermissionUnSet(NekoPermsAPI api, Config config, Logger logger)
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
        boolean hasNode = group.hasPermission(node);
        if (!hasNode)
        {
            source.sendFeedback(StringColorUtils.getColoredString("NPGroupPermissionUnSet. Group: " + group.getName() + ", node: '" + node + "', hasNode: false"), true);
            return SINGLE_SUCCESS;
        }

        group.removePermission(node);

        source.sendFeedback(StringColorUtils.getColoredString("NPGroupPermissionUnSet. Group: " + group.getName() + ", node: '" + node + "'."), true);
        source.sendFeedback(StringColorUtils.getColoredString("Removed permission node, saving group data - please wait..."), true);
        group.saveAsync().thenAcceptAsync(ret ->
        {
            if (ret == 0)
                source.sendFeedback(StringColorUtils.getColoredString("Saving succeeded :)"), true);
            else
                source.sendFeedback(StringColorUtils.getColoredString("Failed saving data..."), true);
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

            String groupId = ctx.getArgument("groupId", String.class);
            NekoGroup group = api.getGroup(groupId);
            if (group == null)
            {
                source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' not found."), true);
                return SINGLE_SUCCESS;
            }

            source.sendFeedback(StringColorUtils.getColoredString("Group: " + group), true);

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] Permission UnSet commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> permission unset <node>"), true);

            return SINGLE_SUCCESS;
        }

    }

}
