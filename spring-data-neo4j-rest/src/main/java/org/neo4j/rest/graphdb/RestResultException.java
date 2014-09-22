/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import java.util.List;
import java.util.Map;

public class RestResultException extends RuntimeException {
    public RestResultException(Object result, String...messages) {
        super(format(toMap(result),messages));
    }

    public RestResultException(String...messages) {
        super(format(messages));
    }

    private static String format(String[] messages) {
        StringBuilder sb = new StringBuilder();
        if (messages==null || messages.length==0) return "";
        for (String s : messages) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    private static String format(Map<?, ?> result, String...messages) {
        if (result==null && (messages==null || messages.length==0)) return "Unknown Exception";
        StringBuilder sb = new StringBuilder();
        sb.append(format(messages));
        sb.append(result.get("message")).append(" at\n");
        sb.append(result.get("exception")).append("\n");
        List<String> stacktrace = (List<String>) result.get("stacktrace");
        if (stacktrace != null) {
            for (String line : stacktrace) {
                sb.append("   ").append(line).append("\n");
            }
        }
        if (result.containsKey("body")) sb.append(format(toMap(result.get("body"))));
        return sb.toString();
    }

    public static boolean isExceptionResult(Object result) {
        final Map<String, Object> map = toMap(result);
        return map!=null && (hasErrorStatus(map) || map.containsKey("exception") || map.containsKey("message") || isExceptionResult(map.get("body")));
    }

    private static boolean hasErrorStatus(Map<String, Object> map) {
        Object status = map.get("status");
        return status != null && !status.toString().startsWith("2");
    }

    private static Map<String, Object> toMap(Object result) {
        if (!(result instanceof Map)) return null;
        return (Map<String, Object>) result;

    }

    static RestResultException create(RequestResult result, String...messages) {
        if (result.isMap()) return new RestResultException(result.toMap(),messages);
        if (messages.length==0) return new RestResultException("Error executing request,status " + result.getStatus()+" message "+result.getText());
        return new RestResultException(messages);
    }
}
