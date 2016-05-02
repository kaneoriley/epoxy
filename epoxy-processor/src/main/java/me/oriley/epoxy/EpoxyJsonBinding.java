/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.oriley.epoxy;

import com.squareup.javapoet.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;
import static me.oriley.epoxy.EpoxyProcessor.*;

@SuppressWarnings("WeakerAccess")
final class EpoxyJsonBinding {

    private static final String BOX = "[]";
    private static final String BINDING_CLASS_SUFFIX = "$$EpoxyBinder";
    private static final String PARSE_JSON = "parseJson";
    private static final String PARSE_MODEL = "parseModel";

    private static final String STRING_CLASS = "java.lang.String";
    private static final String LIST_CLASS = "java.util.List";

    private static final String PARSE_STRING_ARRAY = "parseStringArray";
    private static final String PARSE_SHORT_ARRAY = "parseShortArray";
    private static final String PARSE_LONG_ARRAY = "parseLongArray";
    private static final String PARSE_FLOAT_ARRAY = "parseFloatArray";
    private static final String PARSE_DOUBLE_ARRAY = "parseDoubleArray";
    private static final String PARSE_INT_ARRAY = "parseIntegerArray";
    private static final String PARSE_BOOLEAN_ARRAY = "parseBooleanArray";

    private static final String PARSE_STRING = "parseString";
    private static final String PARSE_SHORT = "parseShort";
    private static final String PARSE_LONG = "parseLong";
    private static final String PARSE_FLOAT = "parseFloat";
    private static final String PARSE_DOUBLE = "parseDouble";
    private static final String PARSE_INTEGER = "parseInteger";
    private static final String PARSE_BOOLEAN = "parseBoolean";

    private static final String PARSE_MULTI_ARRAY = "parseMultiDimensionalArray";

    private static final String PUT_BOOLEAN = "putBoolean";
    private static final String PUT_DOUBLE = "putDouble";
    private static final String PUT_INTEGER = "putInteger";
    private static final String PUT_LONG = "putLong";
    private static final String PUT_OBJECT = "putObject";

    private static final String GET_OBJECT = "getObject";
    private static final String GET_ARRAY = "getArray";

    @Nonnull
    private final List<BindingElement> mElementList = new ArrayList<>();

    @Nonnull
    public final String packageName;

    @Nonnull
    public final String className;

    @Nullable
    private EpoxyJsonBinding parentBinding;

    EpoxyJsonBinding(@Nonnull String packageName, @Nonnull String className) {
        this.packageName = packageName;
        this.className = className;
    }


    void addElement(@Nonnull Element element) {
        mElementList.add(new BindingElement(element));
    }

    void setParentBinding(@Nonnull EpoxyJsonBinding parentBinding) {
        this.parentBinding = parentBinding;
    }

    @Nonnull
    TypeName getSuperClassName() {
        if (parentBinding != null) {
            return ParameterizedTypeName.get(parentBinding.getBindingClassName(), T);
        } else {
            return ParameterizedTypeName.get(EPOXY_JSON_BINDER, T);
        }
    }

    @Nonnull
    ClassName getBindingClassName() {
        return ClassName.get(packageName, className + BINDING_CLASS_SUFFIX);
    }

    @Nonnull
    ClassName getObjectClassName() {
        return ClassName.get(packageName, className);
    }

    @Nonnull
    MethodSpec createFromJsonMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(FROM_JSON)
                .returns(T)
                .addAnnotation(CALL_SUPER)
                .addAnnotation(NULLABLE)
                .addParameter(ParameterSpec.builder(JSON_OBJECT_CLASS, JSON_OBJECT).addAnnotation(NONNULL).build())
                .addModifiers(PUBLIC)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS);

        return builder.addStatement("$T $L = ($T) new $L()", T, MODEL, T, className)
                .addStatement("$L($L, $L)", PARSE_JSON, MODEL, JSON_OBJECT)
                .addStatement("return $L", MODEL).build();
    }

    @Nonnull
    MethodSpec createParseJsonMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(PARSE_JSON)
                .addAnnotation(CALL_SUPER)
                .addAnnotation(NULLABLE)
                .addParameter(ParameterSpec.builder(T, MODEL).addAnnotation(NONNULL).build())
                .addParameter(ParameterSpec.builder(JSON_OBJECT_CLASS, JSON_OBJECT).addAnnotation(NONNULL).build())
                .addModifiers(PUBLIC)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addStatement("super.$L($L, $L)", PARSE_JSON, MODEL, JSON_OBJECT);

        for (BindingElement element : mElementList) {
            if (element.dimensions > 1) {
                builder.addStatement("$L.$L = $L($L, $S, $L.class, $L)", MODEL, element.fieldName, element.getMethod, JSON_OBJECT,
                        element.jsonName, element.getArrayElementName(), element.isOptional);
            } else if (element.fromEpoxy) {
                String epoxyMethod = element.isList ? LIST_FROM_JSON : FROM_JSON;
                builder.addStatement("$L.$L = $T.$L($L($L, $S, $L), $L.class)", MODEL, element.fieldName, EPOXY_JSON, epoxyMethod,
                        element.getMethod, JSON_OBJECT, element.jsonName, element.isOptional, element.className);
            } else {
                builder.addStatement("$L.$L = $L($L, $S, $L)", MODEL, element.fieldName, element.getMethod, JSON_OBJECT,
                        element.jsonName, element.isOptional);
            }
        }

        return builder.build();
    }

    @Nonnull
    MethodSpec createToJsonMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(TO_JSON)
                .returns(JSON_OBJECT_CLASS)
                .addAnnotation(CALL_SUPER)
                .addAnnotation(NULLABLE)
                .addParameter(ParameterSpec.builder(T, MODEL).addAnnotation(NONNULL).build())
                .addModifiers(PUBLIC)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS);

        return builder.addStatement("$T $L = new $T()", JSON_OBJECT_CLASS, JSON_OBJECT, JSON_OBJECT_CLASS)
                .addStatement("$L($L, $L)", PARSE_MODEL, JSON_OBJECT, MODEL)
                .addStatement("return $L", JSON_OBJECT).build();
    }

    @Nonnull
    MethodSpec createParseModelMethod() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(PARSE_MODEL)
                .addAnnotation(CALL_SUPER)
                .addAnnotation(NULLABLE)
                .addParameter(ParameterSpec.builder(JSON_OBJECT_CLASS, JSON_OBJECT).addAnnotation(NONNULL).build())
                .addParameter(ParameterSpec.builder(T, MODEL).addAnnotation(NONNULL).build())
                .addModifiers(PUBLIC)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addStatement("super.$L($L, $L)", PARSE_MODEL, JSON_OBJECT, MODEL);

        for (BindingElement element : mElementList) {
            if (element.isPrimitive) {
                builder.addStatement("$L($L, $S, $L.$L)", element.putMethod, JSON_OBJECT, element.jsonName, MODEL,
                        element.fieldName);
            } else {
                builder.addStatement("$L($L, $S, $L.$L, $L)", element.putMethod, JSON_OBJECT, element.jsonName, MODEL,
                        element.fieldName, element.isOptional);
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "Class: " + className + ", Elements: " + mElementList;
    }

    private static final class BindingElement {

        @Nonnull
        public final String fieldName;

        @Nonnull
        public final String className;

        @Nonnull
        public final String jsonName;

        @Nonnull
        public final String getMethod;

        @Nonnull
        public final String putMethod;

        public final int dimensions;

        public final boolean isOptional;

        public final boolean isPrimitive;

        public final boolean isList;

        public final boolean fromEpoxy;

        BindingElement(@Nonnull Element element) {
            fieldName = element.getSimpleName().toString();
            TypeMirror elementType = element.asType();
            String className = elementType.toString();
            jsonName = element.getAnnotation(JsonField.class).value();
            isOptional = EpoxyUtils.isOptional(element);

            TypeKind elementKind = element.asType().getKind();
            boolean fromEpoxy = false;
            boolean isList = false;
            boolean isPrimitive = false;
            int dimensions = 0;

            switch (elementKind) {
                case BOOLEAN:
                    isPrimitive = true;
                    getMethod = PARSE_BOOLEAN;
                    putMethod = PUT_BOOLEAN;
                    break;
                case INT:
                    isPrimitive = true;
                    getMethod = PARSE_INTEGER;
                    putMethod = PUT_INTEGER;
                    break;
                case LONG:
                    isPrimitive = true;
                    getMethod = PARSE_LONG;
                    putMethod = PUT_LONG;
                    break;
                case DOUBLE:
                    isPrimitive = true;
                    getMethod = PARSE_DOUBLE;
                    putMethod = PUT_DOUBLE;
                    break;
                case SHORT:
                    isPrimitive = true;
                    getMethod = PARSE_SHORT;
                    putMethod = PUT_INTEGER;
                    break;
                case FLOAT:
                    isPrimitive = true;
                    getMethod = PARSE_FLOAT;
                    putMethod = PUT_DOUBLE;
                    break;
                case ARRAY:
                    putMethod = PUT_OBJECT;
                    TypeKind arrayKind = ((ArrayType) element.asType()).getComponentType().getKind();

                    int i = 0;
                    while (i < className.length()) {
                        int j = className.indexOf(BOX, i);
                        if (j > 0) {
                            dimensions++;
                            i = j + 1;
                        } else {
                            break;
                        }
                    }

                    className = className.replaceAll("\\[\\]", "");

                    switch (arrayKind) {
                        case BOOLEAN:
                            getMethod = PARSE_BOOLEAN_ARRAY;
                            break;
                        case INT:
                            getMethod = PARSE_INT_ARRAY;
                            break;
                        case LONG:
                            getMethod = PARSE_LONG_ARRAY;
                            break;
                        case DOUBLE:
                            getMethod = PARSE_DOUBLE_ARRAY;
                            break;
                        case SHORT:
                            getMethod = PARSE_SHORT_ARRAY;
                            break;
                        case FLOAT:
                            getMethod = PARSE_FLOAT_ARRAY;
                            break;
                        case ARRAY:
                            // TODO: Fix for primitive and custom object multidimen arrays?
                            getMethod = PARSE_MULTI_ARRAY;
                            break;
                        case DECLARED:
                            if (STRING_CLASS.equals(className)) {
                                getMethod = PARSE_STRING_ARRAY;
                            } else {
                                getMethod = GET_ARRAY;
                                fromEpoxy = true;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid array element passed: " + element + ", type: " + arrayKind);
                    }
                    break;
                case DECLARED:
                    putMethod = PUT_OBJECT;
                    if (className.equals(STRING_CLASS)) {
                        getMethod = PARSE_STRING;
                    } else if (className.contains(LIST_CLASS)) {
                        // TODO: Fix list handling
//                        className = className.replaceAll(".*<", "").replaceAll(">.*", "");
                        getMethod = GET_ARRAY;
                        isList = true;
                        fromEpoxy = true;
                    } else {
                        getMethod = GET_OBJECT;
                        fromEpoxy = true;
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid element passed: " + element + ", type: " + elementKind);
            }

            this.className = className;
            this.fromEpoxy = fromEpoxy;
            this.isList = isList;
            this.isPrimitive = isPrimitive;
            this.dimensions = dimensions;
        }

        @Nonnull
        public String getArrayElementName() {
            String elementClassName = this.className;
            for (int i = 1; i < dimensions; i++) {
                elementClassName += BOX;
            }
            return elementClassName;
        }

        @Override
        public String toString() {
            return "Field: " + fieldName +
                    ", Json: " + jsonName +
                    ", Optional: " + isOptional +
                    ", Getter: " + getMethod +
                    ", Putter: " + putMethod;
        }
    }
}
