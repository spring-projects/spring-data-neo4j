/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared;

import org.neo4j.driver.Value;
import org.neo4j.driver.internal.value.IntegerValue;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Rosetta Roberts
 */
public class CustomPersonIdConverter implements GenericConverter {
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return new HashSet<>(Arrays.asList(
				new ConvertiblePair(CustomPersonId.class, Value.class),
				new ConvertiblePair(Value.class, CustomPersonId.class)
		));
	}

	@Override
	public Object convert(Object o, TypeDescriptor type1, TypeDescriptor type2) {
		if (o == null) {
			return null;
		}

		if (CustomPersonId.class.isAssignableFrom(type1.getType())) {
			// convert to Value.
			return new IntegerValue(((CustomPersonId) o).getId());
		} else {
			return new CustomPersonId(((Value) o).asLong());
		}
	}
}
