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

import io.dropwizard.util.DataSize;
import io.dropwizard.util.DataSizeUnit;
import picocli.CommandLine.ITypeConverter;

/**
 * A Picocli converter for {@link DataSizeUnit}.
 * It supports both the full enum names (e.g., "MEGABYTES") and the shorthand notation used by {@link DataSize} (e.g., "MB", "MiB").
 */
public class DataSizeUnitConverter implements ITypeConverter<DataSizeUnit> {
    @Override
    public DataSizeUnit convert(String value) throws Exception {
        try {
            return DataSizeUnit.valueOf(value.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return DataSize.parse("1" + value).getUnit();
        }
    }
}
