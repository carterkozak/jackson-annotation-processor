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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;
import net.ckozak.jackson.annotations.JacksonProcessor;

@JacksonProcessor
@JsonSerialize(using = Simple_GeneratedSerializer.class)
public final class Simple {

    private final String value;
    private final int numeric;
    private final SomeType someType;
    private final OptionalInt optionalInt;
    private final Optional<BigDecimal> optionalBigDecimal;

    @JsonProperty("field")
    public final Object fieldValue;

    @JsonCreator(mode = Mode.PROPERTIES)
    public Simple(
            @JsonProperty("foo") String value,
            @JsonProperty("int") int numeric,
            @JsonProperty("arbitraryObject") SomeType someType,
            @JsonProperty("optionalInt") OptionalInt optionalInt,
            @JsonProperty("optionalBigDecimal") Optional<BigDecimal> optionalBigDecimal) {
        this.value = value;
        this.numeric = numeric;
        this.someType = someType;
        this.optionalInt = optionalInt;
        this.optionalBigDecimal = optionalBigDecimal;
        this.fieldValue = "value";
    }

    @JsonProperty("foo")
    public String getFoo() {
        return value;
    }

    @JsonProperty("int")
    public int getNumeric() {
        return numeric;
    }

    @JsonProperty("boxedInt")
    public Integer getBoxedNumeric() {
        return numeric;
    }

    @JsonProperty("arbitraryObject")
    public SomeType getSomeType() {
        return someType;
    }

    @JsonProperty("optionalInt")
    public OptionalInt getOptionalInt() {
        return optionalInt;
    }

    @JsonProperty("optionalBigDecimal")
    public Optional<BigDecimal> getOptionalBigDecimal() {
        return optionalBigDecimal;
    }
}
