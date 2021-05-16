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

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

public final class TypeNames {

    public static TypeName erased(TypeName input) {
        if (input instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) input;
            return parameterizedTypeName.rawType;
        }
        if (input instanceof WildcardTypeName) {
            WildcardTypeName wild = (WildcardTypeName) input;
            return wild.lowerBounds.size() == 1 ? erased(wild.lowerBounds.get(0)) : TypeName.OBJECT;
        }

        if (input instanceof TypeVariableName) {
            TypeVariableName typeVar = (TypeVariableName) input;
            return typeVar.bounds.size() == 1 ? erased(typeVar.bounds.get(0)) : TypeName.OBJECT;
        }
        if (input instanceof ArrayTypeName) {
            ArrayTypeName arrayTypeName = (ArrayTypeName) input;
            return ArrayTypeName.of(erased(arrayTypeName.componentType));
        }
        return input;
    }

    private TypeNames() {}
}
