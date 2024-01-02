/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.events;

import lombok.AllArgsConstructor;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Michael J. Simons
 */
@Service
@Transactional
@AllArgsConstructor
public class Neo4jObjectService {

	private final EventsPublisherIT.Neo4jObjectRepository neo4jObjectRepository;
	private final ApplicationEventPublisher publisher;

	public Optional<Neo4jObject> findById(String id) {
		return neo4jObjectRepository.findById(id);
	}

	public Neo4jObject save(String id) {
		Neo4jObject saved = neo4jObjectRepository.save(new Neo4jObject(id));
		publisher.publishEvent(new EventsPublisherIT.Neo4jMessage(id));
		return saved;
	}
}
