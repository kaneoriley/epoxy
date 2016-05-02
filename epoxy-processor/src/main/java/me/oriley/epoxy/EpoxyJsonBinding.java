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

    private static final String BINDING_CLASS_SUFFIX = "$$EpoxyBinder";
    private static final String TO_JSON = "toJson";
    private static final String PARSE_JSON = "parseJson";
    private static final String MODEL = "model";

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
    private static final String PARSE_INT = "parseInteger";
    private static final String PARSE_BOOLEAN = "parseBoolean";

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
            if (element.fromEpoxy) {
                String epoxyMethod = element.isList ? LIST_FROM_JSON : FROM_JSON;
                builder.addStatement("$L.$L = $T.$L($L($L, $S, $L), $L.class)", MODEL, element.fieldName, EPOXY_JSON, epoxyMethod,
                        element.method, JSON_OBJECT, element.jsonName, element.isOptional, element.className);
            } else {
                builder.addStatement("$L.$L = $L($L, $S, $L)", MODEL, element.fieldName, element.method, JSON_OBJECT,
                        element.jsonName, element.isOptional);
            }
        }

        return builder.build();
    }

    // TODO: Write object back to JSON
//    @Nonnull
//    MethodSpec createToJsonMethod() {
//        return MethodSpec.methodBuilder(TO_JSON)
//                .addAnnotation(CALL_SUPER)
//                .addAnnotation(NULLABLE)
//                .addParameter(ParameterSpec.builder(getObjectClassName(), MODEL).addAnnotation(NONNULL).build())
//                .addModifiers(PUBLIC)
//                .build();
//    }

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
        public final String method;

        public final boolean isOptional;

        public final boolean isArray;

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
            boolean isArray = false;

            switch (elementKind) {
                case BOOLEAN:
                    method = PARSE_BOOLEAN;
                    break;
                case INT:
                    method = PARSE_INT;
                    break;
                case LONG:
                    method = PARSE_LONG;
                    break;
                case DOUBLE:
                    method = PARSE_DOUBLE;
                    break;
                case SHORT:
                    method = PARSE_SHORT;
                    break;
                case FLOAT:
                    method = PARSE_FLOAT;
                    break;
                case ARRAY:
                    TypeKind arrayKind = ((ArrayType) element.asType()).getComponentType().getKind();
                    className = className.replace("[]", "");
                    isArray = true;
                    switch (arrayKind) {
                        case BOOLEAN:
                            method = PARSE_BOOLEAN_ARRAY;
                            break;
                        case INT:
                            method = PARSE_INT_ARRAY;
                            break;
                        case LONG:
                            method = PARSE_LONG_ARRAY;
                            break;
                        case DOUBLE:
                            method = PARSE_DOUBLE_ARRAY;
                            break;
                        case SHORT:
                            method = PARSE_SHORT_ARRAY;
                            break;
                        case FLOAT:
                            method = PARSE_FLOAT_ARRAY;
                            break;
                        case DECLARED:
                            if (STRING_CLASS.equals(className)) {
                                method = PARSE_STRING_ARRAY;
                            } else {
                                // TODO: Handle multi dimensional arrays
                                method = GET_ARRAY;
                                fromEpoxy = true;
                            }
                            break;
                        default:
                            throw new IllegalStateException("Invalid array element passed: " + element + ", type: " + arrayKind);
                    }
                    break;
                case DECLARED:
                    if (className.equals(STRING_CLASS)) {
                        method = PARSE_STRING;
                    } else if (className.contains(LIST_CLASS)) {
                        // TODO: Fix list handling
//                        className = className.replaceAll(".*<", "").replaceAll(">.*", "");
                        method = GET_ARRAY;
                        isList = true;
                        fromEpoxy = true;
                    } else {
                        method = GET_OBJECT;
                        fromEpoxy = true;
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid element passed: " + element + ", type: " + elementKind);
            }

            this.className = className;
            this.fromEpoxy = fromEpoxy;
            this.isArray = isArray;
            this.isList = isList;
        }

        @Override
        public String toString() {
            return "Field: " + fieldName +
                    ", Json: " + jsonName +
                    ", Optional: " + isOptional +
                    ", Getter: " + method;
        }
    }
}
