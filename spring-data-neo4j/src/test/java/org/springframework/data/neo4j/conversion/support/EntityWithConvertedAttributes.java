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
package org.springframework.data.neo4j.conversion.support;

import java.util.List;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.Convert;

@NodeEntity
public class EntityWithConvertedAttributes {
	@Id @GeneratedValue private Long id;

	private String name;

	@Property
	@Convert(Converters.ConvertedClassToStringConverter.class)
	private ConvertedClass convertedClass;

	@Property
	@Convert(Converters.ListToStringConverter.class)
	private List<Double> doubles;

	@Convert(Converters.DoubleToStringConverter.class)
	private Double theDouble;

	public EntityWithConvertedAttributes() {}

	public EntityWithConvertedAttributes(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setConvertedClass(ConvertedClass convertedClass) {
		this.convertedClass = convertedClass;
	}

	public void setDoubles(List<Double> doubles) {
		this.doubles = doubles;
	}

	public void setTheDouble(Double theDouble) {
		this.theDouble = theDouble;
	}
}
