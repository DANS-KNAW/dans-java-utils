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

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.cli.EnvironmentCommand;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * A DropWizard {@link EnvironmentCommand} that configures the value of system property `dans.default.config` as the default for the `file` parameter. This can be used to create a start-up script for
 * the application that does not require the user to explicitly provide the `config.yml` location with every invocation, .e.g.,
 * <p>
 * ```bash ARGS=$@
 * <p>
 * java -Ddans.config.default=/etc/opt/dans.knaw.nl/my-app/config.yml my-app.jar $ARGS ```
 *
 * @param <T> the application's configuration class
 */
public abstract class DefaultConfigEnvironmentCommand<T extends Configuration> extends EnvironmentCommand<T> {
    private final boolean configFileAsOption;

    /**
     * Creates a new environment command.
     *
     * @param application        the application providing this command
     * @param name               the name of the command, used for command line invocation
     * @param description        a description of the command's purpose
     * @param configFileAsOption if <code>true</code>, the configuration file is set via an option <code>--config</code>, otherwise, as a positional argument
     */
    protected DefaultConfigEnvironmentCommand(Application<T> application, String name, String description, boolean configFileAsOption) {
        super(application, name, description);
        this.configFileAsOption = configFileAsOption;
    }

    /**
     * Creates a new environment command.
     *
     * @param application the application providing this command
     * @param name        the name of the command, used for command line invocation
     * @param description a description of the command's purpose
     */
    protected DefaultConfigEnvironmentCommand(Application<T> application, String name, String description) {
        this(application, name, description, false);
    }

    @Override
    protected Argument addFileArgument(Subparser subparser) {
        return ConfiguredCommandUtils.addFileArgument(subparser, configFileAsOption);
    }
}
