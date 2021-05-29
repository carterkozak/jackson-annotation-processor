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

import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

public final class JacksonAnnotationExtraction {

    private static final String JACKSON_ANNOTATION_NAME = JacksonAnnotation.class.getName();
    private static final String JACKSON_ANNOTATIONS_INSIDE_NAME = JacksonAnnotationsInside.class.getName();

    public static List<AnnotationMirror> getJacksonAnnotations(AnnotatedConstruct element) {
        List<AnnotationMirror> jacksonAnnotations = new ArrayList<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            DeclaredType annotationType = mirror.getAnnotationType();
            Element annotationTypeElement = annotationType.asElement();
            boolean annotatedWithJacksonAnnotation = false;
            boolean annotatedWithJacksonAnnotationsInside = false;
            for (AnnotationMirror metaAnnotationMirror : annotationTypeElement.getAnnotationMirrors()) {
                TypeElement metaAnnotationElement =
                        (TypeElement) metaAnnotationMirror.getAnnotationType().asElement();
                Name qualifiedName = metaAnnotationElement.getQualifiedName();
                if (qualifiedName.contentEquals(JACKSON_ANNOTATIONS_INSIDE_NAME)) {
                    annotatedWithJacksonAnnotationsInside = true;
                } else if (qualifiedName.contentEquals(JACKSON_ANNOTATION_NAME)) {
                    annotatedWithJacksonAnnotation = true;
                }
            }
            // If both JacksonAnnotationsInside and JacksonAnnotation are present, only produce the 'inside'
            // annotations, not the custom wrapper.
            if (annotatedWithJacksonAnnotationsInside) {
                jacksonAnnotations.addAll(getJacksonAnnotations(annotationTypeElement));
            } else if (annotatedWithJacksonAnnotation) {
                jacksonAnnotations.add(mirror);
            }
        }
        return jacksonAnnotations;
    }

    private JacksonAnnotationExtraction() {}
}
