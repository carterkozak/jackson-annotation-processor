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
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import net.ckozak.jackson.annotations.JacksonProcessor;
import net.ckozak.jackson.processor.gen.ser.Serializer;
import net.ckozak.jackson.processor.gen.ser.SerializerGenerator;
import net.ckozak.jackson.processor.gen.ser.Serializers;
import net.ckozak.jackson.processor.model.AccessorField;
import net.ckozak.jackson.processor.model.AccessorMethod;
import net.ckozak.jackson.processor.model.BoundProperty;

@AutoService(Processor.class)
@SuppressWarnings("checkstyle:CyclomaticComplexity")
public final class JacksonAnnotationProcessor extends AbstractProcessor {
    private static final ImmutableSet<String> ANNOTATIONS = ImmutableSet.of(JacksonProcessor.class.getName());
    private static final ImmutableSet<ElementKind> ENCLOSED_ANNOTATED_ELEMENTS =
            ImmutableSet.of(ElementKind.CONSTRUCTOR, ElementKind.FIELD, ElementKind.METHOD);

    private Messager messager;
    private Filer filer;
    private Elements elements;

    @SuppressWarnings("unused")
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public ImmutableSet<String> getSupportedAnnotationTypes() {
        return ANNOTATIONS;
    }

    @Override
    public boolean process(Set<? extends TypeElement> _annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(JacksonProcessor.class)) {
            if (element.getKind() != ElementKind.INTERFACE && element.getKind() != ElementKind.CLASS) {
                messager.printMessage(
                        Kind.ERROR,
                        "Only classes may be instrumented using @" + JacksonProcessor.class.getSimpleName(),
                        element);
                continue;
            }
            List<AccessorMethod> accessorMethods = new ArrayList<>();
            List<AccessorField> accessorFields = new ArrayList<>();
            TypeElement typeElement = (TypeElement) element;
            for (Element enclosed : typeElement.getEnclosedElements()) {
                if (!ENCLOSED_ANNOTATED_ELEMENTS.contains(enclosed.getKind())) {
                    continue;
                }
                Collection<AnnotationMirror> jacksonAnnotations = getJacksonAnnotations(enclosed);
                for (AnnotationMirror mirror : jacksonAnnotations) {
                    String name = mirror.getAnnotationType()
                            .asElement()
                            .getSimpleName()
                            .toString();
                    switch (name) {
                        case "com.fasterxml.jackson.annotation.JsonProperty":
                        case "com.fasterxml.jackson.annotation.JsonGetter":
                            break;
                        default:
                            messager.printMessage(Kind.ERROR, "Unexpected annotation: " + name, enclosed);
                    }
                }
                JsonProperty property = enclosed.getAnnotation(JsonProperty.class);
                JsonGetter getter = enclosed.getAnnotation(JsonGetter.class);
                if (property == null && getter == null) {
                    continue;
                }
                if (enclosed.getModifiers().contains(Modifier.PRIVATE)
                        || enclosed.getModifiers().contains(Modifier.PROTECTED)) {
                    messager.printMessage(
                            Kind.ERROR, "Annotated elements must be accessible from the same package", enclosed);
                    continue;
                }
                if (enclosed.getModifiers().contains(Modifier.STATIC)) {
                    messager.printMessage(Kind.ERROR, "Static elements are not supported", enclosed);
                    continue;
                }
                if (enclosed.getKind() == ElementKind.METHOD) {
                    ExecutableElement executableElement = (ExecutableElement) enclosed;
                    if (!executableElement.getParameters().isEmpty()) {
                        messager.printMessage(
                                Kind.ERROR, "Getter method must not take any arguments", executableElement);
                        continue;
                    }

                    accessorMethods.add(AccessorMethod.builder()
                            .method(executableElement)
                            .property(BoundProperty.builder()
                                    .name(
                                            property == null
                                                    ? PropertyNames.name(getter, executableElement)
                                                    : PropertyNames.name(property, executableElement))
                                    .type(TypeName.get(executableElement.getReturnType()))
                                    .build())
                            .build());
                } else if (enclosed.getKind() == ElementKind.FIELD) {
                    VariableElement variableElement = (VariableElement) enclosed;
                    accessorFields.add(AccessorField.builder()
                            .field(variableElement)
                            .property(BoundProperty.builder()
                                    .name(PropertyNames.name(property, variableElement))
                                    .type(TypeName.get(variableElement.asType()))
                                    .build())
                            .build());
                } else {
                    messager.printMessage(
                            Kind.ERROR,
                            "Only methods and fields are handled at the moment by @"
                                    + JacksonProcessor.class.getSimpleName(),
                            enclosed);
                    continue;
                }
            }

            List<Serializer> serializers = new ArrayList<>();
            int index = 0;
            for (AccessorMethod method : accessorMethods) {
                serializers.add(Serializers.serializerFor(
                        method.property().name(),
                        method.property().type(),
                        CodeBlock.of(
                                "$N.$N()",
                                "value",
                                method.method().getSimpleName().toString()),
                        "generator",
                        index++));
            }
            for (AccessorField field : accessorFields) {
                serializers.add(Serializers.serializerFor(
                        field.property().name(),
                        field.property().type(),
                        CodeBlock.of(
                                "$N.$N", "value", field.field().getSimpleName().toString()),
                        "generator",
                        index++));
            }

            try {
                JavaFile generatedFile = SerializerGenerator.generateSerializer(
                        getClass().getName(), elements, typeElement, serializers);
                try {
                    generatedFile.writeTo(filer);
                } catch (IOException e) {
                    messager.printMessage(
                            Kind.ERROR, "Failed to write instrumented class: " + Throwables.getStackTraceAsString(e));
                }
            } catch (RuntimeException e) {
                messager.printMessage(
                        Kind.ERROR, "Failed to generate instrumented class: " + Throwables.getStackTraceAsString(e));
            }
        }
        return false;
    }

    private static Collection<AnnotationMirror> getJacksonAnnotations(AnnotatedConstruct element) {
        Set<AnnotationMirror> jacksonAnnotations = new LinkedHashSet<>();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            DeclaredType annotationType = mirror.getAnnotationType();
            Element annotationTypeElement = annotationType.asElement();
            for (AnnotationMirror metaAnnotationMirror : annotationTypeElement.getAnnotationMirrors()) {
                Element metaAnnotationElement =
                        metaAnnotationMirror.getAnnotationType().asElement();
                String toString = metaAnnotationElement.toString();
                String nameString = metaAnnotationElement.getSimpleName().toString();
                if (metaAnnotationElement.getSimpleName().contentEquals(JacksonAnnotationsInside.class.getName())) {
                    jacksonAnnotations.addAll(getJacksonAnnotations(annotationTypeElement));
                } else if (metaAnnotationElement.getSimpleName().contentEquals(JacksonAnnotation.class.getName())) {
                    jacksonAnnotations.add(mirror);
                }
            }
        }
        return jacksonAnnotations;
    }
}
