/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.bookmarks;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.neo4j.integration.movies.shared.CypherUtils;

/**
 * @author Michael J. Simons
 */
public final class DatabaseInitializer implements InitializingBean {

	private final Driver driver;

	public DatabaseInitializer(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void afterPropertiesSet() {
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			CypherUtils.loadCypherFromResource("/data/movies.cypher", session);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
