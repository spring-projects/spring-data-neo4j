# Contributing to Spring Data FalkorDB

Thank you for your interest in contributing to Spring Data FalkorDB! This guide will help you set up your development environment and understand our contribution workflow.

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

### Required
- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8.3+** 
- **FalkorDB** (for running tests)
- **Git**

### Recommended
- **Docker** (for easy FalkorDB setup)
- **IntelliJ IDEA** or **Eclipse** with Spring plugins

## 🛠️ Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/falkordb/spring-data-falkordb.git
cd spring-data-falkordb
```

### 2. Set Up FalkorDB

#### Option A: Docker (Recommended)
```bash
# Start FalkorDB in Docker
docker run -d --name falkordb -p 6379:6379 falkordb/falkordb:latest

# Verify it's running
redis-cli -p 6379 ping
# Should return: PONG

# Test graph functionality
redis-cli -p 6379 GRAPH.QUERY test "RETURN 'Hello FalkorDB' as greeting"
```

#### Option B: Native Installation
Follow the [FalkorDB installation guide](https://falkordb.com/docs/quickstart) for your platform.

### 3. Build the Project

```bash
# Clean build with tests
./mvnw clean verify

# Quick build without tests
./mvnw clean compile -DskipTests

# Build with checkstyle disabled (during development)
./mvnw clean verify -Dcheckstyle.skip=true
```

### 4. Run Tests

```bash
# Run all tests
./mvnw test

# Run only unit tests
./mvnw test -Dtest="*Test"

# Run only integration tests
./mvnw test -Dtest="*IntegrationTest*"

# Run Twitter integration test specifically
./mvnw test -Dtest=FalkorDBTwitterIntegrationTests

# Skip checkstyle during test development
./mvnw test -Dcheckstyle.skip=true
```

## 🧪 Testing Guidelines

### Test Structure
- **Unit tests**: Fast tests that don't require external dependencies
- **Integration tests**: Tests that require FalkorDB to be running
- **Example classes**: Located in `src/test/java/.../examples/`

### Writing Integration Tests
1. Ensure FalkorDB is running on `localhost:6379`
2. Use the test base classes for consistent setup
3. Clean up test data in teardown methods
4. Follow the Twitter integration test as an example

### Test Naming Convention
- Unit tests: `*Test.java`
- Integration tests: `*IntegrationTest.java` or `*IntegrationTests.java`
- Test classes should be descriptive: `PersonRepositoryIntegrationTests`

## 📝 Code Style

We use Checkstyle to enforce code style. The configuration is inherited from Spring Data parent.

```bash
# Check code style
./mvnw checkstyle:check

# Some IDEs can auto-format using Spring Java Format
# IntelliJ: Install "Spring Java Format" plugin
# Eclipse: Follow Spring Java Format setup guide
```

### Key Style Guidelines
- Use tabs for indentation
- Follow Spring Framework conventions
- Add proper JavaDoc for public APIs
- Use `@author` tags with your name and the adaptation note

Example:
```java
/**
 * Repository interface for TwitterUser entities.
 *
 * @author Your Name (FalkorDB adaptation)
 * @since 1.0
 */
```

## 🏗️ Project Structure

```
spring-data-falkordb/
├── src/main/java/              # Main source code
│   └── org/springframework/data/falkordb/
│       ├── core/              # Core functionality
│       ├── repository/        # Repository abstractions
│       └── support/           # Support utilities
├── src/test/java/             # Test source code
│   └── org/springframework/data/falkordb/
│       ├── examples/          # Example entities and tests
│       └── integration/       # Integration tests (Twitter demo)
├── ci/                        # CI/CD scripts
├── .github/workflows/         # GitHub Actions workflows
└── docs/                      # Documentation
```

## 🚀 Contribution Workflow

### 1. Create an Issue
- Check existing issues first
- Use issue templates when available
- Provide clear description and reproduction steps for bugs
- Discuss major changes before implementing

### 2. Fork and Branch
```bash
# Fork the repository on GitHub, then:
git clone https://github.com/YOUR-USERNAME/spring-data-falkordb.git
cd spring-data-falkordb
git remote add upstream https://github.com/falkordb/spring-data-falkordb.git

# Create a feature branch
git checkout -b feature/your-feature-name
```

### 3. Make Changes
- Write clear, focused commits
- Include tests for new functionality
- Update documentation if needed
- Follow existing code patterns

### 4. Test Your Changes
```bash
# Ensure FalkorDB is running
docker run -d --name falkordb -p 6379:6379 falkordb/falkordb:latest

# Run the full test suite
./mvnw clean verify

# Run specific tests relevant to your changes
./mvnw test -Dtest=YourSpecificTest
```

### 5. Submit a Pull Request
- Push your branch to your fork
- Create a pull request against the `main` branch
- Fill out the PR template
- Link to related issues
- Respond to review feedback promptly

## 🔍 Common Development Tasks

### Adding a New Entity for Testing
1. Create the entity class in `src/test/java/.../examples/`
2. Add appropriate annotations (`@Node`, `@Id`, `@Property`, etc.)
3. Create a corresponding repository interface
4. Write integration tests demonstrating the functionality

### Adding New Core Functionality
1. Create the implementation in `src/main/java/`
2. Add comprehensive unit tests
3. Add integration tests if applicable
4. Update JavaDoc and documentation
5. Consider backward compatibility

### Working with Relationships
- Use the Twitter integration test as a reference
- Currently, relationships are created via raw Cypher
- Automatic relationship handling is in development

## 📚 Resources

- **FalkorDB Documentation**: https://www.falkordb.com/docs/
- **Spring Data Documentation**: https://docs.spring.io/spring-data/commons/docs/current/reference/html/
- **Project Issues**: https://github.com/falkordb/spring-data-falkordb/issues
- **Discord Community**: https://discord.gg/falkordb

## 🤝 Getting Help

- **Community Discord**: Join our Discord for real-time help
- **GitHub Discussions**: For broader discussions and questions
- **Issues**: For bug reports and feature requests

## 📄 License

By contributing to Spring Data FalkorDB, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Spring Data FalkorDB! 🎉