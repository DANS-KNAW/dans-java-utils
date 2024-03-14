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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.core.Configuration;
import io.dropwizard.util.Generics;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractCommandLineApp<C extends Configuration> implements Callable<Integer> {
    public static String CONFIG_FILE_KEY = "dans.default.config";
    public static String CONFIG_FILE_OVERRIDE_KEY = "dans.default.config.override";

    public void run(String[] args) throws IOException, ConfigurationException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        File configFile = new File(System.getProperty(CONFIG_FILE_KEY));
        List<File> configFiles = new ArrayList<>();
        configFiles.add(configFile);
        if (System.getProperty(CONFIG_FILE_OVERRIDE_KEY) != null) {
            configFiles.add(new File(System.getProperty(CONFIG_FILE_OVERRIDE_KEY)));
        }
        C config = new ConfigLoader<C>(Generics.getTypeParameter(getClass(), Configuration.class), configFiles.toArray(new File[] {})).loadConfiguration();
        MetricRegistry metricRegistry = new MetricRegistry();
        config.getLoggingFactory().configure(metricRegistry, getName());
        CommandLine commandLine = new CommandLine(this);
        configureCommandLine(commandLine, config);
        System.exit(commandLine.execute(args));
    }

    public abstract String getName();

    public abstract void configureCommandLine(CommandLine commandLine, C config);

    public Integer call() throws Exception {
        return 0;
    }

}
