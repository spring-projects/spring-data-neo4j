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
package org.springframework.data.neo4j.domain.sample;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.springframework.util.Assert;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Michael J. Simons
 */
@NodeEntity
public class SampleEntity {

	@Id @GeneratedValue protected Long id;
	private String first;
	private String second;

	protected SampleEntity() {

	}

	public SampleEntity(String first, String second) {
		Assert.notNull(first, "First must not be null!");
		Assert.notNull(second, "Second mot be null!");
		this.first = first;
		this.second = second;
	}

	public Long getId() {
		return id;
	}
}
