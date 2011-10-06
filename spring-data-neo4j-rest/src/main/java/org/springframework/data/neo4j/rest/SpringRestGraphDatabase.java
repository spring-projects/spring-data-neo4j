/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.rest;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.rest.graphdb.*;
import org.neo4j.rest.graphdb.RestRequest;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.query.ConversionServiceQueryResultConverter;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;

public class SpringRestGraphDatabase extends org.neo4j.rest.graphdb.RestGraphDatabase implements GraphDatabase{
    private ConversionService conversionService;
    public SpringRestGraphDatabase( RestAPI api){
    	super(api);
    }

    public SpringRestGraphDatabase( String uri ) {
        this( new ExecutingRestRequest( uri ));
    }

    public SpringRestGraphDatabase( String uri, String user, String password ) {
        this(new ExecutingRestRequest( uri, user, password ));
    }

    public SpringRestGraphDatabase( RestRequest restRequest){
    	this(new RestAPI(restRequest));
    }

    @Override
    public Node createNode(Map<String, Object> props) {
        return super.getRestAPI().createNode(props);
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props) {
       return super.getRestAPI().createRelationship(startNode, endNode, type, props);
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        return super.getRestAPI().getIndex(indexName);
    }

    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
       return super.getRestAPI().createIndex(type, indexName, fullText);
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return super.getRestAPI().createTraversalDescription();
    }

    @Override
    public <T> QueryEngine<T> queryEngineFor(QueryType type) {
      /** switch (type) {
            case Cypher: return new RestCypherQueryEngine(this, createResultConverter());
            case Gremlin: return new RestGremlinQueryEngine(this, createResultConverter());
        }
        throw new IllegalArgumentException("Unknown Query Engine Type "+type);    */
        return null;
    }

    @Override
    public void setConversionService(ConversionService conversionService) {
      this.conversionService = conversionService;
    }

    private ResultConverter createResultConverter() {
        if (conversionService==null) return null;
        return new ConversionServiceQueryResultConverter(conversionService);
    }

}
