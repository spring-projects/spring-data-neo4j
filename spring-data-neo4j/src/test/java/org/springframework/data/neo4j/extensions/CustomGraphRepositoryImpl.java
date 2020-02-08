/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
