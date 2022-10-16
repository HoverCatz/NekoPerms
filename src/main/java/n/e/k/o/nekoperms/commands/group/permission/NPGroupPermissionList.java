package n.e.k.o.nekoperms.commands.group.permission;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.api.NekoGroup;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class NPGroupPermissionList implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPGroupPermissionList(NekoPermsAPI api, Config config, Logger logger)
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

        source.sendFeedback(StringColorUtils.getColoredString("NPGroupPermissionList. Group: " + group.getName() + "."), true);
        source.sendFeedback(StringColorUtils.getColoredString("&bListing &lALL &r&bpermission nodes for group:"), true);

        List<String> nodes = group.getAllPermissionNodes();
        for (String node : nodes)
            source.sendFeedback(StringColorUtils.getColoredString("&2- &6" + node), true);

        return SINGLE_SUCCESS;
    }

}
