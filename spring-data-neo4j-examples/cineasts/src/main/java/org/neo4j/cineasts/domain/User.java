package org.neo4j.cineasts.domain;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.annotation.RelatedToVia;
import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.neo4j.annotation.Indexed;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

@NodeEntity
public class User {
    private static final String SALT = "cewuiqwzie";
    public static final String FRIEND = "FRIEND";
    public static final String RATED = "RATED";
    @Indexed
    String login;
    String name;
    String password;
    String info;
    private Roles[] roles;

    public User() {
    }

    public User(String login, String name, String password, Roles... roles) {
        this.login = login;
        this.name = name;
        this.password = encode(password);
        this.roles = roles;
    }

    private String encode(String password) {
        return new Md5PasswordEncoder().encodePassword(password, SALT);
    }

    @RelatedToVia(elementClass = Rating.class, type = RATED)
    Iterable<Rating> ratings;

    @RelatedTo(elementClass = Movie.class, type = RATED)
    Set<Movie> favorites;


    @RelatedTo(elementClass = User.class, type = FRIEND, direction = Direction.BOTH)
    Set<User> friends;

    public void addFriend(User friend) {
        this.friends.add(friend);
    }

    public Rating rate(Movie movie, int stars, String comment) {
        return relateTo(movie, Rating.class, RATED).rate(stars, comment);
    }

    public Collection<Rating> getRatings() {
        return IteratorUtil.asCollection(ratings);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, login);
    }

    public String getName() {
        return name;
    }

    public Set<User> getFriends() {
        return friends;
    }

    public Roles[] getRole() {
        return roles;
    }


    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
    public void updatePassword(String old, String newPass1, String newPass2) {
        if (!password.equals(encode(old))) throw new IllegalArgumentException("Existing Password invalid");
        if (!newPass1.equals(newPass2)) throw new IllegalArgumentException("New Passwords don't match");
        this.password = encode(newPass1);
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFriend(User other) {
        return other!=null && getFriends().contains(other);
    }

    public enum Roles implements GrantedAuthority {
        ROLE_USER, ROLE_ADMIN;

        @Override
        public String getAuthority() {
            return name();
        }
    }
}
