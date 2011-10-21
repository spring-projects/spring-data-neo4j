package org.neo4j.cineasts.domain;

import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.Date;
import java.util.Set;

@NodeEntity
public class Person {
    @Indexed
    String id;
    @Indexed(indexType=IndexType.FULLTEXT, indexName = "people")
    String name;
    private Date birthday;
    private String birthplace;
    private String biography;
    private Integer version;
    private Date lastModified;
    private String profileImageUrl;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Person() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", name, id);
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setBirthplace(String birthplace) {
        this.birthplace = birthplace;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getBirthday() {
        return birthday;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public String getBiography() {
        return biography;
    }

    public Integer getVersion() {
        return version;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    @RelatedTo(elementClass = Movie.class, type = "DIRECTED")
    private Set<Movie> directedMovies;

    public Set<Movie> getDirectedMovies() {
        return directedMovies;
    }

    public void directed(Movie movie) {
        this.directedMovies.add(movie);
    }

    @RelatedToVia(elementClass = Role.class, type = "ACTS_IN")
    Iterable<Role> roles;

    public Iterable<Role> getRoles() {
        return roles;
    }

    public Role playedIn(Movie movie, String roleName) {
        Role role = relateTo(movie, Role.class, "ACTS_IN");
        role.setName(roleName);
        return role;
    }

    @Query(value = "start person=node({self}) match person-->movie<--co_actor return distinct co_actor")
    private Iterable<Person> coActors;

   // Co-Actors

    public Iterable<Person> getCoActors() {
        return coActors;
    }
}
