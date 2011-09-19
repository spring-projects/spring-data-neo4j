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

package org.springframework.data.neo4j;

import java.util.Date;
import java.util.Map;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.core.Direction;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;


@NodeEntity
public class Person {

    public static final String NAME_INDEX = "name_index";
    @GraphId
	private Long graphId;

    @Indexed(indexName = NAME_INDEX)
    @Size(min = 3, max = 20)
	private String name;

	@Indexed
	private String nickname;

	@Max(100)
	@Min(0)
    @Indexed
    private int age;

	private Short height;

	private transient String thought;

	private Personality personality;

	private Date birthdate;

	private Person spouse;

	private Car car;

	private DynamicProperties personalProperties;
	
	@RelatedTo
	private Person mother;

	@RelatedTo(type = "boss", direction = Direction.INCOMING)
	private Person boss;

	@RelatedToVia(type = "knows", elementClass = Friendship.class)
	private Iterable<Friendship> friendships;

    @Query(value = "start person=(%start) match (person)<-[:boss]-(boss) return boss")
    private Person bossByQuery;

    @Query(value = "start person=(%start) match (person)<-[:boss]-(boss) return boss.%property",params = {"property","name"})
    private String bossName;

    @Query(value = "start person=(%start) match (person)<-[:persons]-(team)-[:persons]->(member) return member",elementClass = Person.class)
    private Iterable<Person> otherTeamMembers;

    @Query(value = "start person=(%start) match (person)<-[:persons]-(team)-[:persons]->(member) return member.name, member.age")
    private Iterable<Map<String,Object>> otherTeamMemberData;

    public String getBossName() {
        return bossName;
    }

    public Iterable<Person> getOtherTeamMembers() {
        return otherTeamMembers;
    }

    public Iterable<Map<String, Object>> getOtherTeamMemberData() {
        return otherTeamMemberData;
    }

    public Person getBossByQuery() {
        return bossByQuery;
    }

    public Person() {
    }

    public Person(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Short getHeight() {
		return height;
	}

	public void setHeight(Short height) {
		this.height = height;
	}


	public Person getSpouse() {
		return spouse;
	}

	public void setSpouse(Person spouse) {
		this.spouse = spouse;
	}

	public Person getMother() {
		return mother;
	}

	public void setMother(Person mother) {
		this.mother = mother;
	}

	public Person getBoss() {
		return boss;
	}

	public void setBoss(Person boss) {
		this.boss = boss;
	}

	@Override
	public String toString() {
		return name;
	}

	public Iterable<Friendship> getFriendships() {
		return friendships;
	}

	public void setFriendships(Iterable<Friendship> f) {
		friendships = f;
	}

	public Friendship knows(Person p) {
        return (Friendship)relateTo(p, Friendship.class,"knows");
	}

	public void setPersonality(Personality personality) {
		this.personality = personality;
	}

	public Personality getPersonality() {
		return personality;
	}

	public void setThought(String thought) {
		this.thought = thought;
	}

	public String getThought() {
		return thought;
	}

	public Date getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}

	public long getId() {
		return graphId;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	public Car getCar() {
		return car;
	}

	public void setProperty(String key, Object value) {
		personalProperties.setProperty(key, value);
	}
	
	public Object getProperty(String key) {
		return personalProperties.getProperty(key);
	}
	
	public DynamicProperties getPersonalProperties() {
		return personalProperties;
	}
	
	public void setPersonalProperties(DynamicProperties personalProperties) {
		this.personalProperties = personalProperties;
	}
	
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

    public static Person persistedPerson(String name, int age) {
        return new Person(name,age).persist();
    }
}
