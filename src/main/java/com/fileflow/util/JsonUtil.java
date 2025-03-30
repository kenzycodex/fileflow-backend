package com.fileflow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>)
                    (localDateTime, type, jsonSerializationContext) ->
                            new com.google.gson.JsonPrimitive(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}