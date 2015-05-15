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

package org.neo4j.ogm.session.result;

import org.neo4j.ogm.model.GraphModel;

/**
 * @author Luanne Misquitta
 */
public class GraphRowResult {

	private GraphModel graph;
	private Object[] row;

	public GraphRowResult(GraphModel graph, Object[] row) {
		this.graph = graph;
		this.row = row;
	}

	public GraphModel getGraph() {
		return graph;
	}
	public Object[] getRow() {
		return row;
	}


}
