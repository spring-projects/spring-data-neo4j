package org.springframework.data.neo4j.integration.movies.repo;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public interface UserRepository extends GraphRepository<User> {

    Collection<User> findByName(String name);

    Collection<User> findByMiddleName(String middleName);

    @Query("MATCH (user:User) RETURN COUNT(user)")
    int findTotalUsers();

    @Query("MATCH (user:User) RETURN user.id")
    List<Integer> getUserIds();

    @Query("MATCH (user:User) RETURN user.name, user.id")
    Iterable<Map<String,Object>> getUsersAsProperties();

    @Query("MATCH (user:User) RETURN user")
    Collection<User> getAllUsers();

    @Query("MATCH (m:Movie)<-[:ACTED_IN]-(a:User) RETURN m.title as movie, collect(a.name) as cast")
    List<Map<String, Object>> getGraph();

    @Query("MATCH (user:User{name:{name}}) RETURN user")
    User findUserByNameWithNamedParam(@Param("name") String name);

    @Query("MATCH (user:User{name:{0}}) RETURN user")
    User findUserByName(String name);
}
