package org.neo4j.cineasts.domain;

import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mh
 * @since 12.03.11
 */
@NodeEntity
public class Person {
    @GraphId Long nodeId;
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
    private Set<Movie> directedMovies=new HashSet<Movie>();

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

    public Role playedIn(Neo4jOperations template,Movie movie, String roleName) {
        Role role = template.createRelationshipBetween(this, movie, Role.class, "ACTS_IN",false);
        role.setName(roleName);
        return template.save(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;
        if (nodeId == null) return super.equals(o);
        return nodeId.equals(person.nodeId);

    }

    @Override
    public int hashCode() {
        return nodeId != null ? nodeId.hashCode() : super.hashCode();
    }


}
