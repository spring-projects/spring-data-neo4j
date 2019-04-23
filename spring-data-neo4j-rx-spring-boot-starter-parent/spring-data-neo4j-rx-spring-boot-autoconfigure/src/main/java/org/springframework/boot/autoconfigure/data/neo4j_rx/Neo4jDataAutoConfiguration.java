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
package org.springframework.boot.autoconfigure.data.neo4j_rx;

import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Automatic configuration of base infrastructure that imports configuration for both imperative and reactive Neo4j
 * repositories. Depends on the configured Neo4j driver.
 *
 * @author Michael J. Simons
 */
@Configuration
@ConditionalOnClass(Driver.class)
@ConditionalOnBean(Driver.class)
@Import({ Neo4jImperativeDataAutoConfiguration.class, Neo4jReactiveDataAutoConfiguration.class })
public class Neo4jDataAutoConfiguration {
}
