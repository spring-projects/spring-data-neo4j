/*
 * Copyright 2011-2025 the original author or authors.
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
/**
 * <!-- tag::intent[] --> This package contains the callback API. There are both
 * imperative and reactive callbacks available that get invoked just before an entity is
 * bound to a statement. These can be implemented by client code. For further convenience,
 * both imperative and reactive auditing callbacks are available. The config package
 * contains a registrar and an annotation to enable those without having to provide the
 * beans manually.
 *
 * The event system comes in two flavours: Events that are based on Spring's application
 * event system and callbacks that are based on Spring Data's callback system. Application
 * events can be configured to run asynchronously, which make them a bad fit in
 * transactional workloads.
 *
 * As a rule of thumb, use Entity callbacks for modifying entities before persisting and
 * application events otherwise. The best option however to react in a transactional way
 * to changes of an entity is to implement
 * {@link org.springframework.data.domain.DomainEvents} on an aggregate root. <!--
 * end::intent[] -->
 * @author Michael J. Simons
 */
package org.springframework.data.neo4j.core.mapping.callback;
