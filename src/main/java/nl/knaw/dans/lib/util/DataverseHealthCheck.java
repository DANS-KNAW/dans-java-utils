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
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DataverseHealthCheck extends HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(DataverseHealthCheck.class);

    private final DataverseClient dataverseClient;

    public DataverseHealthCheck(DataverseClient dataverseService) {
        this.dataverseClient = dataverseService;
    }

    @Override
    protected Result check() {
        try {
            dataverseClient.checkConnection();
            return Result.healthy();
        }
        catch (IOException | DataverseException e) {
            return Result.builder()
                .withMessage(e.getMessage())
                .unhealthy(e)
                .build();
        }
    }
}
