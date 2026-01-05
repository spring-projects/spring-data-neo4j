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
package org.springframework.data.neo4j.integration.imperative;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.integration.shared.common.AuditingITBase;

@Configuration
@EnableNeo4jAuditing(modifyOnCreate = false, auditorAwareRef = "auditorProvider",
		dateTimeProviderRef = "fixedDateTimeProvider")
class AuditingConfig {

	@Bean
	AuditorAware<String> auditorProvider() {
		return () -> Optional.of("A user");
	}

	@Bean
	DateTimeProvider fixedDateTimeProvider() {
		return () -> Optional.of(AuditingITBase.DEFAULT_CREATION_AND_MODIFICATION_DATE);
	}

}
