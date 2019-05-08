/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.test;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.testcontainers.containers.Neo4jContainer;

/**
 * This extension is for internal use only. It is meant to speed up development and keep test containers for normal build.
 * When both {@code SDN_RX_NEO4J_URL} and {@code SDN_RX_NEO4J_PASSWORD} are set as environment variables, the extension
 * will inject a field of type {@link Neo4jConnectionSupport} into the extended test with a connection to that instance,
 * otherwise it will start a test container and use that connection.
 *
 * @author Michael J. Simons
 */
@Slf4j
public class Neo4jExtension implements BeforeAllCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
		.create(Neo4jExtension.class);

	private static final String KEY = "neo4j.standalone";

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		List<Field> injectableFields = ReflectionUtils
			.findFields(context.getRequiredTestClass(),
				field -> ReflectionUtils.isStatic(field) && field.getType() == Neo4jConnectionSupport.class,
				ReflectionUtils.HierarchyTraversalMode.BOTTOM_UP);

		if (injectableFields.size() != 1) {
			return;
		}

		String neo4jUrl = Optional.ofNullable(System.getenv("SDN_RX_NEO4J_URL")).orElse("");
		String neo4jPassword = Optional.ofNullable(System.getenv("SDN_RX_NEO4J_PASSWORD")).orElse("");

		Neo4jConnectionSupport neo4jConnectionSupport;
		if (!(neo4jUrl.isEmpty() || neo4jPassword.isEmpty())) {
			log.warn("Using Neo4j instance at {}.", neo4jUrl);
			neo4jConnectionSupport = new Neo4jConnectionSupport(neo4jUrl,
				AuthTokens.basic("neo4j", neo4jPassword));
		} else {
			log.warn("Using Neo4j test container.");
			ExtensionContext.Store store = context.getStore(NAMESPACE);
			ContainerAdapter adapter = store
				.getOrComputeIfAbsent(KEY, key -> new Neo4jExtension.ContainerAdapter(), ContainerAdapter.class);
			adapter.start();
			neo4jConnectionSupport = new Neo4jConnectionSupport(adapter.getBoltUrl(), AuthTokens.none());
		}

		Field field = injectableFields.get(0);
		field.setAccessible(true);
		field.set(null, neo4jConnectionSupport);
	}

	public static class Neo4jConnectionSupport {

		public final String url;

		public final AuthToken authToken;

		public final Config config;

		public Neo4jConnectionSupport(String url, AuthToken authToken) {
			this.url = url;
			this.authToken = authToken;
			this.config = Config.builder().withLogging(Logging.slf4j()).build();
		}

		public Driver openConnection() {
			return GraphDatabase.driver(url, authToken, config);
		}
	}

	static class ContainerAdapter implements ExtensionContext.Store.CloseableResource {

		private final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>()
			.withoutAuthentication();

		public String getBoltUrl() {
			return neo4jContainer.getBoltUrl();
		}

		public void start() {
			neo4jContainer.start();
		}

		@Override
		public void close() {
			this.neo4jContainer.close();
		}
	}
}
