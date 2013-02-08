/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.test.snippets;

import org.springframework.data.neo4j.repository.GraphRepository;

import static org.springframework.data.test.snippets.SnippetRepositoryDerivedFinderTests.Group;

/**
* @author mh
* @since 14.10.11
*/
public interface GroupRepository extends GraphRepository<Group> { }
