/*
 * Copyright 2011-present the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.junit.jupiter.causal_cluster.NeedsCausalCluster;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base annotation for tests that depend on a Causal Cluster. The causal cluster setup via Docker puts a high load on
 * the system and also requires acceptance of the commercial license. Therefore it is only enabled when the environment
 * variable {@literal SDN_NEO4J_ACCEPT_COMMERCIAL_EDITION} is set to {@literal yes} and a {@literal SDN_NEO4J_VERSION}
 * points to a stable 4.0.x version.
 *
 * @author Michael J. Simons
 * @soundtrack Command & Conquer - Alarmstufe Rot
 * @since 6.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@NeedsCausalCluster(password = "secret", startupTimeOutInMillis = 600_000L)
@ExtendWith(SpringExtension.class)
@Tag("CausalClusterRequired")
@EnabledIfEnvironmentVariable(named = "SDN_NEO4J_ACCEPT_COMMERCIAL_EDITION", matches = "yes")
@EnabledIfEnvironmentVariable(named = "SDN_NEO4J_VERSION", matches = "4\\.0\\.\\d(-.+)?")
public @interface CausalClusterIntegrationTest {
}
