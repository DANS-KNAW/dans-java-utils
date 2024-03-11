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
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigLoaderTest {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SimpleTestConfig extends Configuration {
        @NotNull
        private String name;
        @NotNull
        private Integer age;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TestConfigWithSubObject extends Configuration {
        @NotNull
        private SimpleTestConfig subConfig;
        @NotNull
        private Integer number;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Confi

    private File getTestFile(String name) {
        return new File(new File("src/test/resources/config-loader"), name);
    }

    @Test
    public void testLoadConfiguration() throws Exception {
        SimpleTestConfig config = new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig1.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("John Doe");
        assertThat(config.getAge()).isEqualTo(30);
    }

    @Test
    public void testLoadConfigurationWithOverride() throws Exception {
        SimpleTestConfig config = new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig1.yml"), getTestFile("SimpleTestConfigOverride.yml")).loadConfiguration();
        assertThat(config.getName()).isEqualTo("Jane Doe");
        assertThat(config.getAge()).isEqualTo(30);
    }

    @Test
    public void testLoadConfigurationWithSubObject() throws Exception {
        TestConfigWithSubObject config = new ConfigLoader<>(TestConfigWithSubObject.class, getTestFile("TestConfigWithSubObject1.yml"),
            getTestFile("TestConfigWithSubObjectOverride.yml")).loadConfiguration();
        assertThat(config.getSubConfig().getName()).isEqualTo("Fulano");
        assertThat(config.getSubConfig().getAge()).isEqualTo(7);
        assertThat(config.getNumber()).isEqualTo(123);
    }

    @Test
    public void validateBaseConfig() throws Exception {
        ConfigurationValidationException e = assertThrows(ConfigurationValidationException.class,
            () -> new ConfigLoader<>(SimpleTestConfig.class, getTestFile("SimpleTestConfig_Invalid.yml")).loadConfiguration());
        assertThat(e.getMessage()).contains("age must not be null");
    }

    // Test that override file cannot put in invalid values

    @Test


}
