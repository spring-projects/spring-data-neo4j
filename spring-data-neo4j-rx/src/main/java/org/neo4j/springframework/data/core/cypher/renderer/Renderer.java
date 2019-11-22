/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.cypher.renderer;

import org.neo4j.springframework.data.core.cypher.Statement;

/**
 * Instances of this class are supposed to be thread-safe.
 * @author Michael J. Simons
 * @since 1.0
 */
public interface Renderer {

	/**
	 * Renders a statement.
	 * @param statement the statement to render
	 * @return The rendered Cypher statement.
	 */
	String render(Statement statement);

	/**
	 * Provides the default renderer. This method may or may not provide shared instances of the renderer.
	 *
	 * @return The default renderer.
	 */
	static Renderer getDefaultRenderer() {
		return CypherRenderer.INSTANCE;
	}
}
