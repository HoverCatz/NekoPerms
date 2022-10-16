package n.e.k.o.nekoperms.utils;

import java.util.regex.Pattern;

public class PermissionHelper
{

    private static final String dot = Pattern.quote(".");

    // checkPermission( node to check for, permission from user/group )
    // checkPermission( 'dev.test', 'dev' )
    // checkPermission( 'dev.test.abc', 'dev.test' )
    // checkPermission( 'dev.*', 'dev.test' )
    public static boolean checkPermission(String node, String perm)
    {

        // At this point, 'node' and 'perm' cant be equal,
        // they have to be different.

        String[] nodeSplit = node.split(dot);
        String[] permSplit = perm.split(dot);

        int len = nodeSplit.length;
        int len2 = permSplit.length;

        if (len != len2)
            return false;

        // Semi-wildcard support (probably bugged)
        for (int i = 0; i < len; i++)
        {
            String nodePart = nodeSplit[i];
            String permPart = permSplit[i];
            if (nodePart.equals("*") || permPart.equals("*"))
                continue;
            if (!nodePart.equals(permPart))
                return false;
        }

        return true;
    }

}
