/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.ReflectionEntityInstantiator;

/**
 * Class to avoid direct dependency to {@link org.neo4j.ogm.metadata.reflect.ReflectionEntityInstantiator} that is only
 * present in OGM 3.1.0+
 *
 * @author Gerrit Meier
 */
class OgmReflectionEntityInstantiator extends ReflectionEntityInstantiator {
	OgmReflectionEntityInstantiator(MetaData metadata) {
		super(metadata);
	}
}
