package org.springframework.data.neo4j.namedquery.domain;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class SampleEntityForNamedQuery {

	private Long id;

	private String name;

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
