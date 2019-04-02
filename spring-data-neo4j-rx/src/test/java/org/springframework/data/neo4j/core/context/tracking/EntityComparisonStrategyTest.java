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
package org.springframework.data.neo4j.core.context.tracking;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.PropertyDescription;

/**
 * @author Gerrit Meier
 */
class EntityComparisonStrategyTest {

	private PropertyDescription valuePropertyDescription = new PropertyDescription("value", "value");

	private PropertyDescription informationPropertyDescription = new PropertyDescription("information", "information");

	private PropertyDescription parentPropertyDescription = new PropertyDescription("parentValue", "parentValue");

	private NodeDescription description = NodeDescription.builder().primaryLabel("Something")
			.properties(Arrays.asList(valuePropertyDescription, informationPropertyDescription, parentPropertyDescription))
			.relationships(emptyList()).underlyingClass(Something.class).build();

	@Test
	void trackSimplePropertyChange() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something = new Something("oldValue");

		strategy.track(description, something);

		String fieldName = "value";
		String newValue = "newValue";
		something.value = newValue;

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedDelta(something);

		EntityChangeEvent changeEvent = changeEvents.iterator().next();
		assertThat(changeEvent.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent.getValue()).isEqualTo(newValue);
	}

	@Test
	void trackCollectionPropertyChange() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something = new Something("oldValue");

		strategy.track(description, something);

		String fieldName = "information";
		something.information.add("additional entry");

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedDelta(something);

		EntityChangeEvent changeEvent = changeEvents.iterator().next();
		assertThat(changeEvent.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent.getValue()).isInstanceOf(Integer.class);
	}

	@Test
	void trackCollectionPropertyReorderChange() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something = new Something("blubb");
		something.information.add("entry 1");
		something.information.add("entry 2");

		strategy.track(description, something);

		something.information.sort(Comparator.reverseOrder());

		String fieldName = "information";

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedDelta(something);

		EntityChangeEvent changeEvent = changeEvents.iterator().next();
		assertThat(changeEvent.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent.getValue()).isInstanceOf(Integer.class);
	}

	@Test
	void trackParentClassPropertyChange() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something = new Something("oldValue");

		strategy.track(description, something);

		String fieldName = "parentValue";
		String newValue = "newValue";
		something.parentValue = newValue;

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedDelta(something);

		EntityChangeEvent changeEvent = changeEvents.iterator().next();
		assertThat(changeEvent.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent.getValue()).isEqualTo(newValue);
	}

	class ParentClass {
		String parentValue;
	}

	class Something extends ParentClass {
		final List<String> information = new ArrayList<>();
		String value;

		Something(String value) {
			this.value = value;
		}

		// create own equals and hashCode that should not get used in any technical parts of the dirty tracking
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Something something = (Something) o;
			return value.equals(something.value) && information.equals(something.information);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, information);
		}
	}
}
