/*
 * Regression test for Issue #23: @Id field not populated when loading
 * entities via repository when ID is stored as a node property.
 */

package org.springframework.data.falkordb.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.mapping.model.EntityInstantiators;

/**
 * Ensures that @Id properties backed by node properties (e.g. n.id) are
 * correctly populated even when no top-level id/nodeId columns exist
 * in the record.
 */
class IdMappingFromNodePropertiesTest {

	@Test
	void shouldPopulateIdFromNodePropertiesWhenNoTopLevelIdColumns() {
		DefaultFalkorDBMappingContext mappingContext = new DefaultFalkorDBMappingContext();
		DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(mappingContext,
				new EntityInstantiators());

		Map<String, Object> props = new HashMap<>();
		props.put("id", "test-123");
		props.put("name", "John Doe");
		props.put("email", "john@example.com");

		NodeStub node = new NodeStub(props);
		FalkorDBClient.Record record = new RecordStub(node);

		Developer dev = converter.read(Developer.class, record);

		assertThat(dev).isNotNull();
		assertThat(dev.id).isEqualTo("test-123");
		assertThat(dev.name).isEqualTo("John Doe");
		assertThat(dev.email).isEqualTo("john@example.com");
	}

	@Node("Developer")
	static class Developer {
		@Id
		String id;

		String name;

		String email;
	}

	/**
	 * Simple node stub exposing properties via getProperty / getProperties,
	 * matching what DefaultFalkorDBEntityConverter expects when using
	 * reflection.
	 */
	static class NodeStub {

		private final Map<String, Object> properties;

		NodeStub(Map<String, Object> properties) {
			this.properties = properties;
		}

		public Object getProperty(String name) {
			return properties.get(name);
		}

		public Map<String, Object> getProperties() {
			return properties;
		}
	}

	/**
	 * Minimal Record stub that exposes a single node under alias "n" and no
	 * top-level id/nodeId columns, to mirror the failing scenario in Issue #23.
	 */
	static class RecordStub implements FalkorDBClient.Record {

		private final Map<String, Object> data = new HashMap<>();

		RecordStub(Object node) {
			this.data.put("n", node);
		}

		@Override
		public Object get(int index) {
			// We only care about lookups by key in this test
			if (index == 0) {
				return data.get("n");
			}
			return null;
		}

		@Override
		public Object get(String key) {
			return data.get(key);
		}

		@Override
		public Iterable<String> keys() {
			return data.keySet();
		}

		@Override
		public int size() {
			return data.size();
		}

		@Override
		public Iterable<Object> values() {
			return data.values();
		}
	}
}
