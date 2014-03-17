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


import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.support.index.IndexType;

import javax.validation.constraints.Max;

import java.io.Serializable;
import java.util.Date;

@RelationshipEntity(useShortNames = false)
public class Friendship implements Serializable {

    private static final long serialVersionUID = 1L;

    @GraphId
    private Long id;

    public Friendship() {
	}
	
	public Friendship(Person p1, Person p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public Friendship(Person p1, Person p2, int years) {
		this.p1 = p1;
		this.p2 = p2;
		this.years = years;
	}

	@StartNode
	private Person p1;
	
	@EndNode
	private Person p2;

    @RelationshipType
    private String type;

    @Indexed(indexType = IndexType.SIMPLE)
    @Max(75)
	private int years;

	private Date firstMeetingDate;

	private DynamicProperties personalProperties;
	private transient String latestLocation;

    public Friendship(Person start, Person end, String type) {
        this.p1 = start;
        this.p2 = end;
        this.type = type;
    }

    public void setPerson1(Person p) {
		p1 = p;
	}
	
	public Person getPerson1() {
		return p1;
	}
	
	public void setPerson2(Person p) {
		p2 = p;
	}
	
	public Person getPerson2() {
		return p2;
	}
	
	public void setYears(int years) {
		this.years = years;
	}

	public int getYears() {
		return years;
	}

	public void setFirstMeetingDate(Date firstMeetingDate) {
		this.firstMeetingDate = firstMeetingDate;
	}

	public Date getFirstMeetingDate() {
		return firstMeetingDate;
	}

	public void setLatestLocation(String latestLocation) {
		this.latestLocation = latestLocation;
	}

	public String getLatestLocation() {
		return latestLocation;
	}
	
	public void setPersonalProperties(DynamicProperties personalProperties) {
		this.personalProperties = personalProperties;
	}
	
	public DynamicProperties getPersonalProperties() {
		return personalProperties;
	}

    public Long getId() {
        return id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Friendship friendship = (Friendship) o;
        if (id == null) return super.equals(o);
        return id.equals(friendship.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

}
