/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * See <a href="https://github.com/neo4j/neo4j-java-driver/blob/4.4/driver/src/test/java/org/neo4j/driver/internal/util/ServerVersionTest.java">ServerVersionTest.java</a>
 *
 * @author Driver Team at Neo4j
 */
class ServerVersionTest {
	@Test
	void version() {
		Assertions.assertEquals(ServerVersion.version("Neo4j/dev"), ServerVersion.vInDev);
		Assertions.assertEquals(ServerVersion.version("Neo4j/4.0.0"), ServerVersion.v4_0_0);
	}

	@Test
	void shouldHaveCorrectToString() {
		Assertions.assertEquals("Neo4j/dev", ServerVersion.vInDev.toString());
		Assertions.assertEquals("Neo4j/4.0.0", ServerVersion.v4_0_0.toString());
		Assertions.assertEquals("Neo4j/3.5.0", ServerVersion.v3_5_0.toString());
		Assertions.assertEquals("Neo4j/3.5.7", ServerVersion.version("Neo4j/3.5.7").toString());
	}

	@Test
	void shouldFailToParseIllegalVersions() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> ServerVersion.version(""));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ServerVersion.version("/1.2.3"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ServerVersion.version("Neo4j1.2.3"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ServerVersion.version("Neo4j"));
	}

	@Test
	void shouldFailToCompareDifferentProducts() {
		ServerVersion version1 = ServerVersion.version("MyNeo4j/1.2.3");
		ServerVersion version2 = ServerVersion.version("OtherNeo4j/1.2.4");

		Assertions.assertThrows(IllegalArgumentException.class, () -> version1.greaterThanOrEqual(version2));
	}
}
