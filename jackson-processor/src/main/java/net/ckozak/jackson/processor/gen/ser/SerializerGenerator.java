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

package net.ckozak.jackson.processor.gen.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.List;
import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public final class SerializerGenerator {

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public static JavaFile generateSerializer(
            String processorName, Elements elements, TypeElement typeElement, List<Serializer> serializers) {
        String packageName =
                elements.getPackageOf(typeElement).getQualifiedName().toString();
        String className = typeElement.getSimpleName() + "_GeneratedSerializer";
        TypeName targetType = TypeName.get(typeElement.asType());

        ImmutableList<FieldSpec> fieldSpecs = serializers.stream()
                .flatMap(ser -> ser.serializerFields().stream())
                .collect(ImmutableList.toImmutableList());

        ImmutableList<MethodSpec> methodSpecs = serializers.stream()
                .flatMap(ser -> ser.serializerMethods().stream())
                .collect(ImmutableList.toImmutableList());

        ImmutableList<CodeBlock> resolvers = serializers.stream()
                .flatMap(ser -> ser.resolverBlock().stream())
                .filter(block -> !block.isEmpty())
                .collect(ImmutableList.toImmutableList());

        TypeSpec.Builder specBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(ParameterizedTypeName.get(ClassName.get(StdSerializer.class), targetType))
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", processorName)
                        .build())
                .addFields(fieldSpecs)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement(
                                "super($T.defaultInstance().constructType(new $T<$T>() {}))",
                                TypeFactory.class,
                                TypeReference.class,
                                targetType)
                        .build());

        if (typeElement.getAnnotation(Deprecated.class) != null) {
            specBuilder.addAnnotation(Deprecated.class);
        }
        specBuilder
                .addMethod(MethodSpec.methodBuilder("serialize")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(ParameterSpec.builder(targetType, "value").build())
                        .addParameter(ParameterSpec.builder(JsonGenerator.class, "generator")
                                .build())
                        .addParameter(ParameterSpec.builder(SerializerProvider.class, "provider")
                                .build())
                        .addException(IOException.class)
                        .addStatement("$N.writeStartObject($N)", "generator", "value")
                        .addCode(serializers.stream()
                                .map(Serializer::serializerBlock)
                                .collect(CodeBlock.joining("", "", "")))
                        .addStatement("$N.writeEndObject()", "generator")
                        .build())
                .addMethods(methodSpecs);

        if (!resolvers.isEmpty()) {
            specBuilder
                    .addSuperinterface(ResolvableSerializer.class)
                    .addMethod(MethodSpec.methodBuilder("resolve")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                                    // Provides codegen type safety
                                    .addMember("value", "$S", "unchecked")
                                    .build())
                            .addParameter(ParameterSpec.builder(SerializerProvider.class, "provider")
                                    .build())
                            .addException(JsonMappingException.class)
                            .addCode(resolvers.stream().collect(CodeBlock.joining("", "", "")))
                            .build());
        }

        return JavaFile.builder(packageName, specBuilder.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private SerializerGenerator() {}
}
