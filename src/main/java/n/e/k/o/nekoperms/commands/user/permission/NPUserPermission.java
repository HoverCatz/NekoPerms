package n.e.k.o.nekoperms.commands.user.permission;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import org.apache.logging.log4j.Logger;

public class NPUserPermission implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPUserPermission(NekoPermsAPI api, Config config, Logger logger)
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

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] Permission commands:"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission set <node>"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission unset <node>"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission list"), true);

        return SINGLE_SUCCESS;
    }

}
