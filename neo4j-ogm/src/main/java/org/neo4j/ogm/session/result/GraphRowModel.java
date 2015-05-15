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

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.model.GraphModel;

/**
 * Created by luanne on 15/05/15.
 */
public class GraphRowModel {

	List<GraphRowResult> graphRowResults = new ArrayList<>();

	public List<GraphRowResult> getGraphRowResults() {
		return graphRowResults;
	}

	public void addGraphRowResult(GraphModel graphModel, Object[] rowModel) {
		graphRowResults.add(new GraphRowResult(graphModel, rowModel));
	}
}
