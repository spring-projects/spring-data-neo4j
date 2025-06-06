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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class Flight {

	private final String name;

	@Relationship(type = "DEPARTS")
	private final Airport departure;

	@Relationship(type = "ARRIVES")
	private final Airport arrival;

	@Id
	@GeneratedValue
	private Long id;

	@Relationship("NEXT_FLIGHT")
	private Flight nextFlight;

	public Flight(String name, Airport departure, Airport arrival) {
		this.name = name;
		this.departure = departure;
		this.arrival = arrival;
	}

	public String getName() {
		return this.name;
	}

	public Airport getDeparture() {
		return this.departure;
	}

	public Airport getArrival() {
		return this.arrival;
	}

	public Flight getNextFlight() {
		return this.nextFlight;
	}

	public void setNextFlight(Flight nextFlight) {
		this.nextFlight = nextFlight;
	}

}
