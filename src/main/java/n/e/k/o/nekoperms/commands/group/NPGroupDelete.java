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

public class NPGroupDelete implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupDelete(NekoPermsAPI api, Config config, Logger logger)
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

        // '/np group delete <groupId>'

        String groupId = ctx.getArgument("groupId", String.class);

        NekoGroup group = api.getGroup(groupId);
        if (group == null)
        {
            source.sendFeedback(StringColorUtils.getColoredString("Group '" + groupId + "' doesn't exists."), true);
            return SINGLE_SUCCESS;
        }

        api.deleteGroup(group);
        group.saveAsync().thenAcceptAsync(ret ->
        {
            if (ret == 0)
                source.sendFeedback(StringColorUtils.getColoredString("Deletion succeeded :)"), true);
            else
            if (ret == 1)
                // This shouldn't really happen, unless someone manually creates/moves a file with this name
                source.sendFeedback(StringColorUtils.getColoredString("Couldn't delete group..."), true);
            else
                source.sendFeedback(StringColorUtils.getColoredString("Failed deleting group..."), true);
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

            // '/np group delete'

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] Delete commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np group delete <groupId>"), true);

            return SINGLE_SUCCESS;
        }

    }

}