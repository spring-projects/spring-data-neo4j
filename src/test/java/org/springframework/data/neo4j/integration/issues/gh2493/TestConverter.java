/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2493;

import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

/**
 * @author Michael J. Simons
 */
public class TestConverter implements Neo4jPersistentPropertyToMapConverter<String, TestData> {

	static final String NUM = "Num";
	static final String STRING = "String";

	@Override
	public Map<String, Value> decompose(TestData property,
										Neo4jConversionService neo4jConversionService) {

		if (property == null) {
			return Map.of();
		}

		return Map.of(NUM, Values.value(property.getNum()), STRING, Values.value(property.getString()));
	}

	@Override
	public TestData compose(Map<String, Value> source,
							Neo4jConversionService neo4jConversionService) {
		TestData data = new TestData();
		if (source.get(NUM) != null) {
			data.setNum(source.get(NUM).asInt());
		}
		if (source.get(STRING) != null) {
			data.setString(source.get(STRING).asString());
		}
		return data;
	}
}
