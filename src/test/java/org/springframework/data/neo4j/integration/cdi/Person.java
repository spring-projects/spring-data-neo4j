package org.springframework.data.neo4j.integration.cdi;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class Person {
	@Id @GeneratedValue
	private UUID id;

	@CreatedDate
	private LocalDate createdAt;

	private final String name;

	public Person(String name) {
		this.name = name;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LocalDate getCreatedAt() {
		return createdAt;
	}

}
