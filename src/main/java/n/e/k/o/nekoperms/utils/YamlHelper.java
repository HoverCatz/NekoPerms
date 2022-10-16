package n.e.k.o.nekoperms.utils;

import java.util.Map;

public class YamlHelper
{

    public static void getKeys(Map<?, ?> map, Map<String, Object> configMap, String previousKey)
    {
        for (Object o : map.keySet())
        {
            Object val = map.get(o);
            String key = previousKey.isEmpty() ? o.toString() : (previousKey + '.') + o.toString();
            if (val instanceof String)
                configMap.put(key, val);
            else
            if (val instanceof Map)
                getKeys((Map<?, ?>) val, configMap, key);
        }
    }

}
