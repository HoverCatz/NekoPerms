package n.e.k.o.nekoperms.commands.user;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.utils.Config;
import n.e.k.o.nekoperms.utils.StringColorUtils;
import n.e.k.o.nekoperms.utils.Utils;
import net.minecraft.command.CommandSource;
import n.e.k.o.nekoperms.api.NekoUser;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class NPUser implements Command<CommandSource>
{

    private final NekoPermsAPI api;
    private final Config config;
    private final Logger logger;

    public NPUser(NekoPermsAPI api, Config config, Logger logger)
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

        // '/np user'

        source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] commands:"), true);
        source.sendFeedback(StringColorUtils.getColoredString("/np user <username>"), true);

        return SINGLE_SUCCESS;
    }

    public static class UsernameParam implements Command<CommandSource>
    {

        private final NekoPermsAPI api;
        private final Config config;
        private final Logger logger;

        public UsernameParam(NekoPermsAPI api, Config config, Logger logger)
        {
            this.api = api;
            this.config = config;
            this.logger = logger;
        }

        public static CompletableFuture<Suggestions> suggests()
        {
            SuggestionsBuilder suggests = new SuggestionsBuilder("", 0);
            for (String playerName : ServerLifecycleHooks.getCurrentServer().getOnlinePlayerNames())
                suggests.suggest(playerName);
            return suggests.buildFuture();
        }

        @Override
        public int run(CommandContext<CommandSource> ctx) throws CommandSyntaxException
        {
            CommandSource source = ctx.getSource();
            if (!Utils.hasPermissionForCommand(source, api, config))
                return SINGLE_SUCCESS;

            // '/np user username'

            String username = ctx.getArgument("username", String.class);
            NekoUser user = Utils.getNekoUserByUsername(username, api);
            if (user == null || !user.loadBlock())
            {
                source.sendFeedback(StringColorUtils.getColoredString("Username not found (not even using the Mojang API)."), true);
                return SINGLE_SUCCESS;
            }

            source.sendFeedback(StringColorUtils.getColoredString("User: " + user), true);

            source.sendFeedback(StringColorUtils.getColoredString("[NekoPerms User] commands:"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> info"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> groups"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> permission"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> check <node>"), true);
            source.sendFeedback(StringColorUtils.getColoredString("/np user <username> load"), true);

            return SINGLE_SUCCESS;
        }

    }

}