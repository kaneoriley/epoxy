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

import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

class EpoxyUtils {

    private static final String OPTIONAL = "Optional";
    private static final String NULLABLE = "Nullable";


    private EpoxyUtils() {
        throw new IllegalAccessError("no instances");
    }


    static boolean isOptional(@Nonnull Element element) {
        return element.getAnnotation(JsonField.class).optional() || hasAnnotationWithName(element, OPTIONAL, NULLABLE);
    }

    private static boolean hasAnnotationWithName(@Nonnull Element element, @Nonnull String... names) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            for (String name : names) {
                String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
                if (name.equals(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
