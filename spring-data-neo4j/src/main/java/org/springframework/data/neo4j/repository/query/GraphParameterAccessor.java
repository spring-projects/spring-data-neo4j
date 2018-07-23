/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.cypher.query.SortOrder;
import org.springframework.data.repository.query.ParameterAccessor;

/**
 * Class used to lookup and handle special parameters such as depth, spatial parameters...
 *
 * @author Nicolas Mervaillie
 */
public interface GraphParameterAccessor extends ParameterAccessor {

	/**
	 * Gets the loading depth value of the {@link org.springframework.data.neo4j.annotation.Depth} annotated method
	 * parameter.
	 *
	 * @return the depth value
	 */
	int getDepth();

	/**
	 * Get OGM specific sort order translated from method parameters.
	 *
	 * @return The sort order
	 */
	SortOrder getOgmSort();

	// Should probably do the same for ogm specific pagination
}
