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
package nl.knaw.dans.lib.util.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

/**
 * Implementation of {@link DependenciesReadyCheck} that waits until all health checks are healthy.
 */
@Slf4j
public class HealthChecksDependenciesReadyCheck implements DependenciesReadyCheck, Managed {
    private final List<HealthCheck> healthChecks = new ArrayList<>();
    private final Environment environment;
    private final DependenciesReadyCheckConfig config;

    private long pollInterval;
    private boolean running = false;

    public HealthChecksDependenciesReadyCheck(Environment environment, DependenciesReadyCheckConfig config) {
        this.environment = environment;
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        for (var name : config.getHealthChecks()) {
            var healthCheck = environment.healthChecks().getHealthCheck(name);
            if (healthCheck == null) {
                throw new IllegalArgumentException("Health check with name " + name + " not found");
            }
            healthChecks.add(healthCheck);
        }
        pollInterval = config.getPollInterval().toMilliseconds();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void waitUntilReady(String... checks) {
        List<HealthCheck> checksToWaitFor = getChecksToWaitFor(checks);
        while (running && !allHealthy(checksToWaitFor)) {
            log.warn("Not all health checks are healthy yet, waiting for {} ms", pollInterval);
            try {
                sleep(pollInterval);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for health checks to become healthy, stopping wait");
                break;
            }
        }
    }

    private List<HealthCheck> getChecksToWaitFor(String... checks) {
        if (checks.length == 0) {
            return healthChecks;
        }
        var checkNames = Arrays.asList(checks);
        var registeredChecks = environment.healthChecks();
        return checkNames.stream()
            .map(name -> {
                var healthCheck = registeredChecks.getHealthCheck(name);
                if (healthCheck == null) {
                    throw new IllegalArgumentException("Health check with name " + name + " not found");
                }
                return healthCheck;
            })
            .collect(Collectors.toList());
    }

    private boolean allHealthy(List<HealthCheck> checksToWaitFor) {
        for (var healthCheck : checksToWaitFor) {
            if (!healthCheck.execute().isHealthy()) {
                return false;
            }
        }
        return true;
    }
}
