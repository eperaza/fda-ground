package com.boeing.cas.supa.ground.utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonUtils {
    public static String getJsonStringProp(JsonObject obj, String propName) {
        JsonElement element = obj.get(propName);
        if (element != null) {
            return element.getAsString();
        }

        return "";
    }

    public static long getJsonLongProp(JsonObject obj, String propName) {
        JsonElement element = obj.get(propName);
        if (element != null) {
            return element.getAsLong();
        }

        return Long.MIN_VALUE;
    }

    public static String getAsString(JsonElement element) {
        if(element != null) {
            return element.getAsString();
        }

        return "";
    }
}