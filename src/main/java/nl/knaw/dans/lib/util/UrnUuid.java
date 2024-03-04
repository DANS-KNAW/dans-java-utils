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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.net.URI;
import java.util.UUID;

@AllArgsConstructor
@EqualsAndHashCode
public class UrnUuid {
    private URI urn;

    public UUID getUuid() {
        return UUID.fromString(urn.getSchemeSpecificPart().substring("uuid:".length()));
    }

    public static UrnUuid fromString(String s) {
        if (!s.startsWith("urn:uuid:")) {
            throw new IllegalArgumentException("Not a URN UUID: " + s);
        }
        UUID.fromString(s.substring("urn:uuid:".length())); // throws IllegalArgumentException if not a valid UUID
        return new UrnUuid(URI.create(s));
    }

    @Override
    public String toString() {
        return urn.toString();
    }

    public String toASCIIString() {
        return urn.toASCIIString();
    }
}
