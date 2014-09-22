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
package org.neo4j.rest.graphdb.batch;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.core.MediaType;


import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.UpdatableRestResult;
import org.neo4j.rest.graphdb.batch.RestOperations.RestOperation.Methods;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.converter.RestResultConverter;
import org.neo4j.rest.graphdb.util.DefaultConverter;
import org.neo4j.rest.graphdb.util.ResultConverter;

public class RestOperations {
    private RestResultConverter resultConverter;
    private AtomicLong currentBatchId = new AtomicLong(0);
    private Map<Long, RestOperation> operations = new LinkedHashMap<Long, RestOperation>();
    private MediaType contentType;
    private MediaType acceptHeader;

    public RestOperations(){
        this.contentType = MediaType.APPLICATION_JSON_TYPE;
        this.acceptHeader = MediaType.APPLICATION_JSON_TYPE;
    }

    public RestOperations(RestResultConverter converter) {
        this.resultConverter = converter;
    }

    public RestOperation getOperation(Long batchId) {
        return operations.get(batchId);
    }

    public static class RestOperation {

        public void setConverter(RestResultConverter converter) {
            this.resultConverter = converter;
        }

        public enum Methods{
            POST,
            PUT,
            GET,
            DELETE
        }

        private Methods method;
        private Object data;
        private final String baseUri;
        private long batchId;
        private String uri;
        private MediaType contentType;
        private MediaType acceptHeader;
        private RestResultConverter resultConverter;



        public RestOperation(long batchId, Methods method, String uri, MediaType contentType, MediaType acceptHeader, Object data, String baseUri){
            this.batchId = batchId;
            this.method = method;
            this.uri = uri;
            this.contentType = contentType;
            this.acceptHeader = acceptHeader;
            this.data = data;
            this.baseUri = baseUri;
        }

        public RestResultConverter getResultConverter() {
            return resultConverter;
        }

        public Methods getMethod() {
            return method;
        }

        public Object getData() {
            return data;
        }

        public long getBatchId() {
            return batchId;
        }

        public String getUri() {
            return uri;
        }

        public MediaType getContentType() {
            return contentType;
        }

        public MediaType getAcceptHeader() {
            return acceptHeader;
        }

        public String getBaseUri() {
            return baseUri;
        }
        public boolean isSameUri(String baseUri) {
            return this.baseUri.equals(baseUri);
        }
    }

    public Map<Long,RestOperation> getRecordedRequests(){
        return this.operations;
    }

    public RestOperation record(Methods method, String path, Object data, String baseUri){
        long batchId = this.currentBatchId.incrementAndGet();
        RestOperation r = new RestOperation(batchId,method,path,this.contentType,this.acceptHeader,data,baseUri);
        r.setConverter(resultConverter);
        operations.put(batchId,r);
        return r;
    }
}
