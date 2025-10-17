# Spring Data FalkorDB Annotations

This document describes the annotations available in Spring Data FalkorDB for mapping entities and defining custom queries.

## Query Annotations

### @Query

The `@Query` annotation allows you to define custom Cypher queries for repository methods that cannot be expressed as derived queries.

**Location:** `org.springframework.data.falkordb.repository.query.Query`

#### Basic Usage

```java
public interface UserRepository extends FalkorDBRepository<User, Long> {
    
    @Query("MATCH (u:User) WHERE u.age > $age RETURN u")
    List<User> findUsersOlderThan(@Param("age") int age);
}
```

#### Parameter Binding

The `@Query` annotation supports multiple parameter binding techniques:

1. **By parameter name using @Param:**
   ```java
   @Query("MATCH (u:User) WHERE u.name = $name RETURN u")
   User findByName(@Param("name") String name);
   ```

2. **By parameter index:**
   ```java
   @Query("MATCH (u:User) WHERE u.age > $0 RETURN u")
   List<User> findUsersOlderThan(int age);
   ```

3. **By entity property:**
   ```java
   @Query("MATCH (u:User {id: $user.__id__})-[:FOLLOWS]->(f) RETURN f")
   List<User> findFollowing(@Param("user") User user);
   ```

#### Query Types

**Count Queries:**
```java
@Query(value = "MATCH (u:User) WHERE u.age > $age RETURN count(u)", count = true)
Long countUsersOlderThan(@Param("age") int age);
```

**Exists Queries:**
```java
@Query(value = "MATCH (u:User {name: $name}) RETURN count(u) > 0", exists = true)
Boolean existsByName(@Param("name") String name);
```

**Write Operations:**
```java
@Query(value = "MATCH (u:User {id: $id}) SET u.lastLogin = timestamp() RETURN u", write = true)
User updateLastLogin(@Param("id") Long id);
```

#### Complex Queries

```java
@Query("MATCH (m:Movie)-[r:ACTED_IN]-(p:Person {name: $actorName}) " +
       "RETURN m, collect(r), collect(p)")
List<Movie> findMoviesByActorName(@Param("actorName") String actorName);
```

## Relationship Mapping Annotations

### @TargetNode

The `@TargetNode` annotation marks a field in a relationship properties class as the target node of the relationship.

**Location:** `org.springframework.data.falkordb.core.schema.TargetNode`

#### Usage with @RelationshipProperties

```java
@RelationshipProperties
public class ActedIn {
    
    @RelationshipId
    private Long id;
    
    @TargetNode
    private Person actor;  // The target node of the relationship
    
    private List<String> roles;  // Relationship properties
    private Integer year;        // Relationship properties
    
    // constructors, getters, setters...
}
```

#### Complete Example

**Entity Classes:**
```java
@Node
public class Movie {
    @Id
    private String title;
    
    @Relationship(type = "ACTED_IN", direction = Direction.INCOMING)
    private List<ActedIn> actors = new ArrayList<>();
    
    // other fields...
}

@Node 
public class Person {
    @Id @GeneratedValue
    private Long id;
    
    private String name;
    private Integer born;
    
    // other fields...
}
```

**Relationship Properties:**
```java
@RelationshipProperties
public class ActedIn {
    
    @RelationshipId
    private Long id;
    
    @TargetNode
    private Person actor;
    
    private List<String> roles;
    private Integer year;
}
```

### @RelationshipId

The `@RelationshipId` annotation marks a field as the relationship's internal ID.

**Location:** `org.springframework.data.falkordb.core.schema.RelationshipId`

```java
@RelationshipProperties
public class Friendship {
    
    @RelationshipId
    private Long id;  // Relationship's internal ID
    
    @TargetNode
    private Person friend;
    
    private LocalDate since;
}
```

## Repository Examples

### Complete Repository Interface

```java
public interface MovieRepository extends FalkorDBRepository<Movie, String> {

    // Derived query methods
    List<Movie> findByReleasedGreaterThan(Integer year);
    
    // Custom queries with different parameter binding styles
    @Query("MATCH (m:Movie) WHERE m.released > $year RETURN m")
    List<Movie> findMoviesReleasedAfter(@Param("year") Integer year);

    @Query("MATCH (m:Movie) WHERE m.title CONTAINS $0 RETURN m")
    List<Movie> findMoviesByTitleContaining(String titlePart);

    // Complex relationship queries
    @Query("MATCH (m:Movie {title: $title})-[r:ACTED_IN]-(p:Person) " +
           "RETURN m, collect(r), collect(p)")
    Optional<Movie> findMovieWithActors(@Param("title") String title);

    // Entity parameter queries
    @Query("MATCH (m:Movie {title: $movie.__id__})-[:ACTED_IN]-(p:Person) RETURN p")
    List<Person> findActorsInMovie(@Param("movie") Movie movie);

    // Count and exists queries
    @Query(value = "MATCH (m:Movie) WHERE m.released > $year RETURN count(m)", count = true)
    Long countMoviesReleasedAfter(@Param("year") Integer year);

    @Query(value = "MATCH (m:Movie {title: $title}) RETURN count(m) > 0", exists = true)
    Boolean existsByTitle(@Param("title") String title);

    // Write operations
    @Query(value = "MATCH (m:Movie {title: $title}) SET m.updated = timestamp() RETURN m", 
           write = true)
    Movie updateMovieTimestamp(@Param("title") String title);
}
```

## Best Practices

### Query Annotation Best Practices

1. **Use meaningful parameter names** with `@Param` for better readability
2. **Mark write operations** with `write = true` for proper transaction handling
3. **Use count/exists flags** for performance optimization on aggregate queries
4. **Avoid N+1 queries** by using `collect()` in Cypher for related data

### Relationship Properties Best Practices

1. **Always use @RelationshipId** for the relationship's internal ID field
2. **Use @TargetNode** to clearly identify the target node field
3. **Keep relationship properties simple** and avoid deep nesting
4. **Consider performance implications** of relationship properties in queries

## Migration from Spring Data Neo4j

These annotations are designed to be compatible with Spring Data Neo4j patterns:

- `@Query` works similarly to Neo4j's `@Query` annotation
- `@TargetNode` replaces Neo4j's `@TargetNode` with the same semantics
- `@RelationshipId` provides the same functionality as Neo4j's `@RelationshipId`

The main differences are:
- Package names use `falkordb` instead of `neo4j`
- FalkorDB-specific optimizations and features
- Integration with FalkorDB's graph query language