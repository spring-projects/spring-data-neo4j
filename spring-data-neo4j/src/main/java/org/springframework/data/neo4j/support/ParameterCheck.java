/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.support;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * @author mh
 * @since 17.10.11
 */
public class ParameterCheck {
    public static void notNull(Object value, String msg) {
        if (value==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg + " is required; it must not be null");
    }
    public static void notNull(Object value, String msg,Object value2, String msg2) {
        if (value==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg + " is required; it must not be null");
        if (value2==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg2 + " is required; it must not be null");
    }
    public static void notNull(Object value, String msg,Object value2, String msg2,Object value3, String msg3) {
        if (value==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg + " is required; it must not be null");
        if (value2==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg2 + " is required; it must not be null");
        if (value3==null) throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + msg3 + " is required; it must not be null");
    }
    public static void notNull(Object... pairs) {
        assert pairs.length % 2 == 0 : "wrong number of pairs to check";
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null) {
                throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + pairs[i + 1] + " is required; it must not be null");
            }
        }
    }
}
