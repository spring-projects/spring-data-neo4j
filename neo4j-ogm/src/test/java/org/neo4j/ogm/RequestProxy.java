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

package org.neo4j.ogm;

import org.neo4j.ogm.session.request.Neo4jRequest;
import org.neo4j.ogm.session.response.Neo4jResponse;

public abstract class RequestProxy implements Neo4jRequest<String> {

    protected abstract String[] getResponse();

    public Neo4jResponse<String> execute(String url, String request) {
        return new Response(getResponse());
    }

    static class Response implements Neo4jResponse<String> {

        private final String[] jsonModel;
        private int count = 0;

        public Response(String[] jsonModel) {
            this.jsonModel = jsonModel;
        }

        public String next()  {
            if (count < jsonModel.length) {
                String json = jsonModel[count];
                count++;
                return json;
            }
            return null;
        }

        @Override
        public void close() {
            // nothing to do.
        }

        @Override
        public void initialiseScan(String token) {
            // nothing to do
        }

        @Override
        public String[] columns() {
            return new String[0];
        }

        @Override
        public int rowId() {
            return count-1;
        }
    }

}
