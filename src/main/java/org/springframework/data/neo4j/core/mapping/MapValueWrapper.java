/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;

/**
 * A wrapper or marker for a Neo4j {@code org.neo4j.driver.internal.value.MapValue} that
 * needs to be unwrapped when used for properties. This class exists solely for projection
 * / filtering purposes: It allows the {@link DefaultNeo4jEntityConverter} to keep the
 * composite properties together as long as possible (in the form of above's
 * {@code MapValue}. Thus, the key in the {@link Constants#NAME_OF_PROPERTIES_PARAM} fits
 * the filter so that we can continue filtering after binding.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "6.1.9")
public final class MapValueWrapper {

	private final Value mapValue;

	MapValueWrapper(Value mapValue) {
		this.mapValue = mapValue;
	}

	public Value getMapValue() {
		return this.mapValue;
	}

}
