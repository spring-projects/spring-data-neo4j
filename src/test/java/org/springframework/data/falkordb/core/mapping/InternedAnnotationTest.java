/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Interned;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.mapping.model.EntityInstantiators;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Interned} annotation functionality.
 *
 * @author Shahar Biron (FalkorDB)
 * @since 1.0
 */
class InternedAnnotationTest {

	private DefaultFalkorDBMappingContext mappingContext;
	private DefaultFalkorDBEntityConverter entityConverter;

	@BeforeEach
	void setUp() {
		mappingContext = new DefaultFalkorDBMappingContext();
		entityConverter = new DefaultFalkorDBEntityConverter(mappingContext, new EntityInstantiators());
	}

	@Test
	void shouldMarkPropertyAsInterned() {
		// Given
		FalkorDBPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(TestEntity.class);
		FalkorDBPersistentProperty statusProperty = entity.getRequiredPersistentProperty("status");
		FalkorDBPersistentProperty nameProperty = entity.getRequiredPersistentProperty("name");

		// Then
		assertThat(statusProperty.isInterned()).isTrue();
		assertThat(nameProperty.isInterned()).isFalse();
	}

	@Test
	void shouldConvertInternedPropertyToInternedValue() {
		// Given
		TestEntity testEntity = new TestEntity();
		testEntity.id = "123";
		testEntity.name = "John Doe";
		testEntity.status = "ACTIVE";

		Map<String, Object> properties = new HashMap<>();

		// When
		entityConverter.write(testEntity, properties);

		// Then
		assertThat(properties).containsKey("status");
		assertThat(properties).containsKey("name");
		
		// Status should be wrapped in InternedValue
		assertThat(properties.get("status"))
			.isInstanceOf(DefaultFalkorDBEntityConverter.InternedValue.class);
		
		DefaultFalkorDBEntityConverter.InternedValue internedValue = 
			(DefaultFalkorDBEntityConverter.InternedValue) properties.get("status");
		assertThat(internedValue.getValue()).isEqualTo("ACTIVE");
		
		// Name should be a regular string
		assertThat(properties.get("name")).isInstanceOf(String.class);
		assertThat(properties.get("name")).isEqualTo("John Doe");
	}

	@Test
	void shouldEscapeSingleQuotesInInternedValue() {
		// Given
		TestEntity testEntity = new TestEntity();
		testEntity.id = "123";
		testEntity.name = "John Doe";
		testEntity.status = "O'Brien's status"; // Contains single quotes

		Map<String, Object> properties = new HashMap<>();

		// When
		entityConverter.write(testEntity, properties);

		// Then
		assertThat(properties.get("status"))
			.isInstanceOf(DefaultFalkorDBEntityConverter.InternedValue.class);
		
		DefaultFalkorDBEntityConverter.InternedValue internedValue = 
			(DefaultFalkorDBEntityConverter.InternedValue) properties.get("status");
		
		// Single quotes should be escaped
		assertThat(internedValue.getValue()).isEqualTo("O\\'Brien\\'s status");
	}

	@Test
	void shouldEscapeBackslashesAndSingleQuotesInInternedValue() {
		// Given
		TestEntity testEntity = new TestEntity();
		testEntity.id = "123";
		testEntity.name = "John Doe";
		testEntity.status = "test\\value"; // Contains backslash

		Map<String, Object> properties = new HashMap<>();

		// When
		entityConverter.write(testEntity, properties);

		// Then
		DefaultFalkorDBEntityConverter.InternedValue internedValue = 
			(DefaultFalkorDBEntityConverter.InternedValue) properties.get("status");
		
		// Backslashes should be escaped first (doubled)
		assertThat(internedValue.getValue()).isEqualTo("test\\\\value");
	}

	@Test
	void shouldEscapeBothBackslashesAndQuotesInInternedValue() {
		// Given
		TestEntity testEntity = new TestEntity();
		testEntity.id = "123";
		testEntity.name = "John Doe";
		testEntity.status = "path\\to\\'file'"; // Contains both backslashes and quotes

		Map<String, Object> properties = new HashMap<>();

		// When
		entityConverter.write(testEntity, properties);

		// Then
		DefaultFalkorDBEntityConverter.InternedValue internedValue = 
			(DefaultFalkorDBEntityConverter.InternedValue) properties.get("status");
		
		// Backslashes escaped first (doubled), then quotes escaped
		// Input: path\to\'file' -> Escape \: path\\to\\'file' -> Escape ': path\\to\\\'file\'
		assertThat(internedValue.getValue()).isEqualTo("path\\\\to\\\\\\'file\\'");
	}

	@Node("TestEntity")
	static class TestEntity {
		@Id
		String id;

		String name;

		@Interned
		String status;
	}
}
