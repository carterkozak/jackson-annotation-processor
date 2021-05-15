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
import java.util.List;

// @JacksonProcessor
// @JsonSerialize(using = ParameterizedSerializer.class)
public final class Parameterized<T> {

    private final String value;
    private final int numeric;
    private final List<T> vals;

    @JsonCreator(mode = Mode.PROPERTIES)
    public Parameterized(
            @JsonProperty("foo") String value, @JsonProperty("int") int numeric, @JsonProperty("list") List<T> vals) {
        this.value = value;
        this.numeric = numeric;
        this.vals = vals;
    }

    @JsonProperty("foo")
    public String getFoo() {
        return value;
    }

    @JsonProperty("int")
    public int getNumeric() {
        return numeric;
    }

    @JsonProperty("list")
    public List<T> getList() {
        return vals;
    }
}
