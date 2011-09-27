package com.springone.myrestaurants.domain;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.GraphTraversal;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Table(name = "user_account")
@NodeEntity(partial = true)
public class UserAccount {

    private String userName;

    private String firstName;

    private String lastName;

    @GraphProperty
    String nickname;

    @RelatedTo(type = "friends", elementClass = UserAccount.class)
    Set<UserAccount> friends;

    @RelatedToVia(type = "recommends", elementClass = Recommendation.class)
    Iterable<Recommendation> recommendations;

    @GraphTraversal(traversalBuilder = TopRatedRestaurantTraverser.class, elementClass = Restaurant.class)
    Iterable<Restaurant> topRatedRestaurants;

    public Collection<RatedRestaurant> getTopNRatedRestaurants(int n) {
        return new TopRatedRestaurantFinder().getTopNRatedRestaurants(this,n);
    }

    public Collection<RatedRestaurant> getTop5RatedRestaurants() {
        return getTopNRatedRestaurants(5);
    }

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "S-")
    private Date birthDate;

    public Set<UserAccount> getFriends() {
		return friends;
	}

	public void setFriends(Set<UserAccount> friends) {
		this.friends = friends;
	}

	@ManyToMany(cascade = CascadeType.ALL)
    private Set<Restaurant> favorites;

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

	public String getUserName() {
        return this.userName;
    }

	public void setUserName(String userName) {
        this.userName = userName;
    }

	public String getFirstName() {
        return this.firstName;
    }

	public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

	public String getLastName() {
        return this.lastName;
    }

	public void setLastName(String lastName) {
        this.lastName = lastName;
    }

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Date getBirthDate() {
        return this.birthDate;
    }

	public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

	public Set<Restaurant> getFavorites() {
        return this.favorites;
    }

	public void setFavorites(Set<Restaurant> favorites) {
        this.favorites = favorites;
    }

    @Transactional
    public void knows(UserAccount friend) {
        relateTo(friend, "friends");
    /*
        if (friends==null) {
            friends=new HashSet<UserAccount>();
        }
        else friends.add(friend);
    */
    }

	@Transactional
    public Recommendation rate(Restaurant restaurant, int stars, String comment) {
        Recommendation recommendation = (Recommendation) relateTo(restaurant, Recommendation.class, "recommends");
        recommendation.rate(stars, comment);
        return recommendation;
    }
    public Iterable<Recommendation> getRecommendations() {
        return recommendations;
    }

	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("UserName: ").append(getUserName()).append(", ");
        sb.append("FirstName: ").append(getFirstName()).append(", ");
        sb.append("LastName: ").append(getLastName()).append(", ");
        sb.append("BirthDate: ").append(getBirthDate()).append(", ");
        sb.append("Favorites: ").append(getFavorites() == null ? "null" : getFavorites().size());
        return sb.toString();
    }
}
