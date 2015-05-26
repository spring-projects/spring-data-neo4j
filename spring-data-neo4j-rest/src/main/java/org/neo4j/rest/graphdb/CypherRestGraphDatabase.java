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


import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.RestCypherTransactionManager;
import org.neo4j.rest.graphdb.traversal.RestTraversalDescription;
import org.neo4j.rest.graphdb.util.ResourceIterableWrapper;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class CypherRestGraphDatabase extends AbstractRemoteDatabase implements RestAPIProvider {
    private RestAPICypherImpl restAPI;

    public CypherRestGraphDatabase(RestAPI restAPI){
    	this.restAPI = new RestAPICypherImpl(restAPI);
    }

    public CypherRestGraphDatabase(String uri) {
        this( new RestAPIImpl( uri ));
    }

    public CypherRestGraphDatabase(String uri, String user, String password) {
        this(new RestAPIImpl( uri, user, password ));
    }

    public RestAPI getRestAPI(){
    	return this.restAPI;
    }

    public RestIndexManager index() {
       return this.restAPI.index();
    }

    public Node createNode() {
    	return this.restAPI.createNode(null);
    }
  
    public Node getNodeById( long id ) {
    	return this.restAPI.getNodeById(id);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return restAPI.getAllNodes();
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return this.restAPI.getRelationshipTypes();
    }

    public Relationship getRelationshipById( long id ) {
    	return this.restAPI.getRelationshipById(id);
    }    

    public String getStoreDir() {
        return restAPI.getBaseUri();
    }

    @Override
    public boolean isAvailable(long timeout) {
        return restAPI!=null;
    }

    public RestCypherTransactionManager getTxManager() {
        return restAPI.getTxManager();
    }

    public DependencyResolver getDependencyResolver() {
        return new DependencyResolver.Adapter() {
            @Override
            public <T> T resolveDependency(Class<T> type, SelectionStrategy selector) throws IllegalArgumentException {
                if (TransactionManager.class.isAssignableFrom(type)) return (T)getTxManager();
                return null;
            }
        };
    }

    @Override
    public Transaction beginTx() {
        return restAPI.beginTx();
    }

    @Override
    public void shutdown() {
        try {
            getTxManager().rollback();
        } catch (SystemException|IllegalStateException e) {
            e.printStackTrace();
            // ignore
        }
        restAPI.close();
    }

    @Override
    public Node createNode(Label... labels) {
        return restAPI.createNode(null, toLabelNames(labels));
    }

    public Set<String> toLabelNames(Label[] labels) {
        Set<String> labelNames = new LinkedHashSet<>(labels.length);
        for (Label label : labels) labelNames.add(label.name());
        return labelNames;
    }

    @Override
    public ResourceIterable<Node> findNodesByLabelAndProperty(Label label, String property, Object value) {
        Iterable<RestNode> nodes = restAPI.getNodesByLabelAndProperty(label.name(), property, value);
        return new ResourceIterableWrapper<Node,RestNode>(nodes) {
            protected Node underlyingObjectToObject(RestNode node) {
                return node;
            }
        };
    }

    @Override
    public Schema schema() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RestTraversalDescription traversalDescription() {
        return restAPI.createTraversalDescription();
    }

    @Override
    public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getAllLabelNames() {
        return restAPI.getAllLabelNames();
    }

    public ResourceIterator<Node> findNodes(Label label, String s, Object o) {
        return null;
    }

    public Node findNode(Label label, String s, Object o) {
        return null;
    }

    public ResourceIterator<Node> findNodes(Label label) {
        return null;
    }

    public Result execute(String s) {
        return null;
    }

    public Result execute(String s, Map<String, Object> map) {
        return null;
    }
}

