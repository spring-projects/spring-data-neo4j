package org.springframework.datastore.graph.neo4j.partial;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.datastore.graph.annotation.GraphProperty;
import org.springframework.datastore.graph.annotation.NodeEntity;
import org.springframework.datastore.graph.annotation.RelatedTo;
import org.springframework.datastore.graph.annotation.RelatedToVia;
import org.springframework.datastore.graph.api.*;

import javax.persistence.*;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 27.09.2010
 */
@Entity
@NodeEntity(partial = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "id_gen")
    @TableGenerator(name = "id_gen", table = "SEQUENCE", pkColumnName = "SEQ_NAME", valueColumnName = "SEQ_COUNT", pkColumnValue = "SEQ_GEN", allocationSize = 1)
    private Long id;

    String name;
    int age;

    @GraphProperty
    @Transient
    String nickname;

    @RelatedToVia(type = "recommends", elementClass = Recommendation.class)
    @Transient
    Iterable<Recommendation> recommendations;

    @RelatedTo(type = "friends", elementClass = User.class)
    @Transient
    Set<User> friends;

    public Recommendation rate(Restaurant restaurant, int stars, String comment) {
        Recommendation recommendation = (Recommendation) relateTo(restaurant, Recommendation.class, "recommends");
        recommendation.rate(stars, comment);
        return recommendation;
    }
    public Iterable<Recommendation> getRecommendations() {
        return recommendations;
    }

    public Set<User> getFriends() {
        return friends;
    }

    public void setFriends(Set<User> friends) {
        this.friends = friends;
    }

    public Long getId() {
        return id;
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public void knows(User other) {
        relateTo(other, DynamicRelationshipType.withName("friends"));
    }
}
