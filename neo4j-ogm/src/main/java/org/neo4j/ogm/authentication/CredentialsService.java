/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple class that should ultimately support multiple strategies for loading
 * authorisation credentials to access Neo4j 2.2 and later
 *
 *
 */
public class CredentialsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsService.class);

    /**
     * We expect user-name and password credentials to be set via command-line switches
     * i.e. System properties, rather than via a properties file. In the future,
     * we should support different mechanisms for loading credentials.
     *
     * At the moment, if a user defines credentials, they will be sent with each request
     * regardless of whether the Neo4j server can handle them or not.
     *
     * @return A Neo4jCredentials object, or null if credentials were not supplied, or
     * were incomplete.
     */
    public static Neo4jCredentials<?> userNameAndPassword() {

        String userName = System.getProperty("username");
        String password = System.getProperty("password");

        if (userName != null) {
            if (password != null) {
                LOGGER.info("Using credentials supplied");
                return new UsernamePasswordCredentials(userName, password);
            }
            else {
                LOGGER.warn("Incomplete credentials supplied");
            }
        } else {
            LOGGER.warn("No credentials supplied");
        }

        return null; // valid for Neo4j versions < 2.2
    }

    /**
     * The Auth-Token mechanism in 2.2 M02 has been removed in M04. It may yet return!
     * @return A Neo4jCredentials object, or null if credentials were not supplied, or
     * were incomplete.
     */
    public static Neo4jCredentials<?> authToken() {

        String token = System.getProperty("neo4j.auth");

        if (token != null) {
            return new AuthTokenCredentials(token);
        } else {
            LOGGER.warn("No credentials supplied");
        }

        return null; // valid for Neo4j versions < 2.2
    }


}
