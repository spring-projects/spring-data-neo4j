///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//REPOS mavencentral,spring-libs-snapshot=https://repo.spring.io/libs-snapshot,snapshot-repo=file://$SNAPSHOT_REPO

// This is a template to create a Java script runnable via JBang https://github.com/jbangdev/jbang and Java 11 to run.
// It takes most of the integration tests and runs them against Neo4j. It will react on exceptions happening due to cluster
// changes and retry them accordingly. While you can point it against a non-cluster deployment, it wouldn't make much sense,
// as this scenario is already covered based on the standard testing.
// The file is named `template.java` as it has some placeholders that needs to be replaced by the orchestrating `runClusterTests.sh`.

//FILES logback.xml

//DEPS org.junit.platform:junit-platform-launcher:1.7.1
//DEPS org.springframework.data:spring-data-neo4j:$SDN_VERSION
//DEPS org.springframework.data:spring-data-neo4j:$SDN_VERSION:tests@test-jar
//$ADDITIONAL_DEPENDENCIES

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.launcher.TagFilter.excludeTags;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.neo4j.driver.internal.retry.ExponentialBackoffRetryLogic;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.support.RetryExceptionPredicate;

public class runClusterTests {

	public static void main(String... args) throws IOException {

		var logConfig = Files.createTempFile("logback", ".xml");
		try (var s = runClusterTests.class.getResourceAsStream("logback.xml")) {
			Files.copy(s, logConfig, StandardCopyOption.REPLACE_EXISTING);
		}
		System.setProperty("logback.configurationFile", logConfig.toAbsolutePath().normalize().toString());

		var log = LoggerFactory.getLogger(runClusterTests.class);

		var listener = new SummaryGeneratingListener();
		var launcher = LauncherFactory
				.create(LauncherConfig.builder()
						.addTestExecutionListeners(new TestExecutionListener() {
							@Override public void executionStarted(TestIdentifier testIdentifier) {
								if (testIdentifier.isContainer() && testIdentifier.getParentId().isPresent()) {
									log.debug(testIdentifier.getUniqueId());
								}
							}
						})
						.addTestExecutionListeners(listener).build());

		var canRetry = new RetryExceptionPredicate().or(ExponentialBackoffRetryLogic::isRetryable);

		var selectors = List.<DiscoverySelector>of(selectPackage("org.springframework.data.neo4j.integration"));
		var maxRetries = 100;
		var counter = 0;

		while (!selectors.isEmpty() && ++counter <= maxRetries) {
			log.info("Attempt {}/{}", counter, maxRetries);
			var request = LauncherDiscoveryRequestBuilder.request()
					.selectors(selectors)
					.filters(
							includeClassNamePatterns(".*IT.*"),
							excludeTags("incompatible-with-clusters")
					)
					.build();

			launcher.execute(request);

			var failures = listener.getSummary().getFailures();

			if (failures.isEmpty()) {
				log.info("Finished succesfully after {} attempts.", counter);
				System.exit(0);
			}

			var failuresByState = failures.stream().collect(partitioningBy(failure -> {
				var ex = new Throwable[] { failure.getException() };
				if (ex[0] instanceof AssertionError) {
					ex = ((AssertionError) ex[0]).getSuppressed();
				}
				for (var candidate : ex) {
					do {
						if (canRetry.test(candidate)) {
							return true;
						}
						candidate = candidate.getCause();
					} while (candidate != null);
				}
				return false;
			}));

			if (!failuresByState.get(false).isEmpty()) {
				log.error("The following tests failed in non retryable ways:");
				failuresByState.get(false).forEach(failure -> {
					log.error(failure.getTestIdentifier().getUniqueId());
					failure.getException().printStackTrace();
				});
				System.exit(1);
			}

			selectors = failuresByState.get(true).stream()
					.peek(failure -> log.info("{} will be retried", failure.getTestIdentifier().getUniqueId()))
					.map(failure -> DiscoverySelectors.selectUniqueId(failure.getTestIdentifier().getUniqueId()))
					.collect(toList());
		}
		log.error("Several tests failed despite a number of retries.");
		System.exit(1);
	}
}
