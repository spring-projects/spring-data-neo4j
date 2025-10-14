# 🚀 FalkorDB Spring Data Integration Test

This directory contains a comprehensive integration test that demonstrates the FalkorDB Spring Data library working with a real FalkorDB instance using a Twitter-like social graph.

## 📋 Prerequisites

1. **FalkorDB Server**: You need FalkorDB running locally on port 6379
2. **Java 17+**: Make sure Java 17 or later is installed
3. **Maven**: Required for building and running the test

## 🏗️ Starting FalkorDB

### Option 1: Using Docker (Recommended)
```bash
docker run -p 6379:6379 falkordb/falkordb:latest
```

### Option 2: Using Native Installation
If you have FalkorDB installed natively:
```bash
falkordb-server --port 6379
```

### Option 3: Using Redis with FalkorDB Module
If you have Redis with FalkorDB module:
```bash
redis-server --port 6379 --loadmodule /path/to/falkordb.so
```

## 🧪 Running the Test

### Quick Run (Automated Script)
```bash
./run-falkordb-test.sh
```

### Manual Run
1. **Compile the project:**
   ```bash
   mvn compile -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true
   ```

2. **Run the integration test:**
   ```bash
   mvn exec:java -Dexec.mainClass="org.springframework.data.falkordb.integration.FalkorDBTwitterIntegrationTest" -Dexec.classpathScope="test"
   ```

### JUnit Test Runner
You can also run individual tests using JUnit:
```bash
mvn test -Dtest=FalkorDBTwitterIntegrationTest
```

## 🌐 What the Test Does

The integration test creates a comprehensive Twitter-like social graph and demonstrates:

### 🎭 Entity Creation
- **TwitterUser**: Users with profiles, follower counts, verification status
- **Tweet**: Tweets with text, timestamps, engagement metrics
- **Hashtag**: Hashtags with usage tracking

### 🔗 Relationship Types
- `FOLLOWS`: User following relationships
- `POSTED`: Users posting tweets
- `LIKED`: Users liking tweets
- `RETWEETED`: Users retweeting tweets
- `MENTIONS`: Tweets mentioning users
- `HAS_HASHTAG`: Tweets containing hashtags
- `REPLIES_TO`: Tweet reply chains

### 📊 Test Scenarios

1. **Connection & Basic Operations**
   - Connect to FalkorDB instance
   - Create and save entities
   - Retrieve entities by ID

2. **Twitter Network Creation**
   - Create influential users (Elon Musk, Bill Gates, Oprah)
   - Set up realistic profiles with follower counts
   - Create tweets and relationships

3. **Relationship Traversal**
   - Follow relationships between users
   - Find mutual connections
   - Navigate relationship paths

4. **Complex Queries**
   - Analytics queries (user counts, tweet counts)
   - Find most followed users
   - Search verified users
   - Filter by follower thresholds

## 📈 Sample Output

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
=== Testing Relationship Traversal ===
Alice follows 2 users:
  - Bob Smith
  - Charlie Brown
Bob has 1 followers:
  - Alice Johnson
Alice and Charlie both follow 1 users:
  - Bob Smith

================================================================================
🎉 All tests completed successfully!
FalkorDB Spring Data integration is working correctly.
```

## 🔍 Inspecting the Graph

After running the test, you can inspect the created graph using the Redis CLI:

```bash
redis-cli -p 6379
```

### Useful Queries

```cypher
# View all nodes
GRAPH.QUERY TWITTER 'MATCH (n) RETURN n LIMIT 10'

# View all users
GRAPH.QUERY TWITTER 'MATCH (u:User) RETURN u.username, u.display_name, u.follower_count'

# View follow relationships
GRAPH.QUERY TWITTER 'MATCH (u1:User)-[:FOLLOWS]->(u2:User) RETURN u1.username, u2.username'

# View tweets with authors
GRAPH.QUERY TWITTER 'MATCH (u:User)-[:POSTED]->(t:Tweet) RETURN u.username, t.text'

# Find verified users
GRAPH.QUERY TWITTER 'MATCH (u:User) WHERE u.verified = true RETURN u.username, u.follower_count ORDER BY u.follower_count DESC'

# Count nodes by type
GRAPH.QUERY TWITTER 'MATCH (u:User) RETURN "Users" as type, count(u) as count UNION MATCH (t:Tweet) RETURN "Tweets" as type, count(t) as count'

# Clear the graph (if needed)
GRAPH.QUERY TWITTER 'MATCH (n) DETACH DELETE n'
```

## 🏗️ Architecture Demonstrated

### Spring Data FalkorDB Components Used

1. **FalkorDBClient**: Direct connection to FalkorDB server
2. **FalkorDBTemplate**: High-level operations template
3. **Entity Mapping**: JPA-style annotations for graph entities
4. **Relationship Mapping**: Automatic relationship traversal
5. **Repository Pattern**: Spring Data repository interfaces
6. **Query Methods**: Derived and custom query methods

### Annotations Used

- `@Node`: Mark classes as graph nodes
- `@Id`: Specify entity identifiers
- `@GeneratedValue`: Auto-generate IDs
- `@Property`: Map properties to graph attributes
- `@Relationship`: Define relationships between entities

## 🎯 Key Features Demonstrated

✅ **Connection Management**: Robust FalkorDB connectivity  
✅ **Entity Persistence**: Save and retrieve complex objects  
✅ **Relationship Handling**: Navigate graph relationships  
✅ **Query Execution**: Custom Cypher query support  
✅ **Collection Support**: Handle lists of related entities  
✅ **Type Safety**: Strongly-typed entity conversion  
✅ **Error Handling**: Graceful failure management  

## 🚨 Troubleshooting

### FalkorDB Not Running
```
❌ FalkorDB connection failed: Connection refused
```
**Solution**: Make sure FalkorDB is running on localhost:6379

### Compilation Errors
```
❌ Compilation failed
```
**Solution**: Ensure Java 17+ is installed and JAVA_HOME is set correctly

### Permission Denied
```
❌ Permission denied: ./run-falkordb-test.sh
```
**Solution**: Make the script executable: `chmod +x run-falkordb-test.sh`

### Memory Issues
If you encounter OutOfMemoryError, increase Java heap size:
```bash
export MAVEN_OPTS="-Xmx2g"
```

## 📚 Further Reading

- [FalkorDB Documentation](https://www.falkordb.com/docs/)
- [Spring Data Documentation](https://spring.io/projects/spring-data)
- [Graph Database Concepts](https://www.falkordb.com/docs/graph-concepts)

---

🎉 **Happy Graph Traversing with FalkorDB and Spring Data!** 🎉