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
package org.springframework.data.neo4j.integration.constructors.domain;

import java.util.Date;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.DateLong;
import org.springframework.util.Assert;

/**
 * @author Nicolas Mervaillie
 */
@NodeEntity
public class PersonWithConverter {

	@Id private String name;

	@DateLong private Date birthDate;

	public PersonWithConverter(String name, Date birthDate) {
		Assert.notNull(name, "name should not be null");
		Assert.notNull(birthDate, "birthDate should not be null");
		this.name = name;
		this.birthDate = birthDate;
	}

	public String getName() {
		return name;
	}

	public Date getBirthDate() {
		return birthDate;
	}
}
