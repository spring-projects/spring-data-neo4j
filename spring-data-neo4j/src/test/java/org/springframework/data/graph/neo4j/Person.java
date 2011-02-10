package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.*;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.neo4j.Car;
import org.springframework.data.graph.neo4j.Friendship;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.Personality;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.Date;


@NodeEntity(useShortNames = false)
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

	@RelatedTo(type = "mother", direction = Direction.OUTGOING)
	private Person mother;

	@RelatedTo(type = "boss", direction = Direction.INCOMING)
	private Person boss;

	@RelatedToVia(type = "knows", elementClass = Friendship.class)
	private Iterable<Friendship> friendships;

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

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
