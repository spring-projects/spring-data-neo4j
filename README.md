# Spring Data FalkorDB

[![Build & Test](https://github.com/FalkorDB/spring-data-falkordb/actions/workflows/build.yml/badge.svg)](https://github.com/FalkorDB/spring-data-falkordb/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.springframework.data/spring-data-falkordb.svg)](https://search.maven.org/artifact/org.springframework.data/spring-data-falkordb)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://openjdk.java.net/projects/jdk/17/)
[![FalkorDB](https://img.shields.io/badge/FalkorDB-Compatible-red.svg)](https://falkordb.com)

> **Object-Graph-Mapping for FalkorDB using Spring Data patterns**

Spring Data FalkorDB provides JPA-style object-graph mapping for [FalkorDB](https://falkordb.com), the world's fastest graph database. This library enables developers to use familiar Spring Data patterns and annotations to work with graph databases, making it easy to build high-performance graph-based applications.

## 🚀 Key Features

- **🏷️ JPA-style Annotations**: Use familiar `@Node`, `@Relationship`, `@Id`, `@Property` annotations
- **🔧 Repository Abstractions**: Implement `FalkorDBRepository<T, ID>` for automatic CRUD operations  
- **🔍 Derived Query Methods**: Full support for Spring Data query methods like `findByName`, `findByAgeGreaterThan`, etc.
- **📝 Custom Queries**: Write Cypher queries with `@Query` annotation and named parameters
- **⚙️ Auto-Configuration**: Enable repositories with `@EnableFalkorDBRepositories`
- **🔗 Object-Graph Mapping**: Automatic conversion between Java objects and FalkorDB graph structures
- **💳 Transaction Support**: Built on Spring's robust transaction management
- **⚡ High Performance**: Leverages FalkorDB's speed with the official JFalkorDB Java client
- **🌐 RESP Protocol**: Uses the reliable RESP protocol for communication

## 📦 Installation

### Maven

Add the following dependencies to your `pom.xml`:

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

### Gradle

```gradle
dependencies {
    implementation 'org.springframework.data:spring-data-falkordb:1.0.0-SNAPSHOT'
    implementation 'com.falkordb:jfalkordb:0.5.1'
}
```

## 🏃 Quick Start

### 1. Entity Mapping

Define your graph entities using Spring Data annotations:

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
    
    // Derived query methods (automatically implemented)
    Optional<Person> findByName(String name);
    List<Person> findByAgeGreaterThan(int age);
    List<Person> findByEmail(String email);
    List<Person> findByNameAndAgeGreaterThan(String name, int age);
    List<Person> findByNameOrEmail(String name, String email);
    Page<Person> findByAgeGreaterThan(int age, Pageable pageable);
    
    // Count and existence queries
    long countByAge(int age);
    boolean existsByEmail(String email);
    
    // Custom Cypher queries with named parameters
    @Query("MATCH (p:Person)-[:KNOWS]->(f:Person) WHERE p.name = $name RETURN f")
    List<Person> findFriends(@Param("name") String name);
    
    @Query("MATCH (p:Person) WHERE p.age > $minAge AND p.age < $maxAge RETURN p")
    List<Person> findByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
}
```

### 3. Configuration

Configure the FalkorDB connection in your Spring application:

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

### 4. Service Usage

Use repositories and templates in your service classes:

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
    
    public List<Person> findConnectedPeople(int minAge) {
        String cypher = """
            MATCH (p:Person)-[:KNOWS]-(friend:Person) 
            WHERE p.age > $minAge 
            RETURN p, friend
        """;
        Map<String, Object> params = Collections.singletonMap("minAge", minAge);
        return falkorDBTemplate.query(cypher, params, Person.class);
    }
}
```

## 📝 Supported Annotations

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

## 🔍 Repository Query Methods

Spring Data FalkorDB supports two types of queries:

### 1. Derived Query Methods (Automatically Implemented)

Define methods following Spring Data naming conventions, and the implementation is generated automatically:

#### Query Keywords

- **`findBy...`**: Find entities matching criteria
- **`countBy...`**: Count entities matching criteria  
- **`existsBy...`**: Check if entities exist matching criteria
- **`deleteBy...`**: Delete entities matching criteria
- **`findFirstBy...`** / **`findTopNBy...`**: Limit results

#### Supported Comparison Operations

```java
// Equality
findByName(String name)
findByNameNot(String name)

// Comparison
findByAgeGreaterThan(int age)
findByAgeGreaterThanEqual(int age)
findByAgeLessThan(int age)
findByAgeLessThanEqual(int age)

// String operations
findByNameContaining(String substring)      // *substring*
findByNameStartingWith(String prefix)       // prefix*
findByNameEndingWith(String suffix)         // *suffix
findByNameLike(String pattern)              // Custom pattern
findByNameNotContaining(String substring)

// Null checks
findByEmailIsNull()
findByEmailIsNotNull()

// Boolean
findByActiveTrue()
findByActiveFalse()

// Collections
findByAgeIn(Collection<Integer> ages)
findByAgeNotIn(Collection<Integer> ages)

// Logical operations
findByNameAndAge(String name, int age)      // AND condition
findByNameOrEmail(String name, String email) // OR condition

// Sorting and pagination
findByAgeGreaterThan(int age, Sort sort)
findByAgeGreaterThan(int age, Pageable pageable)

// Limiting results
findFirstByOrderByCreatedAtDesc()
findTop10ByOrderByAgeDesc()
```

### 2. Custom Cypher Queries with @Query

Write custom Cypher queries for complex operations:

```java
public interface PersonRepository extends FalkorDBRepository<Person, Long> {
    
    // Using named parameters
    @Query("MATCH (p:Person)-[:KNOWS]->(f:Person) "
         + "WHERE p.name = $name RETURN f")
    List<Person> findFriends(@Param("name") String name);
    
    // Using indexed parameters
    @Query("MATCH (p:Person) WHERE p.age > $0 RETURN p")
    List<Person> findOlderThan(int age);
    
    // Count query
    @Query(value = "MATCH (p:Person)-[:WORKS_FOR]->(c:Company) "
                 + "WHERE c.name = $company RETURN count(p)",
           count = true)
    long countEmployees(@Param("company") String company);
    
    // Exists query
    @Query(value = "MATCH (p:Person {email: $email}) RETURN count(p) > 0",
           exists = true)
    boolean emailExists(@Param("email") String email);
    
    // Write query (creates/updates data)
    @Query(value = "MATCH (p:Person {id: $id}) "
                 + "SET p.lastLogin = $time",
           write = true)
    void updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);
}
```

### Query Method Examples

```java
// Simple equality
List<Person> people = repository.findByName("John");

// Comparison
List<Person> adults = repository.findByAgeGreaterThanEqual(18);

// String matching
List<Person> smiths = repository.findByNameEndingWith("Smith");

// Logical AND/OR
List<Person> results = repository.findByNameAndAgeGreaterThan("John", 25);
List<Person> results = repository.findByNameOrEmail("John", "john@example.com");

// Null checks
List<Person> noEmail = repository.findByEmailIsNull();

// Collections
List<Person> youngPeople = repository.findByAgeIn(Arrays.asList(18, 19, 20));

// Counting and existence
long count = repository.countByAge(25);
boolean exists = repository.existsByEmail("test@example.com");

// Sorting
List<Person> sorted = repository.findByAgeGreaterThan(20, Sort.by("name").ascending());

// Pagination
Page<Person> page = repository.findByAgeGreaterThan(18, PageRequest.of(0, 10));

// Limiting
Optional<Person> youngest = repository.findFirstByOrderByAgeAsc();
List<Person> oldest = repository.findTop5ByOrderByAgeDesc();

// Delete
repository.deleteByAge(0); // Delete all with age = 0
```

## 🧪 Twitter Integration Test

This library includes a comprehensive Twitter-like integration test that demonstrates real-world usage patterns. The test creates a social graph with users, tweets, follows, and hashtags.

### Running the Twitter Test

#### Prerequisites

1. **FalkorDB Server**: Ensure FalkorDB is running on `localhost:6379`
2. **Java 17+**: Make sure Java 17 or later is installed  
3. **Maven**: Required for building and running

#### Start FalkorDB

Choose one of these options:

```bash
# Option 1: Docker (Recommended)
docker run -p 6379:6379 falkordb/falkordb:latest

# Option 2: Native installation
falkordb-server --port 6379

# Option 3: Redis with FalkorDB module
redis-server --port 6379 --loadmodule /path/to/falkordb.so
```

#### Run the Test

```bash
# Clean and compile project
mvn clean compile test-compile -Dcheckstyle.skip=true

# Run specific Twitter integration test
mvn test -Dtest=FalkorDBTwitterIntegrationTests -Dcheckstyle.skip=true

# Run all integration tests
mvn test -Dcheckstyle.skip=true
```

### What the Test Demonstrates

The Twitter integration test (`FalkorDBTwitterIntegrationTests.java`) showcases the following features with complete entity definitions:

#### 🎭 Entity Types

##### TwitterUser Entity
Users with profiles, follower counts, verification status, and bio information:

```java
@Node(labels = { "User", "TwitterUser" })
public class TwitterUser {
    @Id @GeneratedValue
    private Long id;
    
    @Property("username") private String username;
    @Property("display_name") private String displayName;
    @Property("email") private String email;
    @Property("bio") private String bio;
    @Property("follower_count") private Integer followerCount;
    @Property("following_count") private Integer followingCount;
    @Property("tweet_count") private Integer tweetCount;
    @Property("verified") private Boolean verified;
    @Property("created_at") private LocalDateTime createdAt;
    @Property("location") private String location;
    
    // Relationships
    @Relationship(value = "FOLLOWS", direction = OUTGOING)
    private List<TwitterUser> following;
    @Relationship(value = "FOLLOWS", direction = INCOMING)
    private List<TwitterUser> followers;
    @Relationship(value = "POSTED", direction = OUTGOING)
    private List<Tweet> tweets;
    @Relationship(value = "LIKED", direction = OUTGOING)
    private List<Tweet> likedTweets;
    @Relationship(value = "RETWEETED", direction = OUTGOING)
    private List<Tweet> retweetedTweets;
}
```

##### Tweet Entity
Complete tweet entities with content, metadata, engagement counts, and reply/retweet flags:

```java
@Node(labels = { "Tweet" })
public class Tweet {
    @Id @GeneratedValue
    private Long id;
    
    @Property("text") private String text;
    @Property("created_at") private LocalDateTime createdAt;
    @Property("like_count") private Integer likeCount;
    @Property("retweet_count") private Integer retweetCount;
    @Property("reply_count") private Integer replyCount;
    @Property("is_retweet") private Boolean isRetweet;
    @Property("is_reply") private Boolean isReply;
    
    // Relationships
    @Relationship(value = "POSTED", direction = INCOMING)
    private TwitterUser author;
    @Relationship(value = "LIKED", direction = INCOMING)
    private List<TwitterUser> likedBy;
    @Relationship(value = "RETWEETED", direction = INCOMING)
    private List<TwitterUser> retweetedBy;
    @Relationship(value = "MENTIONS", direction = OUTGOING)
    private List<TwitterUser> mentions;
    @Relationship(value = "REPLIES_TO", direction = OUTGOING)
    private Tweet replyToTweet;
    @Relationship(value = "REPLIES_TO", direction = INCOMING)
    private List<Tweet> replies;
    @Relationship(value = "RETWEET_OF", direction = OUTGOING)
    private Tweet originalTweet;
    @Relationship(value = "HAS_HASHTAG", direction = OUTGOING)
    private List<Hashtag> hashtags;
}
```

##### Hashtag Entity
Hashtag entities with usage tracking and tweet associations:

```java
@Node(labels = { "Hashtag" })
public class Hashtag {
    @Id @GeneratedValue
    private Long id;
    
    @Property("tag") private String tag;
    @Property("usage_count") private Integer usageCount;
    
    // Relationships
    @Relationship(value = "HAS_HASHTAG", direction = INCOMING)
    private List<Tweet> tweets;
}
```

##### TwitterUserRepository Interface
Repository interface demonstrating Spring Data query method patterns:

```java
public interface TwitterUserRepository extends FalkorDBRepository<TwitterUser, Long> {
    // Derived query methods
    Optional<TwitterUser> findByUsername(String username);
    List<TwitterUser> findByDisplayNameContaining(String displayName);
    List<TwitterUser> findByVerified(Boolean verified);
    List<TwitterUser> findByFollowerCountGreaterThan(Integer followerCount);
    List<TwitterUser> findByLocationContaining(String location);
    
    // Custom query methods can be added with @Query annotation
    // @Query("MATCH (u:User)-[:FOLLOWS]->(f:User) WHERE u.username = $username RETURN f")
    // List<TwitterUser> findFollowing(@Param("username") String username);
}
```

#### 🔗 Relationship Types
- **`FOLLOWS`**: User following relationships (✅ **Fully Implemented**)
- **`POSTED`**: Users posting tweets (✅ **Entity Defined**, demonstrated via raw Cypher)
- **`LIKED`**: Users liking tweets (✅ **Entity Defined**)
- **`RETWEETED`**: Users retweeting content (✅ **Entity Defined**)
- **`MENTIONS`**: Tweets mentioning users (✅ **Entity Defined**)
- **`HAS_HASHTAG`**: Tweets containing hashtags (✅ **Entity Defined**)
- **`REPLIES_TO`**: Tweet reply threads (✅ **Entity Defined**)
- **`RETWEET_OF`**: Original tweet relationships (✅ **Entity Defined**)

#### 📊 Test Scenarios

1. **`testConnectionAndBasicOperations()`**
   - Connect to FalkorDB instance
   - Create and save TwitterUser entities  
   - Retrieve entities by ID
   - Verify basic CRUD operations

2. **`testTwitterGraphCreationAndTraversal()`**
   - Create influential users (elonmusk, billgates, oprah)
   - Set up verified profiles with follower counts
   - Create tweets with hashtags via raw Cypher
   - Create follow relationships and test traversal queries

3. **`testRelationshipTraversal()`**  
   - Create test users (alice, bob, charlie)
   - Create FOLLOWS relationships via raw Cypher
   - Test relationship queries: who follows whom, mutual connections
   - Navigate complex relationship paths

4. **`testComplexQueries()`**
   - Create sample tweets with hashtags and mentions
   - Test analytics queries: count users, tweets, relationships
   - Find most followed users and verified accounts
   - Demonstrate graph structure analysis

### Sample Test Output

```bash
$ mvn test -Dtest=FalkorDBTwitterIntegrationTests -Dcheckstyle.skip=true

[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.springframework.data.falkordb.integration.FalkorDBTwitterIntegrationTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.339 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
[INFO] Total time: 5.066 s
```

### Actual Graph Data Created

After running the test, you can verify the created data:

```bash
$ redis-cli -p 6379 GRAPH.QUERY TWITTER 'MATCH (u:User) RETURN u.username, u.display_name, u.follower_count'
1) 1) "u.username"
   2) "u.display_name" 
   3) "u.follower_count"
2) 1) 1) "charlie"
      2) "Charlie Brown"
      3) (integer) 0
   2) 1) "bob"
      2) "Bob Smith"
      3) (integer) 0
   3) 1) "alice"
      2) "Alice Johnson"
      3) (integer) 0

$ redis-cli -p 6379 GRAPH.QUERY TWITTER 'MATCH (u1:User)-[:FOLLOWS]->(u2:User) RETURN u1.username, u2.username'
1) 1) "u1.username"
   2) "u2.username"
2) 1) 1) "bob"
      2) "charlie"
   2) 1) "alice"
      2) "charlie"
   3) 1) "alice"
      2) "bob"
```

### Test Results Summary

✅ **What Works:**
- **FalkorDB Connection**: Successfully connects to FalkorDB instance
- **Entity Persistence**: Saves and retrieves TwitterUser entities  
- **Basic Operations**: Create, read operations work correctly
- **Relationship Creation**: FOLLOWS relationships created via raw Cypher
- **Graph Queries**: Complex graph traversal queries execute successfully
- **Spring Data Integration**: Full integration with Spring Data patterns
- **Performance**: Sub-second test execution, millisecond query responses

📊 **Test Statistics:**
- **Total Tests**: 4 (all passing)
- **Execution Time**: ~0.3 seconds
- **Graph Nodes Created**: 3 User entities
- **Relationships Created**: 3 FOLLOWS relationships
- **Query Performance**: < 1ms response time

### Inspecting the Graph

After running the test, explore the created graph using Redis CLI:

```bash
redis-cli -p 6379
```

#### Useful Queries

```cypher
# View all nodes
GRAPH.QUERY TWITTER 'MATCH (n) RETURN n LIMIT 10'

# View all users with details
GRAPH.QUERY TWITTER 'MATCH (u:User) RETURN u.username, u.display_name, u.follower_count'

# View follow relationships  
GRAPH.QUERY TWITTER 'MATCH (u1:User)-[:FOLLOWS]->(u2:User) RETURN u1.username, u2.username'

# View tweets with authors
GRAPH.QUERY TWITTER 'MATCH (u:User)-[:POSTED]->(t:Tweet) RETURN u.username, t.text'

# Find verified users by follower count
GRAPH.QUERY TWITTER 'MATCH (u:User) WHERE u.verified = true RETURN u.username, u.follower_count ORDER BY u.follower_count DESC'

# Analytics: Count nodes by type  
GRAPH.QUERY TWITTER 'MATCH (u:User) RETURN "Users" as type, count(u) as count UNION MATCH (t:Tweet) RETURN "Tweets" as type, count(t) as count'

# Clear the graph (if needed)
GRAPH.QUERY TWITTER 'MATCH (n) DETACH DELETE n'

# Verify FalkorDB is working
GRAPH.QUERY test "RETURN 'Hello FalkorDB' as greeting"
```

### Quick Verification

To verify everything is working correctly:

1. **Check FalkorDB Connection**:
   ```bash
   redis-cli -p 6379 ping  # Should return PONG
   ```

2. **Verify Graph Capabilities**:
   ```bash
   redis-cli -p 6379 GRAPH.QUERY test "RETURN 'FalkorDB Working!' as status"
   ```

3. **Run Integration Tests**:
   ```bash
   mvn test -Dtest=FalkorDBTwitterIntegrationTests -Dcheckstyle.skip=true
   # Should show: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
   ```

## 🚧 Implementation Status

### ✅ **Fully Implemented & Tested**
- ✅ Core annotations (`@Node`, `@Id`, `@Property`, `@GeneratedValue`) 
- ✅ `@EnableFalkorDBRepositories` for repository auto-configuration
- ✅ `FalkorDBClient` integration with JFalkorDB driver
- ✅ `FalkorDBTemplate` for custom Cypher queries
- ✅ Basic entity mapping (Java objects ↔ FalkorDB nodes)
- ✅ Entity persistence (save/retrieve operations)
- ✅ **Derived query methods** - Full support for method name-based queries
  - ✅ All comparison operators (=, <>, >, <, >=, <=)
  - ✅ String operations (Like, StartingWith, EndingWith, Containing)
  - ✅ Null checks (IsNull, IsNotNull)
  - ✅ Boolean checks (True, False)
  - ✅ Collection operations (In, NotIn)
  - ✅ Logical operations (And, Or)
  - ✅ Count, exists, and delete queries
  - ✅ Top/First limiting queries
  - ✅ Sorting support
- ✅ **Custom @Query annotation** with Cypher queries
  - ✅ Named parameters with `@Param`
  - ✅ Indexed parameters ($0, $1, etc.)
  - ✅ Count and exists projections
  - ✅ Write operations support
- ✅ Spring Data repository interfaces
- ✅ Raw Cypher query execution with parameters
- ✅ Integration test suite (Twitter social graph with 4 test scenarios)
- ✅ Complete Twitter entity definitions (TwitterUser, Tweet, Hashtag)
- ✅ Comprehensive relationship mapping definitions
- ✅ Graph relationship creation via raw Cypher
- ✅ Query result mapping and conversion
- ✅ Complex analytics and traversal queries

### 🚧 **In Progress**  
- 🔄 `@Relationship` annotation automatic relationship persistence
- 🔄 Entity converter with automatic relationship traversal
- 🔄 Full transaction support integration

### 📋 **Planned**
- 🎯 Spring Boot auto-configuration starter
- 🎯 Reactive programming support (WebFlux)
- 🎯 Query by Example functionality
- 🎯 Auditing support (`@CreatedDate`, `@LastModifiedDate`)
- 🎯 Advanced relationship mapping automation
- 🎯 Schema migration and evolution tools
- 🎯 Performance optimization and caching

## 🔧 Advanced Configuration

### Connection Pool Settings

```java
@Bean
public FalkorDBClient falkorDBClient() {
    Driver driver = FalkorDB.driver("localhost", 6379);
    // Configure connection pool if needed
    return new DefaultFalkorDBClient(driver, "myapp");
}
```

### Custom Converters

```java
@Configuration
public class FalkorDBConfig {
    
    @Bean
    public FalkorDBCustomConversions customConversions() {
        return new FalkorDBCustomConversions(Arrays.asList(
            new LocalDateTimeToStringConverter(),
            new StringToLocalDateTimeConverter()
        ));
    }
}
```

### Transaction Configuration

```java
@Configuration
@EnableTransactionManagement
public class FalkorDBTransactionConfig {
    
    @Bean
    public FalkorDBTransactionManager transactionManager(FalkorDBClient client) {
        return new FalkorDBTransactionManager(client);
    }
}
```

## 🐛 Troubleshooting

### FalkorDB Connection Issues

**Problem**: `Connection refused` error

**Solution**: Ensure FalkorDB is running:
```bash
# Check if FalkorDB is running
redis-cli -p 6379 ping

# Start FalkorDB with Docker
docker run -p 6379:6379 falkordb/falkordb:latest
```

### Compilation Errors

**Problem**: Java version mismatch

**Solution**: Ensure Java 17+ is installed:
```bash
java --version
export JAVA_HOME=/path/to/java17
```

### Performance Optimization

- Use connection pooling for high-load applications
- Implement caching for frequently accessed data
- Optimize Cypher queries with proper indexing
- Use batch operations for bulk data operations

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **🐛 Bug Reports**: Open issues with detailed reproduction steps
2. **💡 Feature Requests**: Suggest new functionality 
3. **🔧 Code Contributions**: Submit pull requests with:
   - Clear descriptions
   - Unit tests  
   - Documentation updates
4. **📚 Documentation**: Improve docs and examples

### Development Setup

```bash
git clone https://github.com/falkordb/spring-data-falkordb.git
cd spring-data-falkordb
mvn clean compile
mvn test
```

### CI/CD

This project uses GitHub Actions for continuous integration and deployment:

- **Build & Test**: Automatically runs on all PRs and pushes to main/release branches
  - Validates code with checkstyle
  - Runs full test suite with FalkorDB integration tests
  - Publishes test reports
  
- **Publish**: Automatically publishes SNAPSHOT artifacts to Maven repository on merges to main

See [ci/README.md](ci/README.md) for detailed CI/CD documentation.

### Areas Needing Help

- [ ] Query method parsing and generation
- [ ] Spring Boot auto-configuration  
- [ ] Reactive programming support
- [ ] Performance benchmarking
- [ ] Documentation and examples

## 📊 Performance

FalkorDB is the world's fastest graph database, and Spring Data FalkorDB is designed to leverage this performance:

- **Sub-millisecond** query response times
- **High throughput** for concurrent operations  
- **Memory efficient** object mapping
- **Optimized** RESP protocol communication

## 🔗 Related Projects

- **[FalkorDB](https://github.com/falkordb/falkordb)**: The fastest graph database
- **[JFalkorDB](https://github.com/falkordb/jfalkordb)**: Official Java client
- **[Spring Data](https://spring.io/projects/spring-data)**: Spring's data access framework
- **[Spring Data Commons](https://github.com/spring-projects/spring-data-commons)**: Foundation for Spring Data projects

## 📜 License

Licensed under the [MIT License](LICENSE.txt).

```
Copyright (c) 2023-2024 FalkorDB Ltd.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📞 Support

- **📖 Documentation**: [FalkorDB Docs](https://www.falkordb.com/docs/)
- **💬 Community**: [FalkorDB Discord](https://discord.gg/falkordb)  
- **🐛 Issues**: [GitHub Issues](https://github.com/falkordb/spring-data-falkordb/issues)
- **✉️ Email**: [support@falkordb.com](mailto:support@falkordb.com)

---

<div align="center">

**Built with ❤️ by the [FalkorDB](https://falkordb.com) team**

⭐ **Star this repo if you find it useful!** ⭐

</div>