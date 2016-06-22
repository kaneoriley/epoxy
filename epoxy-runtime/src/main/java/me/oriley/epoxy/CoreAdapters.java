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
import android.util.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings("WeakerAccess")
final class CoreAdapters {

    static final JsonAdapter<Boolean> BOOLEAN_ADAPTER = new JsonAdapter<Boolean>() {
        @Override
        public Boolean fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return reader.nextBoolean();
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Boolean value) throws IOException {
            writer.value(value);
        }

        @Override
        public String toString() {
            return "JsonAdapter(Boolean)";
        }
    };

    static final JsonAdapter<Byte> BYTE_ADAPTER = new JsonAdapter<Byte>() {
        @Override
        @Nullable
        public Byte fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return (byte) rangeCheckNextInt(reader, "a byte", Byte.MIN_VALUE, 0xff);
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Byte value) throws IOException {
            writer.value(value.intValue() & 0xff);
        }

        @Override
        public String toString() {
            return "JsonAdapter(Byte)";
        }
    };

    static final JsonAdapter<Character> CHARACTER_ADAPTER = new JsonAdapter<Character>() {
        @Override
        public Character fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return lengthCheckNextChar(reader);
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Character value) throws IOException {
            writer.value(value.toString());
        }

        @Override
        public String toString() {
            return "JsonAdapter(Character)";
        }
    };

    static final JsonAdapter<Double> DOUBLE_ADAPTER = new JsonAdapter<Double>() {
        @Override
        public Double fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return reader.nextDouble();
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Double value) throws IOException {
            writer.value(value.doubleValue());
        }

        @Override
        public String toString() {
            return "JsonAdapter(Double)";
        }
    };

    static final JsonAdapter<Float> FLOAT_ADAPTER = new JsonAdapter<Float>() {
        @Override
        public Float fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return (float) reader.nextDouble();
            // TODO: Double check for infinity after float conversion; many doubles > Float.MAX
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Float value) throws IOException {
            // Manual null pointer check for the float.class adapter.
            if (value == null) {
                throw new NullPointerException();
            }
            // Use the Number overload so we write out float precision instead of double precision.
            writer.value(value);
        }

        @Override
        public String toString() {
            return "JsonAdapter(Float)";
        }
    };

    static final JsonAdapter<Integer> INTEGER_ADAPTER = new JsonAdapter<Integer>() {
        @Override
        public Integer fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return reader.nextInt();
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Integer value) throws IOException {
            writer.value(value.intValue());
        }

        @Override
        public String toString() {
            return "JsonAdapter(Integer)";
        }
    };

    static final JsonAdapter<Long> LONG_ADAPTER = new JsonAdapter<Long>() {
        @Override
        public Long fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return reader.nextLong();
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Long value) throws IOException {
            writer.value(value.longValue());
        }

        @Override
        public String toString() {
            return "JsonAdapter(Long)";
        }
    };

    static final JsonAdapter<Short> SHORT_ADAPTER = new JsonAdapter<Short>() {
        @Override
        public Short fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return (short) rangeCheckNextInt(reader, "a short", Short.MIN_VALUE, Short.MAX_VALUE);
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Short value) throws IOException {
            writer.value(value.intValue());
        }

        @Override
        public String toString() {
            return "JsonAdapter(Short)";
        }
    };


    static final JsonAdapter<String> STRING_ADAPTER = new JsonAdapter<String>() {
        @Override
        public String fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            return reader.nextString();
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, String value) throws IOException {
            writer.value(value);
        }

        @Override
        public String toString() {
            return "JsonAdapter(String)";
        }
    };


    private CoreAdapters() {
        throw new IllegalAccessError("no instances");
    }


    public static JsonAdapter<?> createAdapter(@NonNull Type type, @NonNull Epoxy epoxy) {
        if (type == boolean.class) return BOOLEAN_ADAPTER;
        if (type == byte.class) return BYTE_ADAPTER;
        if (type == char.class) return CHARACTER_ADAPTER;
        if (type == double.class) return DOUBLE_ADAPTER;
        if (type == float.class) return FLOAT_ADAPTER;
        if (type == int.class) return INTEGER_ADAPTER;
        if (type == long.class) return LONG_ADAPTER;
        if (type == short.class) return SHORT_ADAPTER;
        if (type == Boolean.class) return BOOLEAN_ADAPTER.nullSafe();
        if (type == Byte.class) return BYTE_ADAPTER.nullSafe();
        if (type == Character.class) return CHARACTER_ADAPTER.nullSafe();
        if (type == Double.class) return DOUBLE_ADAPTER.nullSafe();
        if (type == Float.class) return FLOAT_ADAPTER.nullSafe();
        if (type == Integer.class) return INTEGER_ADAPTER.nullSafe();
        if (type == Long.class) return LONG_ADAPTER.nullSafe();
        if (type == Short.class) return SHORT_ADAPTER.nullSafe();
        if (type == String.class) return STRING_ADAPTER.nullSafe();
        if (type == Object.class) return new ObjectJsonAdapter().nullSafe();

        Class<?> rawType = Types.getRawType(type);
        if (rawType.isEnum()) {
            //noinspection unchecked
            return enumAdapter((Class<? extends Enum>) rawType).nullSafe();
        } else if (rawType == Map.class) {
            Type[] keyAndValue = Types.mapKeyAndValueTypes(type, rawType);
            // TODO: Support non-String keys
            return new MapJsonAdapter<>(epoxy, keyAndValue[1]).nullSafe();
        } else if (rawType == List.class) {
            Type elementType = Types.collectionElementType(type, Collection.class);
            return new ListJsonAdapter<>(epoxy, elementType).nullSafe();
        }

        Type elementType = Types.arrayComponentType(type);
        if (elementType == null) {
            return null;
        }
        Class<?> elementClass = Types.getRawType(elementType);
        JsonAdapter<Object> elementAdapter = epoxy.typeAdapter(elementType);
        return new ArrayJsonAdapter(elementClass, elementAdapter).nullSafe();
    }

    @NonNull
    static <T extends Enum<T>> JsonAdapter<T> enumAdapter(@NonNull final Class<T> enumType) {
        return new JsonAdapter<T>() {
            @Override
            public T fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
                String name = reader.nextString();
                try {
                    return Enum.valueOf(enumType, name);
                } catch (IllegalArgumentException e) {
                    throw new JsonException("Expected one of "
                            + Arrays.toString(enumType.getEnumConstants()) + " but was " + name + " in " +
                            reader.toString());
                }
            }

            @Override
            public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, T value) throws IOException {
                writer.value(value.name());
            }

            @Override
            public String toString() {
                return "JsonAdapter(" + enumType.getName() + ")";
            }
        };
    }

    static final class ArrayJsonAdapter extends JsonAdapter<Object> {

        @NonNull
        private final Class<?> mElementClass;

        @NonNull
        private final JsonAdapter<Object> mElementAdapter;


        ArrayJsonAdapter(@NonNull Class<?> elementClass, @NonNull JsonAdapter<Object> elementAdapter) {
            mElementClass = elementClass;
            mElementAdapter = elementAdapter;
        }


        @Override
        @NonNull
        public Object fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            List<Object> list = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                list.add(mElementAdapter.fromJson(epoxy, reader));
            }
            reader.endArray();
            Object array = Array.newInstance(mElementClass, list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Object value) throws IOException {
            writer.beginArray();
            for (int i = 0, size = Array.getLength(value); i < size; i++) {
                mElementAdapter.toJson(epoxy, writer, Array.get(value, i));
            }
            writer.endArray();
        }
    }

    static final class ListJsonAdapter<T> extends JsonAdapter<List<T>> {

        private final JsonAdapter<T> mElementAdapter;


        ListJsonAdapter(@NonNull Epoxy epoxy, @NonNull final Type type) {
            mElementAdapter = epoxy.typeAdapter(type);
        }


        @Override
        @NonNull
        public List<T> fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            List<T> result = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                result.add(mElementAdapter.fromJson(epoxy, reader));
            }
            reader.endArray();
            return result;
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, List<T> value) throws IOException {
            writer.beginArray();
            for (T element : value) {
                mElementAdapter.toJson(epoxy, writer, element);
            }
            writer.endArray();
        }

        @Override
        public String toString() {
            return mElementAdapter + ".collection()";
        }
    }

    static final class MapJsonAdapter<V> extends JsonAdapter<Map<String, V>> {

        @NonNull
        private final JsonAdapter<V> mValueAdapter;


        MapJsonAdapter(@NonNull Epoxy epoxy, @NonNull Type valueType) {
            mValueAdapter = epoxy.typeAdapter(valueType);
        }


        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Map<String, V> map) throws IOException {
            writer.beginObject();
            for (Map.Entry<String, V> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    throw new JsonException("Map key is null");
                }
                writer.name(entry.getKey());
                mValueAdapter.toJson(epoxy, writer, entry.getValue());
            }
            writer.endObject();
        }

        @Override
        @NonNull
        public Map<String, V> fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            LinkedHashMap<String, V> result = new LinkedHashMap<>();
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                V value = mValueAdapter.fromJson(epoxy, reader);
                if (result.put(name, value) != null) {
                    throw new JsonException("Map key '" + name + "' has multiple values in " + reader.toString());
                }
            }
            reader.endObject();
            return result;
        }

        @Override
        public String toString() {
            return "MapJsonAdapter(" + mValueAdapter + ")";
        }
    }

    /**
     * This adapter is used when the declared type is {@code java.lang.Object}. Typically the runtime
     * type is something else, and when encoding JSON this delegates to the runtime type's adapter.
     * For decoding (where there is no runtime type to inspect), this uses maps and lists.
     * <p>
     * <p>This adapter needs a Epoxy instance to look up the appropriate adapter for runtime types as
     * they are encountered.
     */
    static final class ObjectJsonAdapter extends JsonAdapter<Object> {

        @Override
        public Object fromJson(@NonNull Epoxy epoxy, @NonNull JsonReader reader) throws IOException {
            switch (reader.peek()) {
                case BEGIN_ARRAY:
                    List<Object> list = new ArrayList<>();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        list.add(fromJson(epoxy, reader));
                    }
                    reader.endArray();
                    return list;

                case BEGIN_OBJECT:
                    Map<String, Object> map = new LinkedHashMap<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        map.put(reader.nextName(), fromJson(epoxy, reader));
                    }
                    reader.endObject();
                    return map;

                case STRING:
                    return reader.nextString();

                case NUMBER:
                    return reader.nextDouble();

                case BOOLEAN:
                    return reader.nextBoolean();

                case NULL:
                    reader.nextNull();
                    return null;

                default:
                    throw new IllegalStateException("Expected a value but was " + reader.peek() + " in " +
                            reader.toString());
            }
        }

        @Override
        public void toJson(@NonNull Epoxy epoxy, @NonNull JsonWriter writer, Object value) throws IOException {
            Class<?> valueClass = value.getClass();
            if (valueClass == Object.class) {
                // Don't recurse infinitely when the runtime type is also Object.class.
                writer.beginObject();
                writer.endObject();
            } else {
                epoxy.typeAdapter(toJsonType(valueClass)).toJson(epoxy, writer, value);
            }
        }

        /**
         * Returns the type to look up a type adapter for when writing {@code value} to JSON. Without
         * this, attempts to emit standard types like `LinkedHashMap` would fail because Epoxy doesn't
         * provide built-in adapters for implementation types. It knows how to <strong>write</strong>
         * those types, but lacks a mechanism to read them because it doesn't know how to find the
         * appropriate constructor.
         */
        @NonNull
        private Class<?> toJsonType(@NonNull Class<?> valueClass) {
            if (Map.class.isAssignableFrom(valueClass)) {
                return Map.class;
            } else if (Collection.class.isAssignableFrom(valueClass)) {
                return Collection.class;
            } else {
                return valueClass;
            }
        }

        @Override
        public String toString() {
            return "JsonAdapter(Object)";
        }
    }
}
