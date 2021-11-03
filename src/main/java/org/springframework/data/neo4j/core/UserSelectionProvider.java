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
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;

/**
 * @author Michael J. Simons
 * @soundtrack Tori Amos - Strange Little Girls
 * @since 6.2
 */
@API(status = API.Status.STABLE, since = "6.2")
@FunctionalInterface
public interface UserSelectionProvider {

	UserSelection getUserSelection();

	/**
	 * A user selection provider always selecting the connected user.
	 *
	 * @return A provider for using the connected user.
	 */
	static UserSelectionProvider getDefaultSelectionProvider() {

		return DefaultUserSelectionProvider.INSTANCE;
	}
}

enum DefaultUserSelectionProvider implements UserSelectionProvider {
	INSTANCE;

	@Override
	public UserSelection getUserSelection() {
		return UserSelection.connectedUser();
	}
}
