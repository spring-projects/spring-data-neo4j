/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.examples;

import java.util.List;

import org.springframework.data.falkordb.core.schema.RelationshipId;
import org.springframework.data.falkordb.core.schema.RelationshipProperties;
import org.springframework.data.falkordb.core.schema.TargetNode;

/**
 * Example of relationship properties entity using {@link TargetNode} annotation. This
 * class represents the ACTED_IN relationship with its properties.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@RelationshipProperties
public class ActedIn {

	@RelationshipId
	private Long id;

	@TargetNode
	private Person actor;

	private List<String> roles;

	private Integer year;

	// Default constructor
	public ActedIn() {
	}

	// Constructor
	public ActedIn(Person actor, List<String> roles, Integer year) {
		this.actor = actor;
		this.roles = roles;
		this.year = year;
	}

	// Getters and setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Person getActor() {
		return actor;
	}

	public void setActor(Person actor) {
		this.actor = actor;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

}