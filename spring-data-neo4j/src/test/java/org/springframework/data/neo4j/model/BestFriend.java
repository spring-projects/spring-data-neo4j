/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.model;


import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.RelationshipType;
import org.springframework.data.neo4j.annotation.StartNode;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;

import java.io.Serializable;
import java.util.Date;

@RelationshipEntity(type = "BEST_FRIEND")
public class BestFriend implements Serializable {

    private static final long serialVersionUID = 1L;

    @GraphId
    private Long id;

    @Indexed(unique = true)
    private String secretName;

    public BestFriend() { }

	public BestFriend(Person p1, Person p2, String secretName) {
		this.p1 = p1;
		this.p2 = p2;
        this.secretName = secretName;
    }

	@StartNode
	private Person p1;

	@EndNode
	private Person p2;

	public Person getPerson1() {
		return p1;
	}

	public Person getPerson2() {
		return p2;
	}

    public String getSecretName() {
        return secretName;
    }

    public Long getId() {
        return id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BestFriend friendship = (BestFriend) o;
        if (id == null) return super.equals(o);
        return id.equals(friendship.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

}
