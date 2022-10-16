package n.e.k.o.nekoperms.commands.group.permission;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import org.apache.logging.log4j.Logger;

public class NPGroupPermission implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupPermission(NekoPermsAPI api, Config config, Logger logger)
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

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Group] Permission commands:"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> permission set <node>"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np group <groupId> permission unset <node>"), true);

        return SINGLE_SUCCESS;
    }

}
