/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.oriley.epoxy;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class EpoxyJsonBinder<T> {

    @Nullable
    protected abstract T fromJson(@NonNull JSONObject jsonObject) throws IOException, JSONException;

    @CallSuper
    protected void parseJson(@NonNull T t, @NonNull JSONObject jsonObject) throws IOException, JSONException {
    }

    protected static boolean parseBoolean(@NonNull JSONObject jsonObject,
                                          @NonNull String name,
                                          boolean optional) throws JSONException {
        return optional ? jsonObject.optBoolean(name) : jsonObject.getBoolean(name);
    }

    @Nullable
    protected static boolean[] parseBooleanArray(@NonNull JSONObject jsonObject,
                                                 @NonNull String name,
                                                 boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        boolean[] array = new boolean[length];

        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getBoolean(i);
        }
        return array;
    }

    protected static int parseInteger(@NonNull JSONObject jsonObject,
                                      @NonNull String name,
                                      boolean optional) throws JSONException {
        return optional ? jsonObject.optInt(name) : jsonObject.getInt(name);
    }

    @Nullable
    protected static int[] parseIntegerArray(@NonNull JSONObject jsonObject,
                                             @NonNull String name,
                                             boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        int[] array = new int[length];

        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getInt(i);
        }
        return array;
    }

    protected static short parseShort(@NonNull JSONObject jsonObject,
                                      @NonNull String name,
                                      boolean optional) throws JSONException {
        return (short) parseInteger(jsonObject, name, optional);
    }

    @Nullable
    protected static short[] parseShortArray(@NonNull JSONObject jsonObject,
                                             @NonNull String name,
                                             boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        short[] array = new short[length];

        for (int i = 0; i < length; i++) {
            array[i] = (short) jsonArray.getInt(i);
        }
        return array;
    }

    protected static long parseLong(@NonNull JSONObject jsonObject,
                                    @NonNull String name,
                                    boolean optional) throws JSONException {
        return optional ? jsonObject.optLong(name) : jsonObject.getLong(name);
    }

    @Nullable
    protected static long[] parseLongArray(@NonNull JSONObject jsonObject,
                                           @NonNull String name,
                                           boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        long[] array = new long[length];

        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getLong(i);
        }
        return array;
    }

    protected static float parseFloat(@NonNull JSONObject jsonObject,
                                      @NonNull String name,
                                      boolean optional) throws JSONException {
        return (float) parseDouble(jsonObject, name, optional);
    }

    @Nullable
    protected static float[] parseFloatArray(@NonNull JSONObject jsonObject,
                                             @NonNull String name,
                                             boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        float[] array = new float[length];

        for (int i = 0; i < length; i++) {
            array[i] = (float) jsonArray.getDouble(i);
        }
        return array;
    }

    protected static double parseDouble(@NonNull JSONObject jsonObject,
                                        @NonNull String name,
                                        boolean optional) throws JSONException {
        return optional ? jsonObject.optDouble(name) : jsonObject.getDouble(name);
    }

    @Nullable
    protected static double[] parseDoubleArray(@NonNull JSONObject jsonObject,
                                               @NonNull String name,
                                               boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        double[] array = new double[length];

        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getDouble(i);
        }
        return array;
    }

    @Nullable
    protected static String parseString(@NonNull JSONObject jsonObject,
                                        @NonNull String name,
                                        boolean optional) throws JSONException {
        return optional ? jsonObject.optString(name) : jsonObject.getString(name);
    }

    @Nullable
    protected static String[] parseStringArray(@NonNull JSONObject jsonObject,
                                               @NonNull String name,
                                               boolean optional) throws JSONException {
        JSONArray jsonArray = optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
        if (jsonArray == null) {
            return null;
        }

        int length = jsonArray.length();
        String[] array = new String[length];

        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getString(i);
        }
        return array;
    }

    @Nullable
    protected static JSONObject getObject(@NonNull JSONObject jsonObject,
                                          @NonNull String name,
                                          boolean optional) throws JSONException {
        return optional ? jsonObject.optJSONObject(name) : jsonObject.getJSONObject(name);
    }

    @Nullable
    protected static JSONArray getArray(@NonNull JSONObject jsonObject,
                                        @NonNull String name,
                                        boolean optional) throws JSONException {
        return optional ? jsonObject.optJSONArray(name) : jsonObject.getJSONArray(name);
    }
}