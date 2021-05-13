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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import net.ckozak.jackson.examples.Parameterized;
import net.ckozak.jackson.examples.SomeType;
import org.junit.jupiter.api.Test;

public final class SerializerTest {

    private static final ObjectMapper mapper =
            (ObjectMapper) new ObjectMapper().setHandlerInstantiator(CustomHandlerInstantiator.INSTANCE);

    @Test
    public void testConcrete() throws IOException {
        ObjectWriter writer = mapper.writerFor(new TypeReference<Parameterized<SomeType>>() {});
        writer.writeValueAsString(new Parameterized<>("st", 3, ImmutableList.of(new SomeType("foo"))));
    }
}
