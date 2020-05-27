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
package org.neo4j.opencypherdsl;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;

/**
 * A container having properties. A property container must be {@link Named} with a non empty name to be able to refer
 * to properties.
 *
 * @author Andreas Berger
 * @author Michael J. Simons
 * @since 1.1
 */
@API(status = EXPERIMENTAL, since = "1.1")
public interface PropertyContainer extends Named {

	/**
	 * Creates a new {@link Property} associated with this property container. This property can be used as a lookup in
	 * other expressions. It does not add a value to the property.
	 * <p>
	 * Note: The property container does not track property creation and there is no possibility to enumerate all
	 * properties that have been created for this property container.
	 *
	 * @param name property name, must not be {@literal null} or empty.
	 * @return a new {@link Property} associated with this {@link Relationship}.
	 */
	Property property(String name);
}
