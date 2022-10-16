package n.e.k.o.nekoperms.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.NekoPerms;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import org.apache.logging.log4j.Logger;

public class NPReloadCommand implements Command<CommandSource>
{

    private final NekoPerms plugin;
    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPReloadCommand(NekoPerms plugin, NekoPermsAPI api, Config config, Logger logger)
    {
        this.plugin = plugin;
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

        plugin.doReload();

        source.sendFeedback(StringColorUtils.getColoredString("NekoPerms reloaded."), true);

        return SINGLE_SUCCESS;
    }

}
