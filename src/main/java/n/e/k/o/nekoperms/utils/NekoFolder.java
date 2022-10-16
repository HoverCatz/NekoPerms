package n.e.k.o.nekoperms.utils;

import java.io.File;

public class NekoFolder
{

    private static final String folderName = "Neko";

    public static File getOrCreateConfigFolder(String modName)
    {
        File folder = new File("./config/" + folderName + "/", modName);
        if (folder.exists()) return folder;
        if (!folder.mkdirs()) return null;
        return folder;
    }

}
