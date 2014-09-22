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
package org.neo4j.rest.graphdb.util;

import java.util.concurrent.TimeUnit;

public class Config {
    public final static String CONFIG_PREFIX = "org.neo4j.rest.";
    public static final String CONFIG_STREAM = CONFIG_PREFIX + "stream";
    public static final String CONFIG_BATCH_TRANSACTION = CONFIG_PREFIX+"batch_transaction";
    public static final String CONFIG_LOG_REQUESTS = CONFIG_PREFIX+"logging_filter";
    public static final String WRITE_THREADS = "write_threads";

    public static int getConnectTimeout() {
        return getTimeout("connect_timeout", 30);
    }

    public static int getReadTimeout() {
        return getTimeout("read_timeout", 30);
    }

    public static boolean streamingIsEnabled() {
        return Boolean.parseBoolean(System.getProperty(CONFIG_STREAM,"true"));
    }

    public static boolean useBatchTransactions() {
        return System.getProperty(CONFIG_BATCH_TRANSACTION,"false").equalsIgnoreCase("true");
    }

    public static boolean useLoggingFilter() {
        return System.getProperty(CONFIG_LOG_REQUESTS,"false").equalsIgnoreCase("true");
    }

    private static int getTimeout(final String param, final int defaultValue) {
        return (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(System.getProperty(CONFIG_PREFIX + param, "" + defaultValue)));
    }
    
    public static int getWriterThreads() {
        return Integer.parseInt(System.getProperty(CONFIG_PREFIX + WRITE_THREADS, "" + 10));
    }
}
