/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session.result;

/**
 * Holds read only statistics about query execution.
 * The field names do not follow convention as they are mapped directly from the JSON received from the Neo4j API.
 *
 * @author Luanne Misquitta
 */
public class QueryStatistics {

	private boolean contains_updates;
	private int nodes_created;
	private int nodes_deleted;
	private int properties_set;
	private int relationships_created;
	private int relationship_deleted;
	private int labels_added;
	private int labels_removed;
	private int indexes_added;
	private int indexes_removed;
	private int constraints_added;
	private int constraints_removed;

	public boolean containsUpdates() {
		return contains_updates;
	}

	public int getNodesCreated() {
		return nodes_created;
	}

	public int getNodesDeleted() {
		return nodes_deleted;
	}

	public int getPropertiesSet() {
		return properties_set;
	}

	public int getRelationshipsCreated() {
		return relationships_created;
	}

	public int getRelationshipsDeleted() {
		return relationship_deleted;
	}

	public int getLabelsAdded() {
		return labels_added;
	}

	public int getLabelsRemoved() {
		return labels_removed;
	}

	public int getIndexesAdded() {
		return indexes_added;
	}

	public int getIndexesRemoved() {
		return indexes_removed;
	}

	public int getConstraintsAdded() {
		return constraints_added;
	}

	public int getConstraintsRemoved() {
		return constraints_removed;
	}

	void setContains_updates(boolean contains_updates) {
		this.contains_updates = contains_updates;
	}

	void setNodes_created(int nodes_created) {
		this.nodes_created = nodes_created;
	}

	void setNodes_deleted(int nodes_deleted) {
		this.nodes_deleted = nodes_deleted;
	}

	void setProperties_set(int properties_set) {
		this.properties_set = properties_set;
	}

	void setRelationships_created(int relationships_created) {
		this.relationships_created = relationships_created;
	}

	void setRelationship_deleted(int relationships_deleted) {
		this.relationship_deleted = relationships_deleted;
	}

	void setLabels_added(int labels_added) {
		this.labels_added = labels_added;
	}

	void setLabels_removed(int labels_removed) {
		this.labels_removed = labels_removed;
	}

	void setIndexes_added(int indexes_added) {
		this.indexes_added = indexes_added;
	}

	void setIndexes_removed(int indexes_removed) {
		this.indexes_removed = indexes_removed;
	}

	void setConstraints_added(int constraints_added) {
		this.constraints_added = constraints_added;
	}

	void setConstraints_removed(int constraints_removed) {
		this.constraints_removed = constraints_removed;
	}
}
