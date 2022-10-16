package n.e.k.o.nekoperms.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import n.e.k.o.nekoperms.api.NekoPermsAPI;
import n.e.k.o.nekoperms.api.NekoUser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class Utils
{

    private static final Pattern validUserPattern = Pattern.compile("^\\w{2,16}$");
    private static final Map<String, UUID> usersUUIDCache = new HashMap<>();
    private static final Map<UUID, String> uuidUsersCache = new HashMap<>();

    public static Duo<UUID, String> getSecureUserdata(String name)
    {
        if (!validUserPattern.matcher(name).matches())
            return null;

        final String lowercase = name.toLowerCase();
        if (usersUUIDCache.containsKey(lowercase))
            return new Duo<>(usersUUIDCache.get(lowercase), name);

        Duo<UUID, String> duo;

        GameProfile profile = ServerLifecycleHooks.getCurrentServer().getPlayerProfileCache().getGameProfileForUsername(name);
        if (profile != null && profile.isComplete())
            duo = new Duo<>(profile.getId(), profile.getName());
        else
            duo = fetchFromAPI(name);

        if (duo != null)
            usersUUIDCache.put(duo.second.toLowerCase(), duo.first);

        return duo;
    }

    public static Duo<UUID, String> fetchFromAPI(String name)
    {
        if (!validUserPattern.matcher(name).matches())
            return null;
        try
        {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + name;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setReadTimeout(5000);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line);
            bufferedReader.close();

            String result = stringBuilder.toString();
            if (!result.startsWith("{"))
                return null;

            String find = "\"name\":\"";
            int index = result.lastIndexOf(find);
            if (index == -1)
                return null;

            result = result.substring(index + find.length());
            index = result.indexOf("\"");
            if (index == -1)
                return null;

            String strUsername = result.substring(0, index);
            result = result.substring(index + 1);

            find = "\"id\":\"";
            index = result.indexOf(find);
            if (index == -1)
                return null;

            result = result.substring(index + find.length());
            index = result.indexOf("\"");
            if (index == -1)
                return null;

            String strUUID = result.substring(0, index).replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5");

            return new Duo<>(UUID.fromString(strUUID), strUsername);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }

    public static String fetchFromAPI(UUID uuid)
    {
        if (uuidUsersCache.containsKey(uuid))
            return uuidUsersCache.get(uuid);
        try
        {
            String url = "https://api.mojang.com/user/profiles/" + uuid.toString() + "/names";

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setReadTimeout(5000);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line);
            bufferedReader.close();

            String result = stringBuilder.toString();
            if (!result.startsWith("{"))
                return null;

            String find = "\"name\":\"";
            int index = result.lastIndexOf(find);
            if (index == -1)
                return null;

            result = result.substring(index + find.length());
            index = result.indexOf("\"");
            if (index == -1)
                return null;

            String username = result.substring(0, index);
            uuidUsersCache.put(uuid, username);
            return username;
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return null;
    }

    public static class Duo<T, K>
    {
        public T first;
        public K second;
        public Duo(T first, K second)
        {
            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "Duo{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }
    }

    public static NekoUser getNekoUserByUsername(String username, NekoPermsAPI api)
    {
        Duo<UUID, String> duo = getSecureUserdata(username);
        if (duo == null)
            return null;
        return api.getOrCreateUser(duo.first, duo.second);
    }

    public static boolean hasPermissionForCommand(CommandSource source, NekoPermsAPI api, Config config) throws CommandSyntaxException
    {
        if (!(source.getEntity() instanceof ServerPlayerEntity))
            return true;

        ServerPlayerEntity player = source.asPlayer();
        boolean hasPermission = player.hasPermissionLevel(4);
        if (hasPermission)
            return true;
        NekoUser user = api.getOrCreateUser(player.getUniqueID());
        if (!user.loadBlock())
        {
            source.sendFeedback(StringColorUtils.getColoredString(config.strings.user_not_loaded_yet), true);
            return false;
        }
        hasPermission = user.hasPermission(config.permissions.access);
        if (!hasPermission)
        {
            source.sendFeedback(StringColorUtils.getColoredString(config.strings.no_permission), true);
            return false;
        }

        return true;
    }

}
