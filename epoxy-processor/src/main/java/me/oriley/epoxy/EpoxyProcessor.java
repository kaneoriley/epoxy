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

import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.*;
import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public final class EpoxyProcessor extends AbstractProcessor {

    private static final String COMMENT = "Generated code from EpoxyProcessor.\nDo not modify!";
    private static final String BINDER_MAP = "BINDER_MAP";
    private static final String GET_BINDER = "getBinder";
    private static final String GET_JSON_OBJECT = "getJSONObject";
    private static final String ADD = "add";
    private static final String ARRAY = "array";
    private static final String BINDER = "binder";
    private static final String JSON = "json";
    private static final String LENGTH = "length";
    private static final String LIST = "list";
    private static final String GET = "get";
    private static final String PUT = "put";
    private static final String NEW_INSTANCE = "newInstance";
    private static final String TYPE_CLASS = "typeClass";
    private static final String JSON_ARRAY = "jsonArray";
    static final String FROM_JSON = "fromJson";
    static final String TO_JSON = "toJson";
    static final String MODEL = "model";
    static final String LIST_FROM_JSON = "listFromJson";
    static final String JSON_OBJECT = "jsonObject";

    static final ClassName CALL_SUPER = ClassName.get("android.support.annotation", "CallSuper");
    static final ClassName NULLABLE = ClassName.get("android.support.annotation", "Nullable");
    static final ClassName NONNULL = ClassName.get("android.support.annotation", "NonNull");
    static final TypeVariableName T = TypeVariableName.get("T");

    private static final ClassName JSON_ARRAY_CLASS = ClassName.get("org.json", "JSONArray");
    static final ClassName EPOXY_JSON_BINDER = ClassName.get("me.oriley.epoxy", "EpoxyJsonBinder");
    static final ClassName EPOXY_JSON = ClassName.get("me.oriley.epoxy", "EpoxyJson");
    static final ClassName JSON_OBJECT_CLASS = ClassName.get("org.json", "JSONObject");
    static final ClassName JSON_EXCEPTION_CLASS = ClassName.get("org.json", "JSONException");

    @Nonnull
    private Elements mElementUtils;

    @Nonnull
    private Filer mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        mElementUtils = env.getElementUtils();
        mFiler = env.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(JsonField.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        Map<TypeElement, EpoxyJsonBinding> targetClassMap = new HashMap<>();
        for (Element element : env.getElementsAnnotatedWith(JsonField.class)) {
            if (!SuperficialValidation.validateElement(element)) continue;
            if (isInaccessibleViaGeneratedCode(JsonField.class, element)) continue;
            if (!isValidType(JsonField.class, element)) continue;

            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
            String componentPackage = getPackageName(enclosingElement);
            String className = getClassName(enclosingElement, componentPackage);

            EpoxyJsonBinding binding = targetClassMap.get(enclosingElement);
            if (binding == null) {
                binding = new EpoxyJsonBinding(componentPackage, className);
                targetClassMap.put(enclosingElement, binding);
            }
            binding.addElement(element);
        }

        if (targetClassMap.isEmpty()) {
            // Nothing to do
            return true;
        }

        for (Map.Entry<TypeElement, EpoxyJsonBinding> entry : targetClassMap.entrySet()) {
            TypeElement parentType = findParentType(entry.getKey(), targetClassMap.keySet());
            if (parentType != null) {
                EpoxyJsonBinding bindingClass = entry.getValue();
                EpoxyJsonBinding parentBindingClass = targetClassMap.get(parentType);
                bindingClass.setParentBinding(parentBindingClass);
            }
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(EPOXY_JSON)
                .addModifiers(PUBLIC, FINAL);

        // Binder map field
        TypeName binderMapType = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(Class.class), EPOXY_JSON_BINDER);
        builder.addField(FieldSpec.builder(binderMapType, BINDER_MAP, PRIVATE, STATIC, FINAL)
                .initializer("new $T<>()", HashMap.class)
                .build());

        CodeBlock.Builder initialiser = CodeBlock.builder();

        for (EpoxyJsonBinding binding : targetClassMap.values()) {
            try {
                initialiser.addStatement("$L.$L($T.class, new $T())", BINDER_MAP, PUT, binding.getObjectClassName(),
                        binding.getBindingClassName());
                brewJava(binding).writeTo(mFiler);
            } catch (IOException e) {
                error("Failed to write binding: %s", binding);
                e.printStackTrace();
            }
        }

        builder.addStaticBlock(initialiser.build());

        // Get binder method
        builder.addMethod(MethodSpec.methodBuilder(GET_BINDER)
                .addAnnotation(NULLABLE)
                .addModifiers(PRIVATE, STATIC)
                .addTypeVariable(T)
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
                        .addAnnotation(NONNULL).build())
                .addStatement("//noinspection unchecked")
                .addStatement("return $L.$L($L)", BINDER_MAP, GET, TYPE_CLASS)
                .returns(ParameterizedTypeName.get(EPOXY_JSON_BINDER, T))
                .build());

        // From JSON String method
        builder.addMethod(MethodSpec.methodBuilder(FROM_JSON)
                .addAnnotation(NULLABLE)
                .addModifiers(PUBLIC, STATIC)
                .addTypeVariable(T)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addParameter(ParameterSpec.builder(String.class, JSON).addAnnotation(NULLABLE).build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
                        .addAnnotation(NONNULL).build())
                .addStatement("if ($L == null) return null", JSON)
                .addStatement("$T $L = new $T($L)", JSON_OBJECT_CLASS, JSON_OBJECT, JSON_OBJECT_CLASS, JSON)
                .addStatement("return $L($L, $L)", FROM_JSON, JSON_OBJECT, TYPE_CLASS)
                .returns(T)
                .build());

        // From JSONObject method
        builder.addMethod(MethodSpec.methodBuilder(FROM_JSON)
                .addAnnotation(NULLABLE)
                .addModifiers(PUBLIC, STATIC)
                .addTypeVariable(T)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addParameter(ParameterSpec.builder(JSON_OBJECT_CLASS, JSON_OBJECT).addAnnotation(NULLABLE).build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
                        .addAnnotation(NONNULL).build())
                .addStatement("if ($L == null) return null", JSON_OBJECT)
                .addStatement("$T $L = $L($L)", ParameterizedTypeName.get(EPOXY_JSON_BINDER, T), BINDER, GET_BINDER, TYPE_CLASS)
                .beginControlFlow("if ($L == null)", BINDER)
                .addStatement("throw new $T(\"Binder not found for type \" + $L)", JSON_EXCEPTION_CLASS, TYPE_CLASS)
                .endControlFlow()
                .addStatement("return $L.$L($L)", BINDER, FROM_JSON, JSON_OBJECT)
                .returns(T)
                .build());

        // To JSONObject method
        builder.addMethod(MethodSpec.methodBuilder(TO_JSON)
                .addAnnotation(NULLABLE)
                .addModifiers(PUBLIC, STATIC)
                .addTypeVariable(T)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addParameter(ParameterSpec.builder(T, MODEL).addAnnotation(NULLABLE).build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
                        .addAnnotation(NONNULL).build())
                .addStatement("if ($L == null) return null", MODEL)
                .addStatement("$T $L = $L($L)", ParameterizedTypeName.get(EPOXY_JSON_BINDER, T), BINDER, GET_BINDER, TYPE_CLASS)
                .beginControlFlow("if ($L == null)", BINDER)
                .addStatement("throw new $T(\"Binder not found for type \" + $L)", JSON_EXCEPTION_CLASS, TYPE_CLASS)
                .endControlFlow()
                .addStatement("return $L.$L($L)", BINDER, TO_JSON, MODEL)
                .returns(JSON_OBJECT_CLASS)
                .build());

        // From JSONArray method
        builder.addMethod(MethodSpec.methodBuilder(FROM_JSON)
                .addAnnotation(NULLABLE)
                .addModifiers(PUBLIC, STATIC)
                .addTypeVariable(T)
                .addException(IOException.class)
                .addException(JSON_EXCEPTION_CLASS)
                .addParameter(ParameterSpec.builder(JSON_ARRAY_CLASS, JSON_ARRAY).addAnnotation(NULLABLE).build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
                        .addAnnotation(NONNULL).build())
                .addStatement("if ($L == null) return null", JSON_ARRAY)
                .addStatement("$T $L = $L($L)", ParameterizedTypeName.get(EPOXY_JSON_BINDER, T), BINDER, GET_BINDER, TYPE_CLASS)
                .beginControlFlow("if ($L == null)", BINDER)
                .addStatement("throw new $T(\"Binder not found for type \" + $L)", JSON_EXCEPTION_CLASS, TYPE_CLASS)
                .endControlFlow()
                .addStatement("int $L = $L.$L()", LENGTH, JSON_ARRAY, LENGTH)
                .addStatement("$T[] $L =  ($T[]) $T.$L($L, $L)", T, ARRAY, T, Array.class, NEW_INSTANCE, TYPE_CLASS, LENGTH)
                .beginControlFlow("for (int i = 0; i < $L; i++)", LENGTH)
                .addStatement("$L[i] = $L.$L($L.$L(i))", ARRAY, BINDER, FROM_JSON, JSON_ARRAY, GET_JSON_OBJECT)
                .endControlFlow()
                .addStatement("return $L", ARRAY)
                .returns(ArrayTypeName.of(T))
                .build());

        // TODO: List support
//        // From JSONArray method
//        ParameterizedTypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), T);
//        builder.addMethod(MethodSpec.methodBuilder(LIST_FROM_JSON)
//                .addAnnotation(NULLABLE)
//                .addModifiers(PUBLIC, STATIC)
//                .addTypeVariable(T)
//                .addException(IOException.class)
//                .addException(JSON_EXCEPTION_CLASS)
//                .addParameter(ParameterSpec.builder(JSON_ARRAY_CLASS, JSON_ARRAY).addAnnotation(NULLABLE).build())
//                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Class.class), T), TYPE_CLASS)
//                        .addAnnotation(NONNULL).build())
//                .addStatement("if ($L == null) return null", JSON_ARRAY)
//                .addStatement("$T $L = $L($L)", ParameterizedTypeName.get(EPOXY_JSON_BINDER, T), BINDER, GET_BINDER, TYPE_CLASS)
//                .beginControlFlow("if ($L == null)", BINDER)
//                .addStatement("throw new $T(\"Binder not found for type \" + $L)", JSON_EXCEPTION_CLASS, TYPE_CLASS)
//                .endControlFlow()
//                .addStatement("int $L = $L.$L()", LENGTH, JSON_ARRAY, LENGTH)
//                .addStatement("$T $L =  new $T<>()", List.class, LIST, ArrayList.class)
//                .beginControlFlow("for (int i = 0; i < $L; i++)", LENGTH)
//                .addStatement("$L.$L(i, $L.$L($L.$L(i)))", LIST, ADD, BINDER, FROM_JSON, JSON_ARRAY, GET_JSON_OBJECT)
//                .endControlFlow()
//                .addStatement("return $L", LIST)
//                .returns(listType)
//                .build());

        try {
            JavaFile.builder(EPOXY_JSON.packageName(), builder.build())
                    .indent("    ")
                    .addFileComment(COMMENT)
                    .build().writeTo(mFiler);
        } catch (IOException e) {
            error("Failed to write Epoxy class");
            e.printStackTrace();
        }

        return true;
    }

    @Nonnull
    private String getPackageName(@Nonnull TypeElement type) {
        return mElementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    @Nonnull
    private String getClassName(@Nonnull TypeElement type, @Nonnull String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen).replace('.', '$');
    }

    @Nonnull
    private JavaFile brewJava(@Nonnull EpoxyJsonBinding binding) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(binding.getBindingClassName())
                .addModifiers(PUBLIC)
                .addTypeVariable(TypeVariableName.get("T", binding.getObjectClassName()))
                .superclass(binding.getSuperClassName());

        builder.addMethod(binding.createFromJsonMethod())
                .addMethod(binding.createParseJsonMethod())
                .addMethod(binding.createToJsonMethod())
                .addMethod(binding.createParseModelMethod());

        return JavaFile.builder(binding.packageName, builder.build())
                .indent("    ")
                .addFileComment(COMMENT).build();
    }

    private boolean isValidType(@Nonnull Class<? extends Annotation> annotationClass,
                                @Nonnull Element element) {
        boolean isValid = true;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        TypeKind elementKind = element.asType().getKind();
        switch (elementKind) {
            case BYTE:
            case CHAR:
                error(element, "@%s fields must be a valid JSON type, found %s. (%s.%s)",
                        annotationClass.getSimpleName(), elementKind, enclosingElement.getQualifiedName(),
                        element.getSimpleName());
                isValid = false;
        }

        return isValid;
    }

    private boolean isInaccessibleViaGeneratedCode(@Nonnull Class<? extends Annotation> annotationClass,
                                                   @Nonnull Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(STATIC) || modifiers.contains(PRIVATE)) {
            error(element, "@%s fields must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s fields may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s fields may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private TypeElement findParentType(TypeElement typeElement, Set<TypeElement> parents) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (parents.contains(typeElement)) {
                return typeElement;
            }
        }
    }

    private void error(@Nonnull String message, @Nullable Object... args) {
        if (args != null && args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message);
    }

    private void error(@Nonnull Element element, @Nonnull String message, @Nullable Object... args) {
        if (args != null && args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(ERROR, message, element);
    }
}
