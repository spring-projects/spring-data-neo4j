/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest;

import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 25.07.11
 */
public class RestResultException extends RuntimeException {
    public RestResultException(Map<?, ?> result) {
        super(format(result));
    }

    private static String format(Map<?, ?> result) {
        StringBuilder sb = new StringBuilder();
        sb.append(result.get("message")).append(" at\n");
        sb.append(result.get("exception")).append("\n");
        List<String> stacktrace = (List<String>) result.get("stacktrace");
        if (stacktrace != null) {
            for (String line : stacktrace) {
                sb.append("   ").append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static boolean isExceptionResult(Map<?, ?> result) {
        return result.containsKey("exception") && result.containsKey("message");
    }
}
