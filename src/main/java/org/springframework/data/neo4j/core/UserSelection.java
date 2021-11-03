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

import java.util.Objects;

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This is a value object for a Neo4j user, potentially different from the user owning the physical Neo4j connection. To make use of
 * this a minimum version of Neo4j 4.4 and Neo4j-Java-Driver 4.4 is required, otherwise any usage of {@link UserSelection#impersonate(String)}
 * together with either the {@link UserSelectionProvider} or the {@link ReactiveUserSelectionProvider} will lead to runtime
 * errors.
 * <p>
 * Similar usage pattern like with the dynamic database selection are possible, for example tying
 * a {@link UserSelectionProvider} into Spring Security and use the current user as a user to impersonate.
 *
 * @author Michael J. Simons
 * @soundtrack Tori Amos - Strange Little Girls
 * @since 6.2
 */
@API(status = API.Status.STABLE, since = "6.2")
public final class UserSelection {

	private static final UserSelection CONNECTED_USER = new UserSelection(null);

	/**
	 * @return A user selection that will just use the user owning the physical connection.
	 */
	public static UserSelection connectedUser() {

		return CONNECTED_USER;
	}

	/**
	 * @param value The name of the user to impersonate
	 * @return A user selection representing an impersonated user.
	 */
	public static UserSelection impersonate(String value) {

		Assert.hasText(value, "Cannot impersonate user without username");
		return new UserSelection(value);
	}

	@Nullable private final String value;

	private UserSelection(@Nullable String value) {
		this.value = value;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		UserSelection that = (UserSelection) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
