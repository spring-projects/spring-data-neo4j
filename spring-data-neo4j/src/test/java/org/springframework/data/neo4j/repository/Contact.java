package org.springframework.data.neo4j.repository;

import java.util.UUID;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Index;
import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Created by markangrish on 24/03/2017.
 */
@NodeEntity
public abstract class Contact {

	@Id @GeneratedValue protected Long id;

	@Index(unique = true) private String uuid;

	public Contact() {
		this.uuid = UUID.randomUUID().toString();
	}

	public Long getId() {
		return id;
	}

	public String getUuid() {
		return uuid;
	}
}
