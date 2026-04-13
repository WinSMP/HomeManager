package org.winlogon.homemanager.importexport.formats;

import java.util.Map;

public final class HomeFormatUtils {

    private HomeFormatUtils() {}

    public static double getNumber(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        return defaultValue;
    }

    public static float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number num) {
            return num.floatValue();
        }
        return defaultValue;
    }
}
