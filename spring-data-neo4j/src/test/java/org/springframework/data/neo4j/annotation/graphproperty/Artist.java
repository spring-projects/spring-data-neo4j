/**
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.neo4j.annotation.graphproperty;

import java.util.Date;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.IdentifiableEntity;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
@TypeAlias("ARTIST")
public class Artist extends IdentifiableEntity {

	@GraphProperty(propertyName = "first_name")
	private String firstName;

	@GraphProperty(propertyName = "second_name")
	private String secondName;
	
	@GraphProperty(propertyName = "last_name")
	private String lastName;
	
	@GraphProperty()
	private Date born;
	
	@GraphProperty(propertyName = "")
	private Date died;
	
    public Artist() {
    }

    public Artist(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Artist(String firstName, String secondName, String lastName) {
    	this(firstName, lastName);
    	this.secondName = secondName;
    }
    
    public Artist(String firstName, String secondName, String lastName, Date born, Date died) {
    	this(firstName, secondName, lastName);
    	this.born = born;
    	this.died = died;
    }
    
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getSecondName() {
		return secondName;
	}

	public void setSecondName(String secondName) {
		this.secondName = secondName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getBorn() {
		return born;
	}

	public void setBorn(Date born) {
		this.born = born;
	}

	public Date getDied() {
		return died;
	}

	public void setDied(Date died) {
		this.died = died;
	}
}
