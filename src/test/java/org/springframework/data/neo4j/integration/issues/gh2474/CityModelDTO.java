/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2474;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Stephen Jackson
 */
@Data
public class CityModelDTO {
	private UUID cityId;
	private String name;
	private String exoticProperty;

	public PersonModelDTO mayor;
	public List<PersonModelDTO> citizens = new ArrayList<>();
	public List<JobRelationshipDTO> cityEmployees = new ArrayList<>();

	/**
	 * Nested projection
	 */
	@Data
	public static class PersonModelDTO {
		private UUID personId;
	}

	/**
	 * Nested projection
	 */
	@Data
	public static class JobRelationshipDTO {
		private PersonModelDTO person;
	}
}
