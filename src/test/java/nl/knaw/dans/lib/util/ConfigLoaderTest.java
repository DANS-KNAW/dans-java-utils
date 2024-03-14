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
package nl.knaw.dans.lib.util;

import io.dropwizard.configuration.ConfigurationValidationException;
import io.dropwizard.core.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigLoaderTest {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SimpleTestConfig extends Configuration {
        @NotNull
        @Size(min = 3)
        private String name;
        @NotNull
        private Integer age;
    }

    @Data
    public static class SubObject {
        @NotNull
        private String name;
        @NotNull
        private Integer age;
    }


    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestConfigWithSubObject extends Configuration {
        @NotNull
        private SubObject subConfig;
        @NotNull
        private Integer number;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestConfigWithArray extends Configuration {
        @NotNull
        String name;

        @NotNull
        private List<String> values;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestConfigWithArrayOfSubObjects extends Configuration {
        @NotNull
        String name;

        @NotNull
        private List<SubObject> values;
    }

    private File getTestFile(String name) {
        return new File(new File("src/test/resources/config-loader"), name);
    }

    @Test
    public void simple_config_should_be_loaded_correctly() throws Exception {
        SimpleTestConfig config = new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig1.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("John Doe");
        assertThat(config.getAge()).isEqualTo(30);
    }

    @Test
    public void string_property_should_replaced_with_override_value() throws Exception {
        SimpleTestConfig config = new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig1.yml"), getTestFile("SimpleTestConfigOverride.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("Jane Doe");
        assertThat(config.getAge()).isEqualTo(30);
    }

    @Test
    public void nested_object_should_be_overriden_with_corresponding_nested_object_in_override_config() throws Exception {
        TestConfigWithSubObject config = new ConfigLoader<>(TestConfigWithSubObject.class, getTestFile("TestConfigWithSubObject1.yml"),
            getTestFile("TestConfigWithSubObjectOverride.yml")).loadConfiguration();
        assertThat(config.getSubConfig().getName()).isEqualTo("Fulano");
        assertThat(config.getSubConfig().getAge()).isEqualTo(7);
        assertThat(config.getNumber()).isEqualTo(123);
    }

    @Test
    public void unoverriden_config_should_be_validated() throws Exception {
        ConfigurationValidationException e = assertThrows(ConfigurationValidationException.class,
            () -> new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig_Invalid.yml")).loadConfiguration());
        assertThat(e.getMessage()).contains("age must not be null");
    }

    @Test
    public void override_with_invalid_value_should_throw_exception() {
        ConfigurationValidationException e = assertThrows(ConfigurationValidationException.class,
            () -> new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig1.yml"), getTestFile("SimpleTestConfig_InvalidOverride.yml")).loadConfiguration());
        assertThat(e.getMessage()).contains("name size must be between 3 and");
    }

    @Test
    public void array_should_be_replaced_with_override_list() throws Exception {
        TestConfigWithArray config = new ConfigLoader<>(TestConfigWithArray.class, getTestFile("TestConfigWithArray1.yml"),
            getTestFile("TestConfigWithArrayOverride.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("John Doe");
        assertThat(config.getValues()).containsExactly("a", "b", "c");
    }

    @Test
    public void array_of_objects_should_be_replaced_with_override_array_of_objects() throws Exception {
        TestConfigWithArrayOfSubObjects config = new ConfigLoader<>(TestConfigWithArrayOfSubObjects.class, getTestFile("TestConfigWithArrayOfSubobjects.yml"),
            getTestFile("TestConfigWithArrayOfSubobjectsOverride.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("John Doe");
        assertThat(config.getValues()).hasSize(2);
        assertThat(config.getValues().get(0).getName()).isEqualTo("Pat Doe");
        assertThat(config.getValues().get(0).getAge()).isEqualTo(5);
        assertThat(config.getValues().get(1).getName()).isEqualTo("Jack Doe");
        assertThat(config.getValues().get(1).getAge()).isEqualTo(9);
    }

}
