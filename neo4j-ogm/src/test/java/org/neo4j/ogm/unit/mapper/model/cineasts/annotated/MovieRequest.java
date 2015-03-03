/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.mapper.model.cineasts.annotated;

import org.neo4j.ogm.RequestProxy;

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
