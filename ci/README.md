# CI/CD Configuration

This directory contains CI/CD configuration and scripts for the Spring Data FalkorDB project.

## GitHub Actions Workflows

The project uses GitHub Actions for continuous integration and deployment. The workflows are located in `.github/workflows/`:

### Build & Test Workflow (`build.yml`)

**Purpose:** Validates all code changes through building and testing

**Triggers:**
- Push to `main` branch
- Push to release branches (pattern: `X.X.x`)
- Pull requests targeting `main` or release branches

**What it does:**
1. Sets up Java 17 (Temurin distribution)
2. Starts FalkorDB service (Docker container on port 6379)
3. Runs Maven build with:
   - Clean build
   - Dependency resolution
   - Full verification (unit + integration tests)
   - Checkstyle validation
4. Uploads test results
5. Publishes test reports

**Requirements:**
- FalkorDB running on port 6379 (handled automatically via GitHub services)
- Maven with Java 17
- Access to Spring snapshot repositories

### Publish Workflow (`publish.yml`)

**Purpose:** Publishes SNAPSHOT artifacts to Maven repository

**Triggers:**
- Push to `main` branch
- Push to release branches (pattern: `X.X.x`)
- Manual workflow dispatch

**What it does:**
1. Sets up Java 17 (Temurin distribution)
2. Starts FalkorDB service for integration tests
3. Runs Maven build and deploy:
   - Clean build
   - Full test suite
   - Generates JavaDoc
   - Deploys to configured Maven repository
4. Uploads build artifacts

**Requirements:**
- GitHub Secrets:
  - `ARTIFACTORY_USERNAME` - Username for Maven repository
  - `ARTIFACTORY_PASSWORD` - Password for Maven repository
- FalkorDB service (handled automatically)
- Access to Spring snapshot repositories

## CI Scripts

### `test.sh`

Shell script for running tests in CI environment. Used by both Jenkins and GitHub Actions workflows.

**Usage:**
```bash
JENKINS_USER_NAME=github-actions PROFILE=none ./test.sh
```

**Environment Variables:**
- `JENKINS_USER_NAME` - User name for Maven operations
- `PROFILE` - Maven profile to activate (default: none)
- `SDN_FORCE_REUSE_OF_CONTAINERS` - Reuse testcontainers (default: true)
- `SDF_FALKORDB_VERSION` - FalkorDB version to test against (default: latest)

### `clean.sh`

Shell script for cleaning up build artifacts in CI environment.

**Usage:**
```bash
JENKINS_USER_NAME=github-actions ./clean.sh
```

## Configuration Files

### `pipeline.properties`

Contains configuration for Jenkins pipelines, including:
- Java versions
- Docker images
- Maven repository URLs
- Credentials references

### `settings.xml` (project root)

Maven settings file configuring:
- Server credentials for artifact deployment
- Repository URLs
- Mirror configurations

## Setup Instructions

### For Repository Maintainers

To enable publishing to Maven repositories, configure the following GitHub secrets:

1. Go to repository Settings → Secrets and variables → Actions
2. Add the following secrets:
   - `ARTIFACTORY_USERNAME` - Your Artifactory username
   - `ARTIFACTORY_PASSWORD` - Your Artifactory password or access token

### For Contributors

No special setup is required. The build workflow runs automatically on pull requests.

To test locally with FalkorDB:

```bash
# Start FalkorDB
docker run -d -p 6379:6379 falkordb/falkordb:latest

# Run tests
./mvnw clean verify

# Run tests with checkstyle
./mvnw clean verify -Dcheckstyle.skip=false
```

## Workflow Status

You can view the status of workflows:
- In the "Actions" tab of the GitHub repository
- On pull requests (status checks)
- In commit history (status badges)

## Troubleshooting

### Build Failures

**Issue:** Tests fail due to FalkorDB connection
- **Solution:** Ensure FalkorDB service is running on port 6379
- **Local:** Start FalkorDB Docker container
- **CI:** Verify service configuration in workflow YAML

**Issue:** Dependency resolution fails
- **Solution:** Check network connectivity to Spring repositories
- **Local:** Ensure internet connection
- **CI:** Usually auto-resolves on retry

### Publish Failures

**Issue:** Authentication failures
- **Solution:** Verify GitHub secrets are correctly configured
- Check `ARTIFACTORY_USERNAME` and `ARTIFACTORY_PASSWORD`

**Issue:** Version conflicts
- **Solution:** Ensure version in `pom.xml` is correct SNAPSHOT version
- Release versions require different publishing workflow

## Migration from Jenkins

The GitHub Actions workflows complement the existing Jenkinsfile. Key differences:

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| Trigger | Poll SCM + Manual | Push/PR automatic |
| Infrastructure | Self-hosted agents | GitHub-hosted runners |
| FalkorDB | Docker via agent | GitHub service containers |
| Credentials | Jenkins credentials | GitHub secrets |
| Caching | Manual setup | Automatic (Maven) |

Both systems can coexist during transition period.
