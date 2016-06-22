/*
 * Copyright (C) 2014 Square, Inc.
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.oriley.epoxy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import java.io.IOException;

/**
 * Converts Java values to JSON, and JSON values to Java.
 */
@SuppressWarnings("WeakerAccess")
public abstract class JsonAdapter<T> {

    static final String ERROR_FORMAT = "Expected %s but was %s in %s";
    static final String CLASS_SUFFIX = "$$JsonAdapter";


    @Nullable
    public abstract T fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException;

    public abstract void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, @Nullable T value) throws IOException;

    /**
     * Returns a JSON adapter equal to this JSON adapter, but with support for reading and writing
     * nulls.
     */
    public final JsonAdapter<T> nullSafe() {
        final JsonAdapter<T> delegate = this;
        return new JsonAdapter<T>() {
            @Override
            @Nullable
            public T fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    return null;
                } else {
                    return delegate.fromJson(epoxy, reader);
                }
            }

            @Override
            public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, @Nullable T value) throws IOException {
                if (value == null) {
                    writer.nullValue();
                } else {
                    delegate.toJson(epoxy, writer, value);
                }
            }

            @Override
            public String toString() {
                return delegate + ".nullSafe()";
            }
        };
    }

    protected static int rangeCheckNextInt(@NonNull JsonReader reader, @NonNull String typeMessage, int min, int max)
            throws IOException {
        int value = reader.nextInt();
        if (value < min || value > max) {
            throw new JsonException(String.format(ERROR_FORMAT, typeMessage, value, reader.toString()));
        }
        return value;
    }

    protected static char lengthCheckNextChar(@NonNull JsonReader reader) throws IOException {
        String value = reader.nextString();
        if (value.length() > 1) {
            throw new JsonException(String.format(ERROR_FORMAT, "a char", '"' + value + '"', reader.toString()));
        }
        return value.charAt(0);
    }

    protected static boolean handleNull(@NonNull JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return true;
        } else {
            return false;
        }
    }
}
