/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Provided via Github as reproducer for entities with dynamic labels and ids that are subject to conversion. Needed for GH-2296.
 *
 * @author Michael J. Simons
 */
@Node
public class EntityWithDynamicLabelsAndIdThatNeedsToBeConverted {
	@Id
	@GeneratedValue
	private UUID id;

	@DynamicLabels
	private Set<String> extraLabels;

	private String value;

	public EntityWithDynamicLabelsAndIdThatNeedsToBeConverted(String value) {
		setValue(value);
	}

	public void setValue(String value) {
		this.value = value;

		if (Objects.isNull(extraLabels)) {
			extraLabels = new HashSet<>();
		}
		extraLabels.add(value);
	}

	public String getValue() {
		return value;
	}

	public Set<String> getExtraLabels() {
		return extraLabels;
	}

	public UUID getId() {
		return id;
	}
}
