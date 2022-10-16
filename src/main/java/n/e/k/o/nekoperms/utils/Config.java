package n.e.k.o.nekoperms.utils;

import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Config
{

    public transient File userdataFolder;
    public transient File groupsFolder;

    public Strings strings;
    public Permissions permissions;

    public static class Strings
    {
        public String plugin_init;
        public String user_not_loaded_yet;
        public String no_permission;
    }

    public static class Permissions
    {
        public String access;
    }

    public static Config init(String modName, Logger logger)
    {
        File folder = NekoFolder.getOrCreateConfigFolder(modName);
        if (folder == null)
            return null;
        try
        {
            File configFile = new File(folder, "config.json");
            if (!configFile.exists())
                try (InputStream is = Config.class.getResourceAsStream("/config.json"))
                {
                    assert is != null;
                    Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            Config config = new Gson().fromJson(new FileReader(configFile), Config.class);
            config.userdataFolder = new File(folder, "userdata");
            if (!config.userdataFolder.exists() && !config.userdataFolder.mkdirs())
                return null;
            config.groupsFolder = new File(folder, "groups");
            if (!config.groupsFolder.exists() && !config.groupsFolder.mkdirs())
                return null;
            return config;
        }
        catch (Throwable t)
        {
            logger.error(t);
            t.printStackTrace();
        }
        return null;
    }

}