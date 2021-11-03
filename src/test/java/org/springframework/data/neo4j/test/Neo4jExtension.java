/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.test;

import static org.assertj.core.api.Assumptions.assumeThat;

import lombok.extern.apachecommons.CommonsLog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.internal.util.ServerVersion;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * This extension is for internal use only. It is meant to speed up development and keep test containers for normal
 * build. When both {@code SDN_NEO4J_URL} and {@code SDN_NEO4J_PASSWORD} are set as environment variables, the extension
 * will inject a field of type {@link Neo4jConnectionSupport} into the extended test with a connection to that instance,
 * otherwise it will start a test container and use that connection.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@CommonsLog
public class Neo4jExtension implements BeforeAllCallback, BeforeEachCallback {

	public final static String NEEDS_REACTIVE_SUPPORT = "reactive-test";
	public final static String COMMUNITY_EDITION_ONLY = "community-edition";
	public final static String COMMERCIAL_EDITION_ONLY = "commercial-edition";
	/**
	 * Indicator that a given _test_ is not compatible in all cases with a cluster setup, especially in terms of
	 * synchronizing bookmarks between fixture / assertions and tests. Or it may indicate a dedicated cluster test, running
	 * against a dedicated extension.
	 */
	public final static String INCOMPATIBLE_WITH_CLUSTERS = "incompatible-with-clusters";
	public final static String REQUIRES = "Neo4j/";

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(Neo4jExtension.class);

	private static final String KEY_NEO4J_INSTANCE = "neo4j.standalone";
	private static final String KEY_DRIVER_INSTANCE = "neo4j.driver";

	private static final String SYS_PROPERTY_NEO4J_URL = "SDN_NEO4J_URL";
	private static final String SYS_PROPERTY_NEO4J_PASSWORD = "SDN_NEO4J_PASSWORD";
	private static final String SYS_PROPERTY_NEO4J_ACCEPT_COMMERCIAL_EDITION = "SDN_NEO4J_ACCEPT_COMMERCIAL_EDITION";
	private static final String SYS_PROPERTY_NEO4J_REPOSITORY = "SDN_NEO4J_REPOSITORY";
	private static final String SYS_PROPERTY_NEO4J_VERSION = "SDN_NEO4J_VERSION";

	private static Set<String> COMMUNITY_EDITION_INDICATOR = Collections.singleton("community");

	private static Set<String> COMMERCIAL_EDITION_INDICATOR = new HashSet<>(Arrays.asList("commercial", "enterprise"));

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		List<Field> injectableFields = ReflectionSupport.findFields(context.getRequiredTestClass(),
				field -> Modifier.isStatic(field.getModifiers()) && field.getType() == Neo4jConnectionSupport.class,
				HierarchyTraversalMode.BOTTOM_UP);

		if (injectableFields.size() != 1) {
			return;
		}

		String neo4jUrl = Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_URL)).orElse("");
		String neo4jPassword = Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_PASSWORD)).orElse("").trim();

		ExtensionContext.Store contextStore = context.getStore(NAMESPACE);
		Neo4jConnectionSupport neo4jConnectionSupport = contextStore.get(KEY_DRIVER_INSTANCE, Neo4jConnectionSupport.class);

		if (neo4jConnectionSupport == null) {
			if (!(neo4jUrl.isEmpty() || neo4jPassword.isEmpty())) {
				log.info(LogMessage.format("Using Neo4j instance at %s.", neo4jUrl));
				neo4jConnectionSupport = new Neo4jConnectionSupport(neo4jUrl, AuthTokens.basic("neo4j", neo4jPassword));
			} else {
				log.info("Using Neo4j test container.");
				ContainerAdapter adapter = contextStore.getOrComputeIfAbsent(KEY_NEO4J_INSTANCE,
						key -> new Neo4jExtension.ContainerAdapter(), ContainerAdapter.class);
				adapter.start();
				neo4jConnectionSupport = new Neo4jConnectionSupport(adapter.getBoltUrl(), AuthTokens.none());
			}
			contextStore.put(KEY_DRIVER_INSTANCE, neo4jConnectionSupport);
		}

		checkRequiredFeatures(neo4jConnectionSupport, context.getTags());

		Field field = injectableFields.get(0);
		field.setAccessible(true);
		field.set(null, neo4jConnectionSupport);
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		ExtensionContext.Store contextStore = context.getStore(NAMESPACE);
		Neo4jConnectionSupport neo4jConnectionSupport = contextStore.get(KEY_DRIVER_INSTANCE, Neo4jConnectionSupport.class);
		checkRequiredFeatures(neo4jConnectionSupport, context.getTags());
	}

	private void checkRequiredFeatures(Neo4jConnectionSupport neo4jConnectionSupport, Set<String> tags) {
		if (tags.contains(NEEDS_REACTIVE_SUPPORT)) {
			assumeThat(neo4jConnectionSupport.getServerVersion().greaterThanOrEqual(ServerVersion.v4_0_0))
					.describedAs("This test requires at least Neo4j 4.0 for reactive database connectivity.").isTrue();
		}

		if (tags.contains(COMMUNITY_EDITION_ONLY)) {
			assumeThat(neo4jConnectionSupport.isCommunityEdition())
					.describedAs("This test should be run on the community edition only").isTrue();
		}

		if (tags.contains(COMMERCIAL_EDITION_ONLY)) {
			assumeThat(neo4jConnectionSupport.isCommercialEdition())
					.describedAs("This test should be run on the commercial edition only").isTrue();
		}

		tags.stream().filter(s -> s.startsWith(REQUIRES)).map(ServerVersion::version).forEach(v -> {
			assumeThat(neo4jConnectionSupport.getServerVersion().greaterThanOrEqual(v))
					.describedAs("This test requires at least " + v.toString()).isTrue();
		});
	}

	/**
	 * Support class that holds the connection information and opens a new connection on demand.
	 *
	 * @since 6.0
	 */
	public static final class Neo4jConnectionSupport implements ExtensionContext.Store.CloseableResource {

		public final String url;

		public final AuthToken authToken;

		public final Config config;

		private volatile ServerVersion cachedServerVersion;

		/**
		 * Shared instance of the standard (non-routing) driver.
		 */
		private volatile Driver driverInstance;

		public Neo4jConnectionSupport(String url, AuthToken authToken) {
			this.url = url;
			this.authToken = authToken;
			this.config = Config.builder().withLogging(Logging.slf4j())
					.withMaxConnectionPoolSize(Runtime.getRuntime().availableProcessors())
					.build();
		}

		/**
		 * This method asserts that the current driver instance is usable before handing it out. If it isn't usable, it
		 * creates a new one.
		 *
		 * @return A shared driver instance, connected to either a database running inside test containers or
		 * running locally.
		 */
		public Driver getDriver() {

			Driver driver = this.driverInstance;
			if (!isUsable(driver)) {
				synchronized (this) {
					driver = this.driverInstance;
					if (!isUsable(driver)) {
						this.driverInstance = GraphDatabase.driver(url, authToken, config);
						driver = this.driverInstance;
					}
				}
			}
			return driver;
		}

		/**
		 * A driver is usable if it's not null and can verify its connectivity. This method force closes
		 * the bean if the connectivity cannot be verified to avoid having a netty pool dangling around.
		 *
		 * @param driver The driver that should be checked for usability
		 * @return true if the driver is currently usable.
		 */
		private static boolean isUsable(@Nullable Driver driver) {

			if (driver == null) {
				return false;
			}
			try {
				driver.verifyConnectivity();
				return true;
			} catch (Exception ex) {
				try {
					driver.close();
				} catch (Exception nested) {
				}
				return false;
			}
		}

		public ServerVersion getServerVersion() {

			ServerVersion serverVersion = this.cachedServerVersion;
			if (serverVersion == null) {
				synchronized (this) {
					serverVersion = this.cachedServerVersion;
					if (serverVersion == null) {
						try (Session session = this.getDriver().session()) {
							Record result = session.run("CALL dbms.components() YIELD versions RETURN 'Neo4j/' + versions[0] as version").single();
							this.cachedServerVersion = ServerVersion.version(result.get("version").asString());
						} catch (Exception e) {
							this.cachedServerVersion = ServerVersion.version(getDriver());
						}
						serverVersion = this.cachedServerVersion;
					}
				}
			}

			return serverVersion;
		}

		String getEdition() {
			String edition;
			SessionConfig sessionConfig = SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build();
			try (Session session = getDriver().session(sessionConfig)) {
				edition = session.run("call dbms.components() yield edition").single().get("edition").asString();
			}
			return edition.toLowerCase(Locale.ENGLISH);
		}

		boolean isCommunityEdition() {

			return COMMUNITY_EDITION_INDICATOR.contains(getEdition());
		}

		boolean isCommercialEdition() {

			return COMMERCIAL_EDITION_INDICATOR.contains(getEdition());
		}

		@Override
		public void close() {

			// Don't open up a driver for just closing it
			if (driverInstance == null) {
				return;
			}

			// Catch all the things... The driver has been closed maybe by a Spring Context already
			try {
				log.debug("Closing Neo4j connection support.");
				driverInstance.close();
			} catch (Exception e) {
			}
		}
	}

	static class ContainerAdapter implements ExtensionContext.Store.CloseableResource {

		private final String repository = Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_REPOSITORY)).orElse("neo4j");

		private final String imageVersion = Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_VERSION)).orElse("4.2");

		private final boolean containerReuseSupported = TestcontainersConfiguration
				.getInstance().environmentSupportsReuse();

		private final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(repository + ":" + imageVersion)
				.withoutAuthentication()
				.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT",
						Optional.ofNullable(System.getenv(SYS_PROPERTY_NEO4J_ACCEPT_COMMERCIAL_EDITION)).orElse("no"))
				.withReuse(containerReuseSupported);

		public String getBoltUrl() {
			return neo4jContainer.getBoltUrl();
		}

		public void start() {
			neo4jContainer.start();
		}

		@Override
		public void close() {
			if (!containerReuseSupported) {
				this.neo4jContainer.close();
			}
		}
	}
}
