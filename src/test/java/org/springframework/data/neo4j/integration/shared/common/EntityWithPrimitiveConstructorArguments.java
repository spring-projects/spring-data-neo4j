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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Reproducer class for Gh-2505
 */
@Node
public class EntityWithPrimitiveConstructorArguments {

	public final boolean someBooleanValue;

	public final int someIntValue;

	@Id
	@GeneratedValue
	public Long id;

	public EntityWithPrimitiveConstructorArguments(boolean someBooleanValue, int someIntValue) {
		this.someBooleanValue = someBooleanValue;
		this.someIntValue = someIntValue;
	}

}
