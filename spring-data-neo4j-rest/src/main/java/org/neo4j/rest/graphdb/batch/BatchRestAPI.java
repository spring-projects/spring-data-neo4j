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


import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIImpl;
import org.neo4j.rest.graphdb.RestResultException;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.converter.RestEntityPropertyRefresher;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.IndexInfo;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.util.DefaultConverter;

import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Arrays.asList;

public class BatchRestAPI {

    private final RecordingRestRequest restRequest;
    private final String baseUri;
    private final RestAPI restApi;

    public BatchRestAPI(RestAPI restApi) {
        this.baseUri = restApi.getBaseUri();
        this.restApi = restApi;
        RestEntityExtractor converter = new RestEntityExtractor(restApi);
        this.restRequest =  new RecordingRestRequest(new RestOperations(converter), restApi.getBaseUri());
    }

    public void stop() {
        restRequest.stop();
    }


    public RestNode getNodeById(long id) {
        RestOperations.RestOperation nodeRequest = restRequest.get("node/" + id);
        RestOperations.RestOperation labelsRequest = restRequest.get("node/" + id+"/labels");
        try {
            Map<Long, Object> results = executeBatchRequest();
            RestNode node = (RestNode) results.get(nodeRequest.getBatchId());
            List<String> labels = (List<String>) results.get(labelsRequest.getBatchId());
            node.setLabels(labels);
            return node;
        } catch(RestResultException rre) {
            if (rre.getMessage().contains("NodeNotFoundException")) throw new NotFoundException("Node not found: "+id);
            else throw rre;
        }
    }


//    public <T extends PropertyContainer> void addToIndex( T entity, RestIndex index,  String key, Object value ) {
//        final RestEntity restEntity = (RestEntity) entity;
//        String uri = restEntity.getUri();
//        if (value instanceof ValueContext) {
//            value = ((ValueContext)value).getCorrectValue();
//        }
//        final Map<String, Object> data = MapUtil.map("key", key, "value", value, "uri", uri);
//        restRequest.post(index.indexPath(), data);
//    }

    public Map<Long, Object> executeBatchRequest() {
        stop();
        RestOperations operations = restRequest.getOperations();
        RequestResult response = restApi.batch(createBatchRequestData(operations));
        return convertRequestResultToEntities(operations, response);
    }


    @SuppressWarnings("unchecked")
    protected Map<Long, Object> convertRequestResultToEntities(RestOperations operations, RequestResult response) {
        Object result = response.toEntity();
        if (RestResultException.isExceptionResult(result)) {
            throw new RestResultException(result);
        }
        Collection<Map<String, Object>> responseCollection = (Collection<Map<String, Object>>) result;
        Map<Long, Object> mappedObjects = new HashMap<Long, Object>(responseCollection.size());
        for (Map<String, Object> entry : responseCollection) {
            if (RestResultException.isExceptionResult(entry)) {
                throw new RestResultException(entry);
            }
            final Long batchId = getBatchId(entry);
            final RequestResult subResult = RequestResult.extractFrom(entry);
            RestOperations.RestOperation restOperation = operations.getOperation(batchId);
            Object entity = restOperation.getResultConverter().convertFromRepresentation(subResult);
            mappedObjects.put(batchId, entity);

        }
        return mappedObjects;
    }


    private Long getBatchId(Map<String, Object> entry) {
        return ((Number) entry.get("id")).longValue();
    }

    protected Collection<Map<String, Object>> createBatchRequestData(RestOperations operations) {
        Collection<Map<String, Object>> batch = new ArrayList<Map<String, Object>>();
        for (RestOperations.RestOperation operation : operations.getRecordedRequests().values()) {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("method", operation.getMethod());
            if (operation.isSameUri(baseUri)) {
                params.put("to", operation.getUri());
            } else {
                params.put("to",createOperationUri(operation));
            }
            if (operation.getData() != null) {
                params.put("body", operation.getData());
            }
            params.put("id", operation.getBatchId());
            batch.add(params);
        }
        return batch;
    }

    private String createOperationUri(RestOperations.RestOperation operation){
        String uri =  operation.getBaseUri();
        String suffix = operation.getUri();
        if (suffix.startsWith("/")){
            return uri + suffix;
        }
        return uri + "/" + suffix;
    }


    private static class BatchIndexInfo implements IndexInfo {

        @Override
        public boolean checkConfig(String indexName, Map<String, String> config) {
            return true;
        }

        @Override
        public String[] indexNames() {
            return new String[0];
        }

        @Override
        public boolean exists(String indexName) {
            return true;
        }

        @Override
        public Map<String, String> getConfig(String name) {
            return null;
        }
    }
}
