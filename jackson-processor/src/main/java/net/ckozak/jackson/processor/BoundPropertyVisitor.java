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

import com.squareup.javapoet.TypeName;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import net.ckozak.jackson.processor.model.AccessorMethod;
import net.ckozak.jackson.processor.model.BoundProperty;

public class BoundPropertyVisitor implements JacksonAnnotationVisitor<Optional<BoundProperty>> {
    private final TypeName typeName;
    private final ExecutableElement executableElement;

    BoundPropertyVisitor(TypeName typeName, ExecutableElement executableElement) {
        this.typeName = typeName;
        this.executableElement = executableElement;
    }

    @Override
    public Optional<BoundProperty> visitJsonProperty(AnnotationMirror mirror) {
        return Optional.empty();
    }

    @Override
    public Optional<BoundProperty> visitJsonGetter(AnnotationMirror mirror) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues =
                elements.getElementValuesWithDefaults(mirror);

        if (annotationValues.size() != 1) {
            throw new IllegalStateException("Unexpected annotations found on JsonGetter: " + annotationValues);
        }
        String keyName =
                annotationValues.keySet().iterator().next().getSimpleName().toString();
        if (!Objects.equals("value", keyName)) {
            throw new RuntimeException("Unexpected property name: " + keyName);
        }
        // JsonGetter is only supported on methods
        ExecutableElement executableElement = (ExecutableElement) enclosed;
        String propertyName = PropertyNames.name(
                (String) annotationValues.values().iterator().next().getValue(), (ExecutableElement) enclosed);
        accessorMethods.add(AccessorMethod.builder()
                .method(executableElement)
                .property(BoundProperty.builder()
                        .name(propertyName)
                        .type(TypeName.get(executableElement.getReturnType()))
                        .build())
                .build());
        return Optional.empty();
    }

    @Override
    public Optional<BoundProperty> visitJsonSetter(AnnotationMirror mirror) {
        return Optional.empty();
    }

    @Override
    public Optional<BoundProperty> visitJsonCreator(AnnotationMirror mirror) {
        return Optional.empty();
    }

    @Override
    public Optional<BoundProperty> visitJsonIgnore(AnnotationMirror mirror) {
        return Optional.empty();
    }
}
