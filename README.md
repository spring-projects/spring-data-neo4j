# Spring Data FalkorDB

[![Maven Central](https://img.shields.io/maven-central/v/org.springframework.data/spring-data-falkordb.svg)](https://search.maven.org/artifact/org.springframework.data/spring-data-falkordb)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://openjdk.java.net/projects/jdk/17/)
[![FalkorDB](https://img.shields.io/badge/FalkorDB-Compatible-red.svg)](https://falkordb.com)

> **Object-Graph-Mapping for FalkorDB using Spring Data patterns**

Spring Data FalkorDB provides JPA-style object-graph mapping for [FalkorDB](https://falkordb.com), the world's fastest graph database. This library enables developers to use familiar Spring Data patterns and annotations to work with graph databases, making it easy to build high-performance graph-based applications.

## 🚀 Key Features

- **🏷️ JPA-style Annotations**: Use familiar `@Node`, `@Relationship`, `@Id`, `@Property` annotations
- **🔧 Repository Abstractions**: Implement `FalkorDBRepository<T, ID>` for automatic CRUD operations  
- **🔍 Query Method Generation**: Support for `findByName`, `findByAgeGreaterThan`, etc.
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
    
    Optional<Person> findByName(String name);
    
    List<Person> findByAgeGreaterThan(int age);
    
    List<Person> findByEmail(String email);
    
    Page<Person> findByAgeGreaterThan(int age, Pageable pageable);
    
    long countByAge(int age);
    
    boolean existsByEmail(String email);
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

Spring Data FalkorDB supports JPA-style query methods with these patterns:

### Query Keywords

- **`findBy...`**: Find entities matching criteria
- **`countBy...`**: Count entities matching criteria  
- **`existsBy...`**: Check if entities exist matching criteria
- **`deleteBy...`**: Delete entities matching criteria

### Supported Operations

```java
// Exact match
findByName(String name)

// Comparison operations  
findByAgeGreaterThan(int age)
findByAgeGreaterThanEqual(int age)
findByAgeLessThan(int age)

// Range queries
findByAgeBetween(int start, int end)

// String operations
findByNameContaining(String substring)
findByNameStartingWith(String prefix)
findByNameIgnoreCase(String name)

// Sorting and pagination
findAllByOrderByNameAsc()
findByAgeGreaterThan(int age, Pageable pageable)

// Logical operations
findByNameAndAge(String name, int age)
findByNameOrEmail(String name, String email)
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
# Automated script (recommended)
./run-falkordb-test.sh

# Manual execution
mvn compile -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true
mvn exec:java -Dexec.mainClass="org.springframework.data.falkordb.integration.FalkorDBTwitterIntegrationTest" -Dexec.classpathScope="test"

# JUnit test runner
mvn test -Dtest=FalkorDBTwitterIntegrationTest
```

### What the Test Demonstrates

The Twitter integration test showcases the following features:

#### 🎭 Entity Types
- **TwitterUser**: Users with profiles, follower counts, verification status
- **Tweet**: Tweets with content, timestamps, engagement metrics  
- **Hashtag**: Hashtags with usage tracking and trends

#### 🔗 Relationship Types
- **`FOLLOWS`**: User following relationships
- **`POSTED`**: Users posting tweets
- **`LIKED`**: Users liking tweets  
- **`RETWEETED`**: Users retweeting content
- **`MENTIONS`**: Tweets mentioning users
- **`HAS_HASHTAG`**: Tweets containing hashtags
- **`REPLIES_TO`**: Tweet reply threads

#### 📊 Test Scenarios

1. **Connection & Basic Operations**
   - Connect to FalkorDB instance
   - Create and save entities  
   - Retrieve entities by ID

2. **Social Network Creation**
   - Create influential users (Elon Musk, Bill Gates, Oprah)
   - Set up realistic profiles with follower counts
   - Create tweets and relationships

3. **Graph Traversal**  
   - Follow relationships between users
   - Find mutual connections
   - Navigate relationship paths

4. **Analytics Queries**
   - Count users and tweets
   - Find most followed users
   - Search verified users
   - Filter by engagement metrics

### Sample Test Output

```
🚀 Starting FalkorDB Twitter Integration Test
================================================================================
=== Testing FalkorDB Connection and Basic Operations ===
✅ Saved user: TwitterUser{id=1, username='testuser', displayName='Test User', ...}
✅ Retrieved user: TwitterUser{id=1, username='testuser', displayName='Test User', ...}

================================================================================  
=== Testing Twitter Graph Creation and Traversal ===
Created Twitter network with users:
- TwitterUser{id=2, username='elonmusk', displayName='Elon Musk', followerCount=150000000, verified=true}
- TwitterUser{id=3, username='billgates', displayName='Bill Gates', followerCount=60000000, verified=true}  
- TwitterUser{id=4, username='oprah', displayName='Oprah Winfrey', followerCount=45000000, verified=true}

Found 3 verified users:
  - Elon Musk (@elonmusk) - 150000000 followers
  - Bill Gates (@billgates) - 60000000 followers
  - Oprah Winfrey (@oprah) - 45000000 followers

================================================================================
🎉 All tests completed successfully!
```

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
```

## 🚧 Implementation Status

### ✅ Implemented
- Core annotations (`@Node`, `@Id`, `@Property`, `@Relationship`, `@GeneratedValue`)
- `FalkorDBRepository` interface with basic CRUD operations
- `FalkorDBClient` integration with JFalkorDB  
- `FalkorDBTemplate` for custom queries
- Basic entity mapping infrastructure
- Twitter integration test demonstrating real-world usage

### 🚧 In Progress  
- Complete mapping context implementation
- Entity converter with relationship traversal
- Query method name parsing and generation
- Full transaction support integration

### 📋 Planned
- Spring Boot auto-configuration starter
- Reactive programming support  
- Query by Example functionality
- Auditing support (`@CreatedDate`, `@LastModifiedDate`)
- Schema migration and evolution tools
- Performance optimization and caching

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

Licensed under the [Apache License 2.0](LICENSE.txt) - same as Spring Data projects.

```
Copyright 2011-2025 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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