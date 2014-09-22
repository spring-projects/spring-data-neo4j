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
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.rest.graphdb.query.CypherRestResult;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.traversal.RestTraverser;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.ResultConverter;

import java.util.Collection;
import java.util.Map;

/**
 * @author mh
 * @since 02.05.12
 */
public interface RestAPI extends RestAPIIndex, RestAPIInternal {

    void deleteEntity(RestEntity entity);

    void setPropertyOnEntity(RestEntity entity, String key, Object value);
    void setPropertiesOnEntity(RestEntity restEntity, Map<String, Object> propertyData);
//    Map<?,?> getData(RestEntity uri);
    void removeProperty(RestEntity entity, String key);

    RestNode getNodeById(long id);

    RestNode createNode(Map<String, Object> props);
    RestNode createNode(Map<String, Object> props,Collection<String> labels);

    RestRelationship getRelationshipById(long id);
    RestRelationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props);

    Iterable<RelationshipType> getRelationshipTypes(RestNode node);
    int getDegree(RestNode restNode, RelationshipType type, Direction direction);

    void addLabels(RestNode node, Collection<String> labels);
    void removeLabel(RestNode node, String label);

    Iterable<RestNode> getNodesByLabel(String label);
    Iterable<RestNode> getNodesByLabelAndProperty(String label, String property, Object value);

    org.neo4j.rest.graphdb.query.CypherResult query(String statement, Map<String, Object> params);
    QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params, ResultConverter resultConverter);

    Transaction beginTx();

    Collection<String> getAllLabelNames();

    Iterable<RelationshipType> getRelationshipTypes();

    TraversalDescription createTraversalDescription();

    Iterable<Relationship> getRelationships(RestNode restNode, Direction direction, RelationshipType... types);

    RestTraverser traverse(RestNode restNode, Map<String, Object> description);

    RestNode merge(String labelName, String key, Object value, Map<String, Object> properties, Collection<String> labels);

    RequestResult batch(Collection<Map<String,Object>> batchRequestData);

    // internal

    RestRequest getRestRequest();

    RestNode addToCache(RestNode restNode);
    RestNode getFromCache(long id);

    void close();
}
