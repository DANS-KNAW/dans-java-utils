/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrnUuidValidatorTest {

    @Test
    public void isValid_should_return_true_when_input_is_valid() {
        assertThat(new UrnUuidValidator().isValid("urn:uuid:123e4567-e89b-12d3-a456-426614174000", null)).isTrue();
    }

    @Test
    public void isValid_should_return_false_when_input_is_simple_uuid() {
        assertThat(new UrnUuidValidator().isValid("123e4567-e89b-12d3-a456-426614174000",  null)).isFalse();
    }

}
