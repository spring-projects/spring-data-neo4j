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

package org.neo4j.ogm.unit.mapper.model.cineasts.annotated;

import org.neo4j.ogm.RequestProxy;

/**
 * @author Michal Bachman
 */
public class MovieRequest extends RequestProxy {

    public String[] getResponse() {
        return jsonModel;
    }

    private static String[] jsonModel = {
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"15\",\"labels\" : [ \"Movie\"],    \"properties\" : {\"title\" : \"Pulp Fiction\"}}, " +
                    "{\"id\" : \"16\",\"labels\" : [ \"Movie\"],    \"properties\" : {\"title\" : \"Top Gun\"}}, " +
                    "{\"id\" : \"17\",\"labels\" : [ \"Movie\"],    \"properties\" : {\"title\" : \"Django Unchained\"}}, " +
                    "{\"id\" : \"18\",\"labels\" : [ \"User\"],     \"properties\" : {\"name\" : \"Michal\"}}, " +
                    "{\"id\" : \"19\",\"labels\" : [ \"User\"],     \"properties\" : {\"name\" : \"Vince\"}}, " +
                    "{\"id\" : \"20\",\"labels\" : [ \"User\"],     \"properties\" : {\"name\" : \"Daniela\"}} " +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"141\",\"type\":\"RATED\",\"startNode\":\"18\",\"endNode\":\"15\",\"properties\":{ \"stars\" : 5, \"comment\" : \"Best Film Ever!\" }}, " +
                    "{\"id\":\"142\",\"type\":\"RATED\",\"startNode\":\"18\",\"endNode\":\"16\",\"properties\":{ \"stars\" : 3, \"comment\" : \"Overrated\" }}, " +
                    "{\"id\":\"143\",\"type\":\"RATED\",\"startNode\":\"19\",\"endNode\":\"16\",\"properties\":{ \"stars\" : 4 }} " +
                    "] " +
                    "} }"
    };
}
