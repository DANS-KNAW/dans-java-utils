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
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.net.URI;
import java.text.MessageFormat;

import static java.text.MessageFormat.format;

@Getter
@Setter
@Slf4j
public class DataverseClientFactory {
    private URI baseUrl;
    private String apiKey;
    private String unblockKey;
    private int awaitLockStateMaxNumberOfRetries = 30;
    private int awaitLockStateMillisecondsBetweenRetries = 500;
    private int awaitIndexingMaxNumberOfRetries = 15;
    private int awaitIndexingMillisecondsBetweenRetries = 1000;
    private HttpClientConfiguration httpClient = null;

    public DataverseClient build() {
        return build(null, null, null);
    }

    public DataverseClient build(Environment environment, String name) {
        return build(environment, name, null);
    }

    private void noNanos(Duration t, String name) {
        if (t.toMilliseconds() * 1000000 != t.toNanoseconds())
            throw new IllegalArgumentException(format("{0}.{1}.{2} must not be set in nanoseconds {3}", DataverseClient.class.getSimpleName(), httpClient.getClass().getSimpleName(), name, t));
    }

    public DataverseClient build(Environment environment, String name, String overrideApiKey) {

        if (httpClient != null) {
            noNanos(httpClient.getTimeout(), "timeout");
            noNanos(httpClient.getConnectionTimeout(), "connectionTimeout");
            noNanos(httpClient.getConnectionRequestTimeout(), "connectionRequestTimeout");
            noNanos(httpClient.getKeepAlive(), "keepAlive");
        }

        DataverseClientConfig config = new DataverseClientConfig(
            baseUrl,
            overrideApiKey != null ? overrideApiKey : apiKey,
            awaitLockStateMaxNumberOfRetries,
            awaitLockStateMillisecondsBetweenRetries,
            awaitIndexingMaxNumberOfRetries,
            awaitIndexingMillisecondsBetweenRetries,
            unblockKey);

        ObjectMapper objectMapper = environment == null ? new ObjectMapper() : environment.getObjectMapper();
        if (httpClient == null) {
            return new DataverseClient(config, HttpClients.createDefault(), objectMapper);
        }
        else {
            Environment env = environment == null ? new Environment(DataverseClient.class.getSimpleName()) : environment;
            return new DataverseClient(config,
                new HttpClientBuilder(env).using(httpClient).build(name), // N.B. name must be unique in the Environment, otherwise the old connection will be overwritten
                objectMapper);
        }
    }

}
