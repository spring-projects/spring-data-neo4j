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

package org.neo4j.ogm.unit.mapper.model.education;

import org.neo4j.ogm.RequestProxy;

/**
 * MATCH p=(t:TEACHER)--(c) return p
 */
public class TeacherRequest extends RequestProxy {

    public String[] getResponse() {
        return jsonModel;
    }

    private static String[] jsonModel = {

            // mr thomas results
            "{\"graph\": { " +
                "\"nodes\" :[ " +
                    "{\"id\" : \"20\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mr Thomas\" } }, " +
                    "{\"id\" : \"2\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"English\" } } " +
                    "], " +
                "\"relationships\": [" +
                "{\"id\":\"202\",\"type\":\"TEACHES\",\"startNode\":\"20\",\"endNode\":\"2\",\"properties\":{}}" +
                "] " +
            "} }"
             ,
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"20\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mr Thomas\" } }, " +
                    "{\"id\" : \"3\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"Maths\" } }" +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"203\",\"type\":\"TEACHES\",\"startNode\":\"20\",\"endNode\":\"3\",\"properties\":{}}" +
                    "] " +
                    "} }"
             ,
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"20\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mr Thomas\" } }, " +
                    "{\"id\" : \"4\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"Physics\" } }" +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"204\",\"type\":\"TEACHES\",\"startNode\":\"20\",\"endNode\":\"4\",\"properties\":{}} " +
                    "] " +
                    "} }"

            ,
            // mrs roberts results

            "{\"graph\": { " +
                "\"nodes\" :[ " +
                    "{\"id\" : \"21\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mrs Roberts\" } }, " +
                    "{\"id\" : \"6\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"PE\" } }" +
                    "], " +
                "\"relationships\": [" +
                    "{\"id\":\"212\",\"type\":\"TEACHES\",\"startNode\":\"21\",\"endNode\":\"2\",\"properties\":{}}" +
                    "] " +
            "} }",
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"21\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mrs Roberts\" } }, " +
                    "{\"id\" : \"2\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"English\" } }" +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"216\",\"type\":\"TEACHES\",\"startNode\":\"21\",\"endNode\":\"6\",\"properties\":{}}" +
                    "] " +
                    "} }",
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"21\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Mrs Roberts\" } }, " +
                    "{\"id\" : \"7\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"History\" } } " +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"217\",\"type\":\"TEACHES\",\"startNode\":\"21\",\"endNode\":\"7\",\"properties\":{}} " +
                    "] " +
                    "} }",

            // miss young results
            "{\"graph\": { " +
                "\"nodes\" :[ " +
                    "{\"id\" : \"22\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Miss Young\" } }, " +
                    "{\"id\" : \"5\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"Philosophy and Ethics\" } }" +
                    "], " +
                "\"relationships\": [" +
                    "{\"id\":\"225\",\"type\":\"TEACHES\",\"startNode\":\"22\",\"endNode\":\"5\",\"properties\":{}}" +
                    "] " +
                    "} }",
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"22\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Miss Young\" } }, " +
                    "{\"id\" : \"7\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"History\" } }" +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"227\",\"type\":\"TEACHES\",\"startNode\":\"22\",\"endNode\":\"7\",\"properties\":{}}" +
                    "] " +
                    "} }",
            "{\"graph\": { " +
                    "\"nodes\" :[ " +
                    "{\"id\" : \"22\",\"labels\" : [ \"Teacher\"], \"properties\" : { \"name\" :\"Miss Young\" } }, " +
                    "{\"id\" : \"8\",\"labels\" : [ \"Course\" ],\"properties\" : {\"name\" : \"Geography\" } }" +
                    "], " +
                    "\"relationships\": [" +
                    "{\"id\":\"228\",\"type\":\"TEACHES\",\"startNode\":\"22\",\"endNode\":\"8\",\"properties\":{}}" +
                    "] " +
                    "} }"

    };


}
