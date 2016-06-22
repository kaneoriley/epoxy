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
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.*;

import static java.util.Locale.US;
import static me.oriley.epoxy.ProcessorUtils.isPrivate;
import static me.oriley.epoxy.ProcessorUtils.isStatic;

public final class EpoxyProcessor extends BaseProcessor {

    private static final String NAME = "name";
    private static final String EPOXY = "epoxy";
    private static final String FROM_JSON = "fromJson";
    private static final String TO_JSON = "toJson";
    private static final String JSON_READER = "jsonReader";
    private static final String JSON_WRITER = "jsonWriter";
    private static final String OBJECT = "object";

    @NonNull
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        mFiler = env.getFiler();
        setTag(EpoxyProcessor.class.getSimpleName());
    }

    @NonNull
    @Override
    protected Class[] getSupportedAnnotationClasses() {
        return new Class[]{JsonField.class};
    }

    @Override
    public boolean process(@NonNull Set<? extends TypeElement> annotations, @NonNull RoundEnvironment env) {
        if (env.processingOver()) {
            return true;
        }

        try {
            final Map<Element, List<Element>> bindings = collectBindings(env);

            for (Map.Entry<Element, List<Element>> entry : bindings.entrySet()) {
                String packageName = getPackageName(entry.getKey());
                writeToFile(packageName, createBinder(entry.getKey(), entry.getValue()));
            }
        } catch (EpoxyException e) {
            error(e.getMessage());
            return true;
        }

        return false;
    }

    @NonNull
    private TypeSpec createBinder(@NonNull Element hostType, @NonNull List<Element> elements) throws EpoxyException {
        // Class type
        String className = getClassName(hostType);
        NameAllocator nameAllocator = new NameAllocator();

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(ClassName.get(JsonAdapter.class),
                TypeName.get(hostType.asType()));

        // Class builder
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className + JsonAdapter.CLASS_SUFFIX)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(parameterizedTypeName);

        // Create restoreInstance method
        MethodSpec fromMethod = MethodSpec.methodBuilder(FROM_JSON)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(Epoxy.class, EPOXY).addAnnotation(NonNull.class).build())
                .addParameter(ParameterSpec.builder(JsonReader.class, JSON_READER).addAnnotation(NonNull.class).build())
                .addCode(createFromJsonMethod(typeSpecBuilder, nameAllocator, hostType, elements))
                .addException(IOException.class)
                .returns(TypeName.get(hostType.asType()))
                .build();

        // Create restoreInstance method
        MethodSpec toMethod = MethodSpec.methodBuilder(TO_JSON)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(Epoxy.class, EPOXY).addAnnotation(NonNull.class).build())
                .addParameter(ParameterSpec.builder(JsonWriter.class, JSON_WRITER).addAnnotation(NonNull.class).build())
                .addParameter(ParameterSpec.builder(TypeName.get(hostType.asType()), OBJECT).addAnnotation(Nullable.class).build())
                .addCode(createToJsonMethod(typeSpecBuilder, nameAllocator, elements))
                .addException(IOException.class)
                .build();

        return typeSpecBuilder.addMethod(fromMethod).addMethod(toMethod).build();
    }

    @NonNull
    private CodeBlock createToJsonMethod(@NonNull TypeSpec.Builder typeBuilder,
                                         @NonNull NameAllocator nameAllocator,
                                         @NonNull List<Element> elements) throws EpoxyException {
        CodeBlock.Builder builder = CodeBlock.builder()
                .beginControlFlow("if ($N == null)", OBJECT)
                .add("$N.nullValue();\n", JSON_WRITER)
                .add("return;\n")
                .endControlFlow()
                .add("$N.beginObject();\n", JSON_WRITER);

        for (Element element : elements) {
            addEpoxyStatement(typeBuilder, builder, nameAllocator, element, true);
        }

        return builder.add("$N.endObject();\n", JSON_WRITER).build();
    }

    @NonNull
    private CodeBlock createFromJsonMethod(@NonNull TypeSpec.Builder typeBuilder,
                                           @NonNull NameAllocator nameAllocator,
                                           @NonNull Element hostType,
                                           @NonNull List<Element> elements) throws EpoxyException {
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("$T $N = new $T();\n", hostType, OBJECT, hostType)
                .add("$N.beginObject();\n", JSON_READER)
                .beginControlFlow("while ($N.hasNext())", JSON_READER)
                .add("$T $N = $N.nextName();\n", String.class, NAME, JSON_READER)
                .beginControlFlow("if ($N.peek() == $T.NULL)", JSON_READER, JsonToken.class)
                // TODO: Do we actually want to do this?
                .add("$N.skipValue();\n", JSON_READER)
                .add("continue;\n")
                .endControlFlow()
                .beginControlFlow("switch ($N)", NAME);

        for (Element element : elements) {
            addEpoxyStatement(typeBuilder, builder, nameAllocator, element, false);
        }

        builder.beginControlFlow("default:")
                .add("$N.skipValue();\n", JSON_READER)
                .endControlFlow()
                .endControlFlow()
                .endControlFlow()
                .add("$N.endObject();\n", JSON_READER)
                .add("return $N;\n", OBJECT);

        return builder.build();
    }

    private void addEpoxyStatement(@NonNull TypeSpec.Builder typeBuilder,
                                   @NonNull CodeBlock.Builder codeBuilder,
                                   @NonNull NameAllocator nameAllocator,
                                   @NonNull Element element,
                                   boolean writer) throws EpoxyException {
        JsonField jsonField = element.getAnnotation(JsonField.class);
        if (writer) {
            codeBuilder.add("$N.name($S);\n", JSON_WRITER, jsonField.value());
        } else {
            codeBuilder.beginControlFlow("case $S:", jsonField.value());
        }

        String erasedName = erasedType(element.asType());

        if (List.class.getCanonicalName().equals(erasedName)) {
            DeclaredType declaredType = (DeclaredType) element.asType();
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.size() != 1) {
                throw new EpoxyException("Argument type not specified for list: " + element);
            }
            TypeMirror elementType = typeArguments.get(0);

            String typeField = "LIST_" + getClassName(asElement(elementType)).toUpperCase(US) + "_TYPE";
            try {
                nameAllocator.get(typeField);
            } catch (Exception e) {
                // Doesn't exist, add the field
                nameAllocator.newName(typeField, typeField);
                typeBuilder.addField(FieldSpec.builder(ParameterizedType.class, typeField)
                        .initializer("$T.newParameterizedType($T.class, $T.class)", Types.class, List.class,
                                elementType).addModifiers(Modifier.FINAL, Modifier.PRIVATE).build());
            }

            if (writer) {
                codeBuilder.add("$N.toJson($N, $N.$N, $N);\n", EPOXY, JSON_WRITER, OBJECT, element.getSimpleName(), typeField);
            } else {
                codeBuilder.add("$N.$N = $N.fromJson($N, $N);\n", OBJECT, element.getSimpleName(), EPOXY, JSON_READER, typeField);
            }
        } else if (Map.class.getCanonicalName().equals(erasedName)) {
            DeclaredType declaredType = (DeclaredType) element.asType();
            List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
            if (typeArguments.size() != 2) {
                throw new EpoxyException("Invalid arguments specified for map: " + element);
            }

            TypeMirror keyType = typeArguments.get(0);
            if (!isAssignable(keyType, String.class)) {
                throw new EpoxyException("Map requires string key types, found " + keyType + ": " + element);
            }

            TypeMirror valueType = typeArguments.get(1);
            String typeField = "MAP_" + getClassName(asElement(keyType)).toUpperCase(US) + "_" +
                    getClassName(asElement(valueType)).toUpperCase(US) + "_TYPE";
            try {
                nameAllocator.get(typeField);
            } catch (Exception e) {
                // Doesn't exist, add the field
                nameAllocator.newName(typeField, typeField);
                typeBuilder.addField(FieldSpec.builder(ParameterizedType.class, typeField)
                        .initializer("$T.newParameterizedType($T.class, $T.class, $T.class)", Types.class, Map.class,
                                String.class, valueType).addModifiers(Modifier.FINAL, Modifier.PRIVATE).build());
            }

            if (writer) {
                codeBuilder.add("$N.toJson($N, $N.$N, $N);\n", EPOXY, JSON_WRITER, OBJECT, element.getSimpleName(), typeField);
            } else {
                codeBuilder.add("$N.$N = $N.fromJson($N, $N);\n", OBJECT, element.getSimpleName(), EPOXY, JSON_READER, typeField);
            }
        } else {
            if (writer) {
                codeBuilder.add("$N.toJson($N, $N.$N, $T.class);\n", EPOXY, JSON_WRITER, OBJECT, element.getSimpleName(), element.asType());
            } else {
                codeBuilder.add("$N.$N = $N.fromJson($N, $T.class);\n", OBJECT, element.getSimpleName(), EPOXY, JSON_READER, element.asType());
            }
        }

        if (!writer) {
            codeBuilder.add("break;\n").endControlFlow();
        }
    }

    private Map<Element, List<Element>> collectBindings(@NonNull RoundEnvironment env) throws EpoxyException {
        Map<Element, List<Element>> bindings = new HashMap<>();
        for (Element e : env.getElementsAnnotatedWith(JsonField.class)) {
            if (e.getKind() != ElementKind.FIELD) {
                throw new EpoxyException(e.getSimpleName() + " is annotated with @" + JsonField.class.getName() +
                        " but is not a field");
            }

            if (isPrivate(e)) {
                throw new EpoxyException("Field must not be private: " + e.getSimpleName());
            } else if (isStatic(e)) {
                throw new EpoxyException("Field must not be static: " + e.getSimpleName());
            }

            final Element type = findEnclosingElement(e);
            // class should exist
            if (type == null) {
                throw new EpoxyException("Could not find a class for " + e.getSimpleName());
            }
            // and it should be public
            if (isPrivate(type)) {
                throw new EpoxyException("Class is private: " + type);
            }
            // as well as all parent classes
            Element parentType = findEnclosingElement(type);
            while (parentType != null) {
                if (isPrivate(parentType)) {
                    throw new EpoxyException("Parent class is private: " + parentType);
                }
                parentType = findEnclosingElement(parentType);
            }

            List<Element> elements = bindings.get(type);
            if (elements == null) {
                elements = new ArrayList<>();
                bindings.put(type, elements);
            }

            elements.add(e);
        }

        return bindings;
    }

    @NonNull
    private JavaFile writeToFile(@NonNull String packageName, @NonNull TypeSpec spec) throws EpoxyException {
        final JavaFile file = JavaFile.builder(packageName, spec)
                .addFileComment("Generated by EpoxyProcessor, do not edit manually!")
                .indent("    ").build();
        try {
            file.writeTo(mFiler);
        } catch (IOException e) {
            throw new EpoxyException(e);
        }
        return file;
    }
}