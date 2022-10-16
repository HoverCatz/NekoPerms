package n.e.k.o.nekoperms;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.commands.NPCommand;
import n.e.k.o.nekoperms.utils.Config;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.IContext;
import n.e.k.o.nekoperms.api.NekoUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@Mod("nekoperms")
public class NekoPerms implements IPermissionHandler
{

    private static final Logger logger = LogManager.getLogger("NekoPerms");

    private IPermissionHandler superPermissionsHandler = null;

    private NekoPermsAPI api;

    private final Config config;

    public static final Map<String, String> allPermissionNodes = new HashMap<>();

    public NekoPerms()
    {
        Config config = null;
        try
        {
            config = Config.init("NekoPerms", logger);
            if (config == null)
                logger.error("Failed creating config.");
            else
            {
                // Just a test, fetching an api instance
                NekoPermsAPI.getAsync().thenAcceptAsync(api ->
                    logger.info("Got API: " + api));
                api = NekoPermsAPI.init(config, logger);

                superPermissionsHandler = PermissionAPI.getPermissionHandler();
                if (superPermissionsHandler == this)
                    superPermissionsHandler = null;
                PermissionAPI.setPermissionHandler(this);
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        this.config = config;
        if (config != null)
            MinecraftForge.EVENT_BUS.register(this);
    }

    public void doReload()
    {
        NekoPermsAPI.doReload = true;
        api.clear();
        NekoPerms.allPermissionNodes.clear();
        NekoPermsAPI.init(config, logger);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        if (api == null) return;
        try
        {
            CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
            NPCommand.register(this, dispatcher, api, config, logger);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (api == null) return;
        try
        {
            final PlayerEntity player = event.getPlayer();
            final UUID uuid = player.getUniqueID();
            final NekoUser user = api.getOrCreateUser(uuid, player.getName().getString());
            user.loadAsync();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event)
    {
        if (api == null) return;
        logger.info(config.strings.plugin_init);
        IPermissionHandler handler = PermissionAPI.getPermissionHandler();
        if (handler != this)
            PermissionAPI.setPermissionHandler(this); // Re-set handler, because something else took over
    }

    @Override
    @ParametersAreNonnullByDefault
    public void registerNode(String node, DefaultPermissionLevel level, String desc)
    {
        if (superPermissionsHandler != null)
            superPermissionsHandler.registerNode(node, level, desc);
    }

    @Nonnull
    @Override
    public Collection<String> getRegisteredNodes()
    {
        return allPermissionNodes.keySet();
    }

    @Override
    public boolean hasPermission(@Nonnull GameProfile profile, @Nonnull String node, @Nullable IContext context)
    {
        if (api == null || !profile.isComplete())
        {
            if (superPermissionsHandler != null)
                return superPermissionsHandler.hasPermission(profile, node, context);
            return false;
        }
        NekoUser user = api.getOrCreateUser(profile.getId(), profile.getName());
        if (user == null || !user.loadBlock())
        {
            if (superPermissionsHandler != null)
                return superPermissionsHandler.hasPermission(profile, node, context);
            return false;
        }
        boolean result = user.hasPermission(node);
        if (!result && superPermissionsHandler != null)
            result = superPermissionsHandler.hasPermission(profile, node, context);
        return result;
    }

    @Nonnull
    @Override
    public String getNodeDescription(@Nonnull String node)
    {
        return node;
    }

}
