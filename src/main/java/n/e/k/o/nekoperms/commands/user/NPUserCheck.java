package n.e.k.o.nekoperms.commands.user;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.api.NekoUser;
import org.apache.logging.log4j.Logger;

public class NPUserCheck implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPUserCheck(NekoPermsAPI api, Config config, Logger logger)
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

        String node = ctx.getArgument("node", String.class).toLowerCase();

        boolean perms = user.hasPermission(node);
        source.sendFeedback(StringColorUtils.getColoredString("User " + user.getUsernameOrUUID() + " has permission node '" + node + "': " + perms), true);

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

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Check commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission check <node>"), true);

            return SINGLE_SUCCESS;
        }

    }

}
