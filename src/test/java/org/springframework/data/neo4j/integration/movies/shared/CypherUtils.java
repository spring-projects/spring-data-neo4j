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
package org.springframework.data.neo4j.integration.movies.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
public final class CypherUtils {

	private CypherUtils() {
	}

	public static void loadCypherFromResource(String resource, Session session) throws IOException {
		try (BufferedReader moviesReader = new BufferedReader(
				new InputStreamReader(CypherUtils.class.getResourceAsStream(resource)))) {
			for (String statement : moviesReader.lines().collect(Collectors.joining(" ")).split(";")) {
				session.run(statement).consume();
			}
		}
	}

}
