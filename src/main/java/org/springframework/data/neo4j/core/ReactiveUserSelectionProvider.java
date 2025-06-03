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
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;
import reactor.core.publisher.Mono;

/**
 * Functional interface for dynamic provision of usernames to the system.
 *
 * @author Michael J. Simons
 * @since 6.2
 */
@API(status = API.Status.STABLE, since = "6.2")
public interface ReactiveUserSelectionProvider {

	/**
	 * A user selection provider always selecting the connected user.
	 * @return a provider for using the connected user.
	 */
	static ReactiveUserSelectionProvider getDefaultSelectionProvider() {

		return DefaultReactiveUserSelectionProvider.INSTANCE;
	}

	Mono<UserSelection> getUserSelection();

}
