/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.neo4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.FactoryBean;

/**
 * Used internally to wrap the creation of Neo4js Java Driver.
 *
 * @author Michael J. Simons
 */
interface Neo4jDriverFactory extends FactoryBean<Driver> {

	@Override
	default Class<?> getObjectType() {
		return Driver.class;
	}

	abstract class DriverFactoryBase {
		private final AuthToken authToken;

		private final Config config;

		DriverFactoryBase(AuthToken authToken, Config config) {
			this.authToken = authToken;
			this.config = config;
		}
	}

	class DefaultDriverFactory extends DriverFactoryBase implements Neo4jDriverFactory {

		private final URI uri;

		DefaultDriverFactory(final URI uri, AuthToken authToken, Config config) {
			super(authToken, config);
			this.uri = uri;
		}

		@Override
		public Driver getObject() {
			return GraphDatabase.driver(this.uri, super.authToken, super.config);
		}
	}

	class RoutingDriverFactory extends DriverFactoryBase implements Neo4jDriverFactory {

		private final List<URI> uris;

		RoutingDriverFactory(List<URI> uris, AuthToken authToken, Config config) {
			super(authToken, config);
			this.uris = new ArrayList<>(uris);
		}

		@Override
		public Driver getObject() {

			return GraphDatabase.routingDriver(this.uris, super.authToken, super.config);
		}
	}
}
