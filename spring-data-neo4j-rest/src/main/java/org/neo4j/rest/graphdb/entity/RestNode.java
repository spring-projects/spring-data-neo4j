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
package org.neo4j.rest.graphdb.entity;

import static java.util.Arrays.asList;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.rest.graphdb.ExecutingRestRequest.encode;

import java.net.URI;
import java.util.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIInternal;
import org.neo4j.rest.graphdb.traversal.RestDirection;
import org.neo4j.rest.graphdb.util.ResourceIterableWrapper;

public class RestNode extends RestEntity implements Node {

    public RestNode( URI uri, RestAPI restApi ) {
        super( uri, restApi );
    }

    public RestNode( String uri, RestAPI restApi ) {
        super( uri, restApi );
    }

    @SuppressWarnings("unchecked")
    public RestNode( Map<?, ?> data, RestAPI restApi ) {
        super(data, restApi);
        if (data.containsKey("metadata")) {
            setLabels((Collection<String>)((Map)data.get("metadata")).get("labels"));
        }
    }

    public RestNode(long id, Collection<String> labels, Map<String, Object> restData, RestAPI facade) {
        super(id,restData,facade);
        setLabels(labels);
    }

    public static RestNode fromCypher(long id, Collection<String> labels, Map<String, Object> props, RestAPI facade) {
        Map<String, Object> restData = map("data", props, "self", RestNode.nodeUri(facade, id));//,"metadata",map("id",String.valueOf("id"),"labels",labels)
        return new RestNode(id, labels, restData, facade);
    }

    @Override
    protected void doUpdate() {
        updateFrom(restApi.getNodeById(getId(), RestAPIInternal.Load.ForceFromServer), restApi);
    }

    @Override
    public void updateFrom(RestEntity entity, RestAPI restApi) {
        super.updateFrom(entity, restApi);
        RestNode node = (RestNode) entity;
        if (node.lastLabelFetchTime > 0 && node.labels != null) {
            setLabels(node.labels);
        }
    }

    public Relationship createRelationshipTo( Node toNode, RelationshipType type ) {
    	 return this.restApi.createRelationship(this, toNode, type, null);
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return this.restApi.getRelationshipTypes(this);
    }

    @Override
    public int getDegree() {
        return this.restApi.getDegree(this, null, Direction.BOTH);
    }

    @Override
    public int getDegree(RelationshipType type) {
        return this.restApi.getDegree(this,type,Direction.BOTH);
    }

    @Override
    public int getDegree(Direction direction) {
        return this.restApi.getDegree(this,null,direction);
    }

    @Override
    public int getDegree(RelationshipType type, Direction direction) {
        return this.restApi.getDegree(this,type,direction);
    }

    public Iterable<Relationship> getRelationships() {
        return restApi.getRelationships(this, Direction.BOTH );
    }

    public Iterable<Relationship> getRelationships( RelationshipType... types ) {
        return restApi.getRelationships(this, Direction.BOTH, types);
    }

    public Iterable<Relationship> getRelationships( Direction direction ) {
        return restApi.getRelationships(this, direction);
    }

    public Iterable<Relationship> getRelationships( RelationshipType type,
                                                    Direction direction ) {
        return restApi.getRelationships(this, direction, type);
    }

    public Relationship getSingleRelationship( RelationshipType type,
                                               Direction direction ) {
        return IteratorUtil.singleOrNull( getRelationships( type, direction ) );
    }

    public boolean hasRelationship() {
        return getRelationships().iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType... types ) {
        return getRelationships( types ).iterator().hasNext();
    }

    public boolean hasRelationship( Direction direction ) {
        return getRelationships( direction ).iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType type, Direction direction ) {
        return getRelationships( type, direction ).iterator().hasNext();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, Object... rels ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction,
                               RelationshipType secondType, Direction secondDirection ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Relationship> getRelationships(final Direction direction, RelationshipType... types) {
        return new CombiningIterable<Relationship>(new IterableWrapper<Iterable<Relationship>, RelationshipType>(asList(types)) {
            @Override
            protected Iterable<Relationship> underlyingObjectToObject(RelationshipType relationshipType) {
                return getRelationships(relationshipType,direction);
            }
        });
    }

    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... types) {
        for (RelationshipType relationshipType : types) {
            if (hasRelationship(relationshipType,direction)) return true;
        }
        return false;
    }

    private Set<String> labels=null;
    private long lastLabelFetchTime = 0;

    @Override
    public void addLabel(Label label) {
        restApi.addLabels(this, Collections.singleton(label.name()));
        if (this.labels!=null) this.labels.add(label.name());
        else updateLabels();
    }

    @Override
    public void removeLabel(Label label) {
        restApi.removeLabel(this,label.name());
        if (this.labels!=null) this.labels.remove(label.name());
        else updateLabels();
    }

    @Override
    public boolean hasLabel(Label label) {
        updateLabels();
        return this.labels.contains(label.name());
    }

    private void updateLabels() {
        if (hasToUpdateLabels()) {
            doUpdate();
        }
    }

    public void setLabels(Collection<String> labels) {
        this.labels = (labels == null) ? new LinkedHashSet<String>() : new LinkedHashSet<>(labels);
        this.lastLabelFetchTime = System.currentTimeMillis();
    }

    private boolean hasToUpdateLabels() {
        return labels == null || restApi.hasToUpdate(this.lastLabelFetchTime);
    }

    @Override
    public ResourceIterable<Label> getLabels() {
        updateLabels();
        return new ResourceIterableWrapper<Label,String>(labels) {
            @Override
            protected Label underlyingObjectToObject(String s) {
                return DynamicLabel.label(s);
            }
        };
    }

    @Override
    public void addAllLabelsBatch(Collection<String> labels) {
        setLabels(labels);
        restApi.addLabels(this, labels);
    }

}
