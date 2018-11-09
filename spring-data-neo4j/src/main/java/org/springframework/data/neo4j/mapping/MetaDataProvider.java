/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology"
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
package org.springframework.data.neo4j.mapping;

import org.neo4j.ogm.metadata.MetaData;

/**
 * The only meta-data provider currently is a Neo4j-OGM {@link org.neo4j.ogm.session.SessionFactory} by proxy. The
 * meta-data is needed during execution of graph-repository-queries to lookup mappings etc.
 *
 * @author Michael J. Simons
 * @since 5.1.2
 */
@FunctionalInterface
public interface MetaDataProvider {
	MetaData getMetaData();
}
