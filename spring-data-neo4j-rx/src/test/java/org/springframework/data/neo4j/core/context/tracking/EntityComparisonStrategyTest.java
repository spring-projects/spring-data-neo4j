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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;

/**
 * @author Gerrit Meier
 */
class EntityComparisonStrategyTest {

	private final GraphPropertyDescription valuePropertyDescription;

	private final GraphPropertyDescription informationPropertyDescription;

	private final GraphPropertyDescription parentPropertyDescription;

	private final NodeDescription description;

	EntityComparisonStrategyTest() {
		this.valuePropertyDescription = mock(GraphPropertyDescription.class);
		when(this.valuePropertyDescription.getFieldName()).thenReturn("value");
		when(this.valuePropertyDescription.getPropertyName()).thenReturn("value");

		this.informationPropertyDescription = mock(GraphPropertyDescription.class);
		when(this.informationPropertyDescription.getFieldName()).thenReturn("information");
		when(this.informationPropertyDescription.getPropertyName()).thenReturn("information");

		this.parentPropertyDescription = mock(GraphPropertyDescription.class);
		when(this.parentPropertyDescription.getFieldName()).thenReturn("parentValue");
		when(this.parentPropertyDescription.getPropertyName()).thenReturn("parentValue");

		this.description = mock(NodeDescription.class);
		when(this.description.getPrimaryLabel()).thenReturn("Something");
		when(this.description.getGraphProperties()).thenReturn(Arrays
			.asList(this.valuePropertyDescription, this.informationPropertyDescription,
				this.parentPropertyDescription));
		when(this.description.getUnderlyingClass()).thenReturn(Something.class);
	}

	@Test
	void trackSimplePropertyChange() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something = new Something("oldValue");

		strategy.track(description, something);

		String fieldName = "value";
		String newValue = "newValue";
		something.value = newValue;

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedEntityChangeEvents(something);

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

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedEntityChangeEvents(something);

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

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedEntityChangeEvents(something);

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

		Collection<EntityChangeEvent> changeEvents = strategy.getAggregatedEntityChangeEvents(something);

		EntityChangeEvent changeEvent = changeEvents.iterator().next();
		assertThat(changeEvent.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent.getValue()).isEqualTo(newValue);
	}

	@Test
	void trackMultipleObjects() {
		EntityComparisonStrategy strategy = new EntityComparisonStrategy();
		Something something1 = new Something("oldValue");
		Something something2 = new Something("oldValue");

		strategy.track(description, something1);
		strategy.track(description, something2);

		String fieldName = "value";
		String newValue1 = "newValue1";
		String newValue2 = "newValue2";
		something1.value = newValue1;
		something2.value = newValue2;

		Collection<EntityChangeEvent> changeEvents1 = strategy.getAggregatedEntityChangeEvents(something1);
		Collection<EntityChangeEvent> changeEvents2 = strategy.getAggregatedEntityChangeEvents(something2);

		EntityChangeEvent changeEvent1 = changeEvents1.iterator().next();
		assertThat(changeEvent1.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent1.getValue()).isEqualTo(newValue1);

		EntityChangeEvent changeEvent2 = changeEvents2.iterator().next();
		assertThat(changeEvent2.getPropertyField()).isEqualTo(fieldName);
		assertThat(changeEvent2.getValue()).isEqualTo(newValue2);
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
