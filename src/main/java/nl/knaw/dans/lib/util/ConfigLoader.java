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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.ConfigurationValidationException;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotEmpty;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public C loadConfiguration() throws ConfigurationException, IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Validator validator = Validators.newValidator();
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());

        SubstitutingSourceProvider sourceProvider = new SubstitutingSourceProvider(
            new FileConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        );

        ConfigurationFactory<C> configurationFactory = new YamlConfigurationFactory<>(configurationClass, validator, objectMapper, "dans");
        C config = configurationFactory.build(sourceProvider, configFile[0].getPath());

        for (int i = 1; i < configFile.length; i++) {
            if (configFile[i].exists()) {
                updateConfig(config, configFile[i]);
            }
            else {
                log.debug("Override config file {} does not exist, skipping", configFile[i]);
            }
        }
        String paths = String.join(", overridden by ", Arrays.stream(configFile).map(File::getAbsolutePath).toArray(String[]::new));
        validate(validator, paths, config);
        return config;
    }

    private void updateConfig(C config, File overrideConfig) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setDefaultMergeable(true);
        mapper.configOverride(List.class).setMergeable(false);
        mapper.readerForUpdating(config).readValue(overrideConfig);
    }

    private void validate(Validator validator, String path, C config) throws ConfigurationValidationException {
        if (validator != null) {
            final Set<ConstraintViolation<C>> violations = validator.validate(config);
            if (!violations.isEmpty()) {
                throw new ConfigurationValidationException(path, violations);
            }
        }
    }
}
