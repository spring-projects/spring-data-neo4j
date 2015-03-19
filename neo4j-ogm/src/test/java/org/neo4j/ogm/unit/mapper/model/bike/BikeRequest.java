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

package org.neo4j.ogm.unit.mapper.model.bike;

import org.neo4j.ogm.RequestProxy;

/**
 * @author Vince Bickers
 */
public class BikeRequest extends RequestProxy {

    public String[] getResponse() {
        return jsonModel;
    }

    private static String[] jsonModel = {
            "{\"graph\": { " +
                "\"nodes\" :[ " +
                    "{\"id\" : \"15\",\"labels\" : [ \"Bike\"], \"properties\" : { \"colours\" :[\"red\", \"black\"] } }, " +
                    "{\"id\" : \"16\",\"labels\" : [ \"Wheel\", \"FrontWheel\" ],\"properties\" : {\"spokes\" : 3 } }, " +
                    "{\"id\" : \"17\",\"labels\" : [ \"Wheel\", \"BackWheel\" ],\"properties\" : {\"spokes\" : 5 } }, " +
                    "{\"id\" : \"18\",\"labels\" : [ \"Frame\" ],\"properties\" : {\"size\" : 27 } }, " +
                    "{\"id\" : \"19\",\"labels\" : [ \"Saddle\" ],\"properties\" : {\"price\" : 42.99, \"material\" : \"plastic\" } } " +
                    "], " +
                "\"relationships\": [" +
                    "{\"id\":\"141\",\"type\":\"HAS_WHEEL\",\"startNode\":\"15\",\"endNode\":\"16\",\"properties\":{ \"purchased\" : 20130917 }}, " +
                    "{\"id\":\"142\",\"type\":\"HAS_WHEEL\",\"startNode\":\"15\",\"endNode\":\"17\",\"properties\":{ \"purchased\" : 20130917 }}," +
                    "{\"id\":\"143\",\"type\":\"HAS_FRAME\",\"startNode\":\"15\",\"endNode\":\"18\",\"properties\":{ \"purchased\" : 20130917 }}," +
                    "{\"id\":\"144\",\"type\":\"HAS_SADDLE\",\"startNode\":\"15\",\"endNode\":\"19\",\"properties\":{\"purchased\" : 20130922 }} " +
                    "] " +
            "} }"
    };
}
