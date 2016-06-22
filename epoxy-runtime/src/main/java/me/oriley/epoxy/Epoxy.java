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

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class Epoxy {

    @NonNull
    private final Map<Type, JsonAdapter<?>> mAdapterCache = new LinkedHashMap<>();


    @SuppressWarnings("unchecked")
    public <T> T fromJson(@NonNull JsonReader reader, @NonNull Type type) throws IOException {
        return (T) typeAdapter(type).fromJson(this, reader);
    }

    public <T> T fromJson(@NonNull Reader source, @NonNull Class<T> c) throws IOException {
        return fromJson(new JsonReader(source), c);
    }

    public <T> T fromJson(@NonNull String string, @NonNull Class<T> c) throws IOException {
        return fromJson(new StringReader(string), c);
    }

    public <T> void toJson(@NonNull JsonWriter writer, @NonNull T value, @NonNull Type type) throws IOException {
        typeAdapter(type).toJson(this, writer, value);
    }

    public <T> void toJson(@NonNull Writer sink, @NonNull T value, @NonNull Class<T> c) throws IOException {
        JsonWriter writer = new JsonWriter(sink);
        toJson(writer, value, c);
    }

    @NonNull
    public <T> String toJson(@NonNull T value, @NonNull Class<T> c) throws IOException {
        StringWriter writer = new StringWriter();
        toJson(writer, value, c);
        return writer.getBuffer().toString();
    }

    @NonNull
    @SuppressWarnings("unchecked")
    <T> JsonAdapter<T> typeAdapter(@NonNull Type type) {
        JsonAdapter result;

        synchronized (mAdapterCache) {
            result = mAdapterCache.get(type);
            if (result != null) {
                return (JsonAdapter<T>) result;
            }
        }

        if (type instanceof Class) {
            Class<?> typeClass = (Class) type;
            try {
                Class<?> adapterClass = Class.forName(typeClass.getName() + JsonAdapter.CLASS_SUFFIX);
                //noinspection unchecked
                result = (JsonAdapter) adapterClass.newInstance();
            } catch (Exception e) {
                // Ignore, move along
            }
        }

        if (result == null) {
            // Attempt to find one in the core factory
            result = CoreAdapters.createAdapter(type, this);
        }

        if (result != null) {
            synchronized (mAdapterCache) {
                mAdapterCache.put(type, result);
            }
            return (JsonAdapter<T>) result;
        }

        throw new IllegalArgumentException("No JsonAdapter for " + type);
    }
}
