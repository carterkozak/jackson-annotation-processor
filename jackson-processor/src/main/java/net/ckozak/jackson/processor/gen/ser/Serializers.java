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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.lang.model.element.Modifier;
import net.ckozak.jackson.processor.StandardNames;

public final class Serializers {

    private static final ClassName OPTIONAL = ClassName.get(Optional.class);
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName BIG_INT = ClassName.get(BigInteger.class);
    private static final ClassName BIG_DEC = ClassName.get(BigDecimal.class);

    private static final ClassName OPTIONAL_INT = ClassName.get(OptionalInt.class);
    private static final ClassName OPTIONAL_LONG = ClassName.get(OptionalLong.class);
    private static final ClassName OPTIONAL_DOUBLE = ClassName.get(OptionalDouble.class);

    public static Serializer serializerFor(
            String fieldName, TypeName valueType, CodeBlock valueAccessor, String generatorName, int index) {
        if (STRING.equals(valueType)) {
            return Serializer.builder()
                    .serializerBlock(CodeBlock.builder()
                            .addStatement("$N.writeStringField($S, $L)", generatorName, fieldName, valueAccessor)
                            .build())
                    .build();
        }
        if (isNullableNumber(valueType)) {
            // Ideally this would be beased on the property name in some way, for cleaner stack traces.
            // Punting on the complexity for now.
            String methodName = "serializeField" + index;
            String valueName = StandardNames.VALUE;
            return Serializer.builder()
                    .serializerBlock(CodeBlock.builder()
                            .addStatement("$N($L, $N)", methodName, valueAccessor, generatorName)
                            .build())
                    .addSerializerMethods(MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                            .addParameter(ParameterSpec.builder(valueType, StandardNames.VALUE)
                                    .build())
                            .addParameter(ParameterSpec.builder(JsonGenerator.class, StandardNames.GENERATOR)
                                    .build())
                            .addException(IOException.class)
                            .beginControlFlow("if ($N == null)", valueName)
                            .addStatement("$N.writeNullField($S)", generatorName, fieldName)
                            .nextControlFlow("else")
                            .addStatement("$N.writeNumberField($S, $N)", generatorName, fieldName, valueName)
                            .endControlFlow()
                            .build())
                    .build();
        }
        Optional<Serializer> optionalNumberSerializer =
                optionalNumber(fieldName, valueType, valueAccessor, generatorName, index);
        if (optionalNumberSerializer.isPresent()) {
            return optionalNumberSerializer.get();
        }
        if (isNumber(valueType)) {
            return Serializer.builder()
                    .serializerBlock(CodeBlock.builder()
                            .addStatement("$N.writeNumberField($S, $L)", generatorName, fieldName, valueAccessor)
                            .build())
                    .build();
        }
        if (ClassName.OBJECT.equals(valueType)) {
            return Serializer.builder()
                    .serializerBlock(CodeBlock.builder()
                            .addStatement("$N.writeObjectField($S, $L)", generatorName, fieldName, valueAccessor)
                            .build())
                    .build();
        }
        // TODO(ckozak): Handle optional and collection serialization inline
        String serializerFieldName = "serializer" + index;
        ClassName rawSerializerType = ClassName.get(JsonSerializer.class);
        TypeName serializerType = ParameterizedTypeName.get(rawSerializerType, valueType);
        return Serializer.builder()
                .serializerBlock(CodeBlock.builder()
                        .addStatement(
                                "$N.serialize($L, $N, $N)",
                                serializerFieldName,
                                valueAccessor,
                                StandardNames.GENERATOR,
                                StandardNames.PROVIDER)
                        .build())
                .resolverBlock(CodeBlock.builder()
                        // This is a bit of a type-system hack, I narrow the serializer type to the field type
                        // to cause a compilation failure if the generated code doesn't work the way we expect.
                        .addStatement(
                                "$N = ($T) ($T) $N.findValueSerializer($N.getTypeFactory()"
                                        + ".constructType(new $T<$T>() {}))",
                                serializerFieldName,
                                serializerType,
                                rawSerializerType,
                                StandardNames.PROVIDER,
                                StandardNames.PROVIDER,
                                TypeReference.class,
                                valueType)
                        .build())
                .addSerializerFields(FieldSpec.builder(serializerType, serializerFieldName)
                        .addModifiers(Modifier.PRIVATE)
                        .build())
                .build();
    }

    private static boolean isNullableNumber(TypeName type) {
        return !type.isPrimitive() && isNumber(type);
    }

    private static Optional<Serializer> optionalNumber(
            String fieldName, TypeName type, CodeBlock valueAccessor, String generatorName, int index) {
        String unwrapMethod = null;
        if (OPTIONAL_INT.equals(type)) {
            unwrapMethod = "getAsInt";
        } else if (OPTIONAL_LONG.equals(type)) {
            unwrapMethod = "getAsLong";
        } else if (OPTIONAL_DOUBLE.equals(type)) {
            unwrapMethod = "getAsDouble";
        }
        if (type instanceof ParameterizedTypeName) {
            ParameterizedTypeName param = (ParameterizedTypeName) type;
            if (OPTIONAL.equals(param.rawType) && isNumber(param.typeArguments.get(0))) {
                unwrapMethod = "get";
            }
        }
        if (unwrapMethod != null) {
            String methodName = "serializeField" + index;
            String valueName = StandardNames.VALUE;
            return Optional.of(Serializer.builder()
                    .serializerBlock(CodeBlock.builder()
                            .addStatement("$N($L, $N)", methodName, valueAccessor, generatorName)
                            .build())
                    .addSerializerMethods(MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                            .addParameter(ParameterSpec.builder(type, StandardNames.VALUE)
                                    .build())
                            .addParameter(ParameterSpec.builder(JsonGenerator.class, StandardNames.GENERATOR)
                                    .build())
                            .addException(IOException.class)
                            // Fail (NPE) on null optional.
                            .beginControlFlow("if ($N.isPresent())", valueName)
                            .addStatement(
                                    "$N.writeNumberField($S, $N.$N())",
                                    generatorName,
                                    fieldName,
                                    valueName,
                                    unwrapMethod)
                            .nextControlFlow("else")
                            .addStatement("$N.writeNullField($S)", generatorName, fieldName)
                            .endControlFlow()
                            .build())
                    .build());
        }
        return Optional.empty();
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private static boolean isNumber(TypeName type) {
        return TypeName.INT.equals(type)
                || TypeName.FLOAT.equals(type)
                || TypeName.LONG.equals(type)
                || TypeName.SHORT.equals(type)
                || TypeName.BYTE.equals(type)
                || TypeName.DOUBLE.equals(type)
                || TypeName.INT.box().equals(type)
                || TypeName.FLOAT.box().equals(type)
                || TypeName.LONG.box().equals(type)
                || TypeName.SHORT.box().equals(type)
                || TypeName.BYTE.box().equals(type)
                || TypeName.DOUBLE.box().equals(type)
                || BIG_INT.equals(type)
                || BIG_DEC.equals(type);
    }

    private Serializers() {}
}
