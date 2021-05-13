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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

final class PropertyNames {

    static String name(JsonProperty property, ExecutableElement method) {
        String propertyValue = property.value();
        if (Objects.equals(JsonProperty.USE_DEFAULT_NAME, propertyValue)) {
            return method.getSimpleName().toString();
        }
        return propertyValue;
    }

    static String name(JsonProperty property, VariableElement variable) {
        String propertyValue = property.value();
        if (Objects.equals(JsonProperty.USE_DEFAULT_NAME, propertyValue)) {
            return variable.getSimpleName().toString();
        }
        return propertyValue;
    }

    static String name(JsonGetter property, ExecutableElement method) {
        String propertyValue = property.value();
        if (Objects.equals(JsonProperty.USE_DEFAULT_NAME, propertyValue)) {
            return method.getSimpleName().toString();
        }
        return propertyValue;
    }

    private PropertyNames() {}
}
