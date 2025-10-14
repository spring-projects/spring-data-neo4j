# Spring Data FalkorDB

Spring Data FalkorDB provides JPA-style object-graph mapping for FalkorDB, enabling developers to use familiar Spring Data patterns to work with graph databases.

## Features

- **JPA-style annotations**: Use `@Node`, `@Relationship`, `@Id`, `@Property` annotations similar to Spring Data JPA
- **Repository abstractions**: Implement `FalkorDBRepository<T, ID>` for automatic CRUD operations
- **Query method generation**: Support for `findByName`, `findByAgeGreaterThan`, etc. (planned)
- **Object-graph mapping**: Automatic conversion between Java objects and FalkorDB graph structures
- **Transaction support**: Built on Spring's transaction management
- **JFalkorDB integration**: Uses the official JFalkorDB Java client with RESP protocol

## Dependencies

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-falkordb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>com.falkordb</groupId>
    <artifactId>jfalkordb</artifactId>
    <version>0.5.1</version>
</dependency>
```

## Quick Start

### 1. Entity Mapping

Define your graph entities using annotations:

```java
@Node(labels = {"Person", "Individual"})
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    @Property("full_name")  // Maps to "full_name" property in FalkorDB
    private String name;

    private String email;
    private int age;

    @Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
    private List<Person> friends;

    @Relationship(type = "WORKS_FOR", direction = Relationship.Direction.OUTGOING)
    private Company company;

    // Constructors, getters, and setters...
}

@Node("Company")
public class Company {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String industry;

    @Property("employee_count")
    private int employeeCount;

    @Relationship(type = "EMPLOYS", direction = Relationship.Direction.INCOMING)
    private List<Person> employees;

    // Constructors, getters, and setters...
}
```

### 2. Repository Interface

Create repository interfaces extending `FalkorDBRepository`:

```java
public interface PersonRepository extends FalkorDBRepository<Person, Long> {

    Optional<Person> findByName(String name);
    
    List<Person> findByAgeGreaterThan(int age);
    
    List<Person> findByEmail(String email);
    
    Page<Person> findByAgeGreaterThan(int age, Pageable pageable);
    
    long countByAge(int age);
    
    boolean existsByEmail(String email);
}
```

### 3. Configuration

Configure the FalkorDB connection:

```java
@Configuration
@EnableFalkorDBRepositories
public class FalkorDBConfig {

    @Bean
    public FalkorDBClient falkorDBClient() {
        Driver driver = FalkorDB.driver("localhost", 6379);
        return new DefaultFalkorDBClient(driver, "social");
    }

    @Bean
    public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client, 
                                           FalkorDBMappingContext mappingContext,
                                           FalkorDBEntityConverter converter) {
        return new FalkorDBTemplate(client, mappingContext, converter);
    }
}
```

### 4. Usage in Service Classes

```java
@Service
@Transactional
public class PersonService {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private FalkorDBTemplate falkorDBTemplate;

    public Person createPerson(String name, String email) {
        Person person = new Person(name, email);
        return personRepository.save(person);
    }

    public List<Person> findYoungAdults() {
        return personRepository.findByAgeBetween(18, 30);
    }

    public List<Person> customQuery() {
        String cypher = "MATCH (p:Person)-[:KNOWS]-(friend:Person) " +
                       "WHERE p.age > $minAge RETURN p, friend";
        Map<String, Object> params = Collections.singletonMap("minAge", 25);
        return falkorDBTemplate.query(cypher, params, Person.class);
    }
}
```

## Supported Annotations

### @Node
Marks a class as a graph node entity:

```java
@Node("Person")                          // Single label
@Node(labels = {"Person", "Individual"}) // Multiple labels
@Node(primaryLabel = "Person")           // Explicit primary label
```

### @Id
Marks the entity identifier:

```java
@Id
private String customId;  // Assigned ID

@Id
@GeneratedValue
private Long id;  // FalkorDB internal ID

@Id
@GeneratedValue(UUIDStringGenerator.class)
private String uuid;  // Custom generator
```

### @Property
Maps fields to graph properties:

```java
@Property("full_name")
private String name;  // Maps to "full_name" property

private String email;  // Maps to "email" property (default)
```

### @Relationship
Maps relationships between entities:

```java
@Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
private List<Person> friends;

@Relationship(type = "WORKS_FOR", direction = Relationship.Direction.OUTGOING)
private Company company;

@Relationship(type = "EMPLOYS", direction = Relationship.Direction.INCOMING)
private List<Person> employees;
```

## Repository Methods

Spring Data FalkorDB supports JPA-style query methods:

- `findBy...`: Find entities matching criteria
- `countBy...`: Count entities matching criteria
- `existsBy...`: Check if entities exist matching criteria
- `deleteBy...`: Delete entities matching criteria

### Supported Keywords
- `findByName`: Exact match
- `findByAgeGreaterThan`: Comparison operations
- `findByAgeBetween`: Range queries
- `findByNameContaining`: String contains
- `findByNameIgnoreCase`: Case insensitive
- `findAllByOrderByNameAsc`: Sorting

## Current Implementation Status

✅ **Implemented**:
- Core annotations (@Node, @Id, @Property, @Relationship, @GeneratedValue)
- FalkorDBRepository interface with basic CRUD operations
- FalkorDBClient integration with JFalkorDB
- FalkorDBTemplate for custom queries
- Basic entity mapping infrastructure

🚧 **In Progress**:
- Complete mapping context implementation
- Entity converter with relationship traversal
- Query method name parsing
- Transaction support

📋 **Planned**:
- Spring Boot auto-configuration
- Reactive support
- Query by Example
- Auditing support
- Schema migration tools

## Connection to FalkorDB

This implementation uses [JFalkorDB](https://github.com/falkordb/jfalkordb), the official Java client for FalkorDB that communicates via RESP protocol:

```java
// Example connection setup
Driver driver = FalkorDB.driver("localhost", 6379);
Graph graph = driver.graph("social");

// The framework handles this automatically through FalkorDBClient
```

## Contributing

This is a Spring Data library for FalkorDB. To contribute:

1. Implement missing mapping context components
2. Add query method parsing
3. Enhance relationship traversal
4. Add comprehensive tests
5. Create Spring Boot starter

## License

Licensed under the Apache License 2.0, same as Spring Data projects.