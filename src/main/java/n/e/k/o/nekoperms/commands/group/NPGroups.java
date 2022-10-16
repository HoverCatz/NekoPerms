package n.e.k.o.nekoperms.commands.group;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class NPGroups implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroups(NekoPermsAPI api, Config config, Logger logger)
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

        // '/np groups'

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms Groups]:"), true);

        Map<String, String> groups = api.getGroups();
        if (groups.size() == 0)
            source.sendFeedback(StringColorUtils.getColoredString("No groups yet."), true);
        else
            for (Map.Entry<String, String> entry : groups.entrySet())
                source.sendFeedback(StringColorUtils.getColoredString("- " + entry.getKey() + " -> " + entry.getValue()), true);

        return SINGLE_SUCCESS;
    }

}