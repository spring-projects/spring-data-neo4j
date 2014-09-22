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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * @author mh
 * @since 10.08.12
 */
public class UserAgent {
    public static final String NEO4J_DRIVER_PROPERTY = "org.neo4j.rest.driver";

    private final String userAgent = determineUserAgent();

    private String determineUserAgent() {
        String property = System.getProperty(NEO4J_DRIVER_PROPERTY);
        if (property == null || property.trim().isEmpty()) {
            final Properties props = loadPomProperties();
            return String.format("%s/%s", props.getProperty("artifactId", "neo4j-rest-graphdb"), props.getProperty("version", "0"));
        }
        return property;
    }

    private Properties loadPomProperties() {
        final Properties props = new Properties();
        try {
            final InputStream is = getClass().getClassLoader().getResourceAsStream("/META-INF/maven/org.neo4j/neo4j-rest-graphdb/pom.properties");
            if (is!=null) {
                props.load(is);
                is.close();
            }
        } catch (Exception e) {
            // ignore
        }
        return props;
    }

    public void install(Client client) {
        client.addFilter(new ClientFilter() {
            @Override
            public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                cr.getHeaders().add("User-Agent", userAgent);
                return getNext().handle(cr);
            }
        });
    }
}
