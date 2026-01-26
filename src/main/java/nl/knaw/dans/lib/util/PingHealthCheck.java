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

import com.codahale.metrics.health.HealthCheck;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Health check for a service that responds to a ping request with "pong".
 */
@Slf4j
@AllArgsConstructor
public class PingHealthCheck extends HealthCheck {
    private final String name;
    private final Client httpClient;
    private final URI pingUri;

    @Override
    protected Result check() {
        try (Response response = httpClient.target(pingUri)
            .request(MediaType.TEXT_PLAIN)
            .get()) {

            if (response.getStatus() == 200) {
                String content = response.readEntity(String.class);

                if (!"pong".equals(content.trim())) {
                    return Result.unhealthy("%s ping URL did not respond with 'pong'", name);
                }
                else {
                    return Result.healthy();
                }
            }
            else {
                return Result.unhealthy("Service responded with unexpected status: %d %s", response.getStatus(), response.getStatusInfo());
            }
        }
        catch (Throwable e) {
            return Result.unhealthy("Connection to %s could not be established: %s", name, e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "PingHealthCheck{" +
            "name='" + name + '\'' +
            ", pingUri=" + pingUri +
            '}';
    }
}
