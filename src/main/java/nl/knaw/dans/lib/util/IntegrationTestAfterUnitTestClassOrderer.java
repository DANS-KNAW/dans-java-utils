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

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

import java.util.Comparator;

/**
 * The integration tests mess up the logging facilities for the unit tests, so we run the integration tests after the unit test This requires every integration test to end with "IntegrationTest"
 */
public class IntegrationTestAfterUnitTestClassOrderer implements ClassOrderer {
    @Override
    public void orderClasses(ClassOrdererContext classOrdererContext) {
        classOrdererContext.getClassDescriptors().sort((Comparator<ClassDescriptor>) (left, right) -> {
            String l = left.getDisplayName();
            String r = right.getDisplayName();

            if (!l.endsWith("IntegrationTest") && r.endsWith("IntegrationTest")) {
                return -1;
            }
            else if (l.endsWith("IntegrationTest") && !r.endsWith("IntegrationTest")) {
                return 1;
            }

            return l.compareTo(r);
        });
    }
}
