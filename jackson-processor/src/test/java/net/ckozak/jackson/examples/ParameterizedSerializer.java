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

package net.ckozak.jackson.examples;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.List;

public final class ParameterizedSerializer<T> extends StdSerializer<Parameterized<T>> implements ResolvableSerializer {
    private final JavaType type;

    private JsonSerializer<T> listObjectSerializer;

    public ParameterizedSerializer(JavaType type) {
        super(type);
        this.type = type;
    }

    @Override
    public void serialize(Parameterized<T> value, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        generator.writeStartObject(value);
        generator.writeFieldName("foo");
        generator.writeString(value.getFoo());
        List<T> valueList = value.getList();
        if (valueList == null) {
            generator.writeNullField("list");
        } else {
            generator.writeFieldName("list");
            generator.writeStartArray(valueList, valueList.size());
            for (T listValue : value.getList()) {
                listObjectSerializer.serialize(listValue, generator, provider);
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        listObjectSerializer = (JsonSerializer<T>)
                provider.findValueSerializer(type.getBindings().getBoundType(0));
    }
}
