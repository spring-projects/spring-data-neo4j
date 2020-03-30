/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.boot.test.autoconfigure.data;

/**
 * An exception that is thrown when a version of Neo4j is present that does not match the requirements of a test setup.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public final class Neo4jVersionMismatchException extends RuntimeException {

	/**
	 * The required version.
	 */
	private final String requiredVersion;

	/**
	 * The actual version of Neo4j persent.
	 */
	private final String actualVersion;

	public Neo4jVersionMismatchException(String message, String requiredVersion, String actualVersion) {
		super(message);
		this.requiredVersion = requiredVersion;
		this.actualVersion = actualVersion;
	}

	public String getRequiredVersion() {
		return requiredVersion;
	}

	public String getActualVersion() {
		return actualVersion;
	}
}
