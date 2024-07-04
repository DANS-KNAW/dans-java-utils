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

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class VersionProvider {
    public String getVersion() {
        Optional<StackTraceElement> mainOpt = Arrays.stream(
            Thread.currentThread().getStackTrace()
        ).filter(stackTraceElement ->
            stackTraceElement.getMethodName().equals("main")
        ).findFirst();
        return mainOpt.map(VersionProvider::getImplementationVersion).orElse(null);
    }

    private static String getImplementationVersion(StackTraceElement stackTraceElement) {
        try {
            Class<?> aClass = Class.forName(stackTraceElement.getClassName());
            String implementationVersion = aClass.getPackage().getImplementationVersion();
            if (implementationVersion == null) {
                // executed locally with start.sh (mvn exec:java)
                // unzip -p target/XXX.jar META-INF/MANIFEST.MF | grep '^Implementation-Version'
                log.warn("MANIFEST.MF not found in jar or it did not contain a value for Implementation-Version");
            }
            return implementationVersion;
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
