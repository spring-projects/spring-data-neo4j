# GitHub Actions Workflows Quick Reference

This document provides a quick reference for the GitHub Actions workflows in this repository.

## Workflows Overview

| Workflow | File | Triggers | Purpose |
|----------|------|----------|---------|
| Build & Test | `build.yml` | PR, Push to main/release | Validate code changes |
| Publish to Maven | `publish.yml` | Push to main/release, Manual | Deploy SNAPSHOT artifacts |
| CodeQL | `codeql.yml` | PR, Push, Schedule | Security analysis |
| Project Management | `project.yml` | Issues, PR events | Automate issue tracking |

## Build & Test Workflow

### When it runs
- On pull requests to `main` or release branches (`X.X.x`)
- On pushes to `main` or release branches

### What it does
1. Checks out code
2. Sets up Java 17 (Temurin)
3. Starts FalkorDB service (port 6379)
4. Runs Maven build with tests and checkstyle
5. Uploads test results
6. Publishes test report

### Environment
- **Java**: 17
- **Maven**: Via wrapper
- **FalkorDB**: Latest (Docker service)

### Required Secrets
None

### Configuration
Modify `build.yml` to:
- Change Java version: Update `java-version` in setup-java step
- Adjust FalkorDB version: Update `image` in services section
- Customize Maven goals: Edit `run` in "Build with Maven" step

## Publish to Maven Workflow

### When it runs
- On pushes to `main` or release branches
- Manually via workflow_dispatch

### What it does
1. Checks out code
2. Sets up Java 17 (Temurin)
3. Starts FalkorDB service for tests
4. Builds and deploys to Maven repository
5. Uploads build artifacts

### Environment
- **Java**: 17
- **Maven**: Via wrapper with settings.xml
- **FalkorDB**: Latest (Docker service)

### Required Secrets
- `ARTIFACTORY_USERNAME` - Maven repository username
- `ARTIFACTORY_PASSWORD` - Maven repository password/token

### Configuration
Modify `publish.yml` to:
- Change deployment target: Update `settings.xml` in project root
- Adjust artifact retention: Change `retention-days` in Upload Artifacts step
- Customize Maven goals: Edit `run` in "Build and Deploy" step

## Manual Workflow Dispatch

To manually trigger the Publish workflow:

1. Go to Actions tab in GitHub
2. Select "Publish to Maven" workflow
3. Click "Run workflow"
4. Select branch (usually `main`)
5. Click "Run workflow" button

## Workflow Status

View workflow status at:
- Repository Actions tab: `https://github.com/FalkorDB/spring-data-falkordb/actions`
- Pull requests: Status checks section
- Commits: Commit status icons

## Troubleshooting

### Build Failures

**Problem**: Tests fail with "Connection refused" to FalkorDB
- **Cause**: FalkorDB service not ready
- **Solution**: Health check should handle this automatically; if persists, increase health check retries in workflow

**Problem**: Maven dependency resolution fails
- **Cause**: Network issues or repository unavailable
- **Solution**: Retry the workflow; GitHub Actions will use cached dependencies if available

**Problem**: Checkstyle failures
- **Cause**: Code style violations
- **Solution**: Run `./mvnw spring-javaformat:apply` locally to fix formatting

### Publish Failures

**Problem**: Authentication error deploying to Maven repository
- **Cause**: Missing or incorrect secrets
- **Solution**: Verify `ARTIFACTORY_USERNAME` and `ARTIFACTORY_PASSWORD` secrets in repository settings

**Problem**: "Version already exists" error
- **Cause**: Trying to publish non-SNAPSHOT version
- **Solution**: This workflow only publishes SNAPSHOT versions; use a release workflow for releases

## Best Practices

### For Contributors

1. **Before opening PR**: Run `./mvnw clean verify` locally
2. **Fix issues quickly**: CI failures block merging
3. **Check test reports**: Review failed tests in Actions tab
4. **Keep PRs focused**: Smaller PRs are easier to review and test

### For Maintainers

1. **Monitor workflow runs**: Check Actions tab regularly
2. **Update secrets**: Rotate credentials periodically
3. **Review dependencies**: Update action versions in workflows
4. **Cache management**: Clear caches if build issues persist

## Workflow Files Location

All workflow files are in `.github/workflows/`:
```
.github/workflows/
├── build.yml       # Build & Test
├── publish.yml     # Publish to Maven
├── codeql.yml      # CodeQL security scan
└── project.yml     # Project management
```

## Related Documentation

- [CI README](../ci/README.md) - Detailed CI/CD documentation
- [Contributing Guide](../README.md#-contributing) - How to contribute
- [Maven Documentation](https://maven.apache.org/) - Maven reference
- [GitHub Actions Docs](https://docs.github.com/actions) - GitHub Actions reference

## Getting Help

- Open an issue in the repository
- Check the Actions tab for workflow run logs
- Review the CI README for troubleshooting steps
- Contact maintainers via email or Discord
