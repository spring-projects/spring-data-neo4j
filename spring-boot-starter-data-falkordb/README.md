# Spring Boot Starter for Spring Data FalkorDB

This starter provides auto-configuration for Spring Data FalkorDB in Spring Boot applications.

## Quick Start

### 1. Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>com.falkordb</groupId>
    <artifactId>spring-boot-starter-data-falkordb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'com.falkordb:spring-boot-starter-data-falkordb:1.0.0-SNAPSHOT'
```

### 2. Configure Application Properties

Add the following to your `application.properties` or `application.yml`:

**application.properties:**
```properties
spring.data.falkordb.uri=falkordb://localhost:6379
spring.data.falkordb.database=my-graph-db
```

**application.yml:**
```yaml
spring:
  data:
    falkordb:
      uri: falkordb://localhost:6379
      database: my-graph-db
```

### 3. Create Entity

```java
@Node
public class Person {
    @Id
    private Long id;
    
    private String name;
    private int age;
    
    @Relationship(type = "KNOWS")
    private List<Person> friends;
    
    // getters and setters
}
```

### 4. Create Repository

```java
public interface PersonRepository extends FalkorDBRepository<Person, Long> {
    List<Person> findByName(String name);
    List<Person> findByAgeGreaterThan(int age);
}
```

### 5. Enable Repositories and Use in Your Application

```java
@SpringBootApplication
@EnableFalkorDBRepositories
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}

@RestController
public class PersonController {
    
    @Autowired
    private PersonRepository personRepository;
    
    @GetMapping("/persons")
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    
    @PostMapping("/persons")
    public Person createPerson(@RequestBody Person person) {
        return personRepository.save(person);
    }
}
```

## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.data.falkordb.uri` | FalkorDB server URI (format: `falkordb://host:port` or `redis://host:port`) | `falkordb://localhost:6379` |
| `spring.data.falkordb.database` | Database/graph name **(required)** | - |

## Health Check

If you have Spring Boot Actuator in your classpath, a health indicator for FalkorDB is automatically configured.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Check health at: `http://localhost:8080/actuator/health`

## Advanced Usage

### Custom Configuration

You can override any auto-configured bean:

```java
@Configuration
public class CustomFalkorDBConfig {
    
    @Bean
    public FalkorDBClient falkorDBClient(Driver driver) {
        // Custom configuration
        return new DefaultFalkorDBClient(driver, "my-custom-database");
    }
    
    @Bean
    public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client,
                                           FalkorDBMappingContext mappingContext) {
        // Custom template configuration
        DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(
            mappingContext, new EntityInstantiators(), client);
        return new FalkorDBTemplate(client, mappingContext, converter);
    }
}
```

## Examples

See the [examples directory](../examples) for complete sample applications.

## License

Apache License 2.0
