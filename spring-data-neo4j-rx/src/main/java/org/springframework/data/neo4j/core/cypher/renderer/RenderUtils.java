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
package org.springframework.data.neo4j.core.cypher.renderer;

import java.util.Locale;

/**
 * Helper methods dealing with the formatting of various Cypher nodes and such.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
final class RenderUtils {

	/**
	 * Escapes a symbolic name. Such a symbolic name is either used for a nodes label, the type of a relationship or a
	 * variable.
	 *
	 * @param unescapedName
	 * @return The correctly escaped name, safe to be used in statements.
	 */
	static CharSequence escapeName(String unescapedName) {
		return String.format(Locale.ENGLISH, "`%s`", unescapedName.replaceAll("`", "``"));
	}

	private RenderUtils() {
	}
}
