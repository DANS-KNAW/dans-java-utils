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

public class CheckDigit {

    public static boolean validateMod11Two(String str) {
        if (str.length() != 16) {
            return false;
        }

        char actual = str.charAt(str.length() - 1);
        int sum = new StringBuilder(str).substring(0, str.length() - 1)
            .chars()
            // convert digit character to numerical value
            .map(c -> c - 48)
            // add the result to the previous result and multiply it by 2
            .reduce(0, (i1, i2) -> (i1 + i2) * 2);

        // apply this calculation to the total
        int check = (12 - (sum % 11)) % 11;

        // convert numerical value back to character
        char expected = check == 10 ? 'X' : (char) (check + 48);

        return expected == actual;
    }
}
