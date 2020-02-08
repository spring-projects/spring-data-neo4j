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
package org.springframework.data.neo4j.examples.jsr303.domain;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author Vince Bickers
 * @author Mark Angrish
 */
@NodeEntity
public class Adult {

	@Id @GeneratedValue private Long id;

	@NotNull @Size(min = 2, max = 50) private String name;

	@Min(18) private Integer age;

	@AssertTrue private boolean votingAge;

	public Adult() {}

	public Adult(String name, Integer age) {
		this.name = name;
		this.age = age;
		this.votingAge = true;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public boolean isVotingAge() {
		return votingAge;
	}

	public void setVotingAge(boolean votingAge) {
		this.votingAge = votingAge;
	}
}
