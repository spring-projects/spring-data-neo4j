/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
package org.springframework.data.neo4j.extensions;

import java.io.Serializable;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * The class implementing our custom interface extension.
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@Repository
public class CustomGraphRepositoryImpl<T, ID extends Serializable> extends SimpleNeo4jRepository<T, ID>
		implements CustomNeo4jRepository<T, ID> {

	public CustomGraphRepositoryImpl(Class<T> clazz, Session session) {
		super(clazz, session);
	}

	@Override
	public boolean sharedCustomMethod() {
		return true;
	}
}
