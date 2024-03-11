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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;

import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

@Slf4j
public class ConfigLoader<C> {
    private final Class<C> configurationClass;
    private final File[] configFile;

    /**
     * Load a configuration from a file and possibly override it with other files.
     *
     * @param configurationClass the class of the configuration
     * @param configFile         the file(s) to load the configuration from, later files override earlier ones
     */
    public ConfigLoader(Class<C> configurationClass, @NotEmpty File... configFile) {
        this.configurationClass = configurationClass;
        this.configFile = configFile;
    }

    public C loadConfiguration() throws Exception {
        Validator validator = Validators.newValidator();
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        ConfigurationFactory<C> configurationFactory = new YamlConfigurationFactory<>(configurationClass, validator, objectMapper, "dans");
        C config = configurationFactory.build(configFile[0]);

        for (int i = 1; i < configFile.length; i++) {
            if (configFile[i].exists()) {
                C overrideConfig = loadOverrides(configFile[i]);
                merge(config, overrideConfig);
            }
            else {
                log.debug("Override config file {} does not exist, skipping", configFile[i]);
            }
        }
        return config;
    }

    private C loadOverrides(File configFile) throws IOException, ConfigurationException {
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        ConfigurationFactory<C> factory = new YamlConfigurationFactory<>(configurationClass, null, objectMapper, "dans");
        return factory.build(configFile);
    }

    private void merge(Object config, Object overrideConfig) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(overrideConfig);
        for (PropertyDescriptor descriptor : descriptors) {
            String name = descriptor.getName();
            if ("class".equals(name)) {
                continue; // No need to handle
            }
            if (PropertyUtils.isReadable(overrideConfig, name) && PropertyUtils.isWriteable(config, name)) {
                Object overrideValue = PropertyUtils.getSimpleProperty(overrideConfig, name);
                if (overrideValue == null) {
                    continue; // Ignore null value
                }
                if (overrideValue instanceof Collection) {
                    // For simplicity, replace the collection
                    PropertyUtils.setSimpleProperty(config, name, overrideValue);
                }
                else {
                    Object originalValue = PropertyUtils.getSimpleProperty(config, name);
                    if (originalValue != null && originalValue.getClass().equals(overrideValue.getClass())) {
                        if (originalValue instanceof String || originalValue instanceof Number || originalValue instanceof Boolean) {
                            // Treat as simple property and replace directly
                            PropertyUtils.setSimpleProperty(config, name, overrideValue);
                        }
                        else {
                            merge(originalValue, overrideValue);
                        }
                    }
                    else {
                        PropertyUtils.setSimpleProperty(config, name, overrideValue);
                    }
                }
            }
        }
    }
}
