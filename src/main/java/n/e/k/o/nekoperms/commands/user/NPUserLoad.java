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

public class NPUserLoad implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPUserLoad(NekoPermsAPI api, Config config, Logger logger)
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

        source.sendFeedback(StringColorUtils.getColoredString("Re-loading user data for " + user.getUsernameOrUUID() + " - please wait..."), true);
        user.loadAsync(true).thenAcceptAsync(success ->
        {
            if (success)
                source.sendFeedback(StringColorUtils.getColoredString("User data re-loaded."), true);
            else
                source.sendFeedback(StringColorUtils.getColoredString("Failed re-loading user data..."), true);
        });

        return SINGLE_SUCCESS;
    }

}
