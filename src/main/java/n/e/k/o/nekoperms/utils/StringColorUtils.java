package n.e.k.o.nekoperms.utils;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;

public class StringColorUtils {

    public static IFormattableTextComponent getColoredString(String string) {
        return new StringTextComponent(string.replaceAll("&([\\da-fk-or])", "§$1"));
    }

    public static IFormattableTextComponent getColoredString(String string, char c) {
        return new StringTextComponent(string.replaceAll(c + "([\\da-fk-or])", "§$1"));
    }

}
