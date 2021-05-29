/*
 * (c) Copyright 2021 Carter Kozak
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

package net.ckozak.jackson.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;

public final class AnnotationVisitors {

    public static <T> T visit(AnnotationMirror mirror, JacksonAnnotationVisitor<T> visitor) {
        TypeElement typeElement = (TypeElement) mirror.getAnnotationType().asElement();
        String fqcn = typeElement.getQualifiedName().toString();
        switch (fqcn) {
            case "com.fasterxml.jackson.annotation.JsonProperty":
                return visitor.visitJsonProperty(mirror);
            case "com.fasterxml.jackson.annotation.JsonGetter":
                return visitor.visitJsonGetter(mirror);
            case "com.fasterxml.jackson.annotation.JsonSetter":
                return visitor.visitJsonSetter(mirror);
            case "com.fasterxml.jackson.annotation.JsonCreator":
                return visitor.visitJsonCreator(mirror);
            case "com.fasterxml.jackson.annotation.JsonIgnore":
                return visitor.visitJsonIgnore(mirror);
        }
        throw new IllegalArgumentException("Unknown jackson annotation type: " + fqcn + " from " + mirror);
    }

    private AnnotationVisitors() {}
}
