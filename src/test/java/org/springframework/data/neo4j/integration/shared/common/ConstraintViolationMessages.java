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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.regex.Pattern;

/**
 * Expected messages of constraint violations, shared between the imperative and reactive
 * exception translation tests.
 *
 * @author Michael J. Simons
 */
public final class ConstraintViolationMessages {

	private ConstraintViolationMessages() {
	}

	/**
	 * The wording of constraint violation messages differs between Neo4j versions and
	 * Aura (e.g. resolved vs. {@code Label[n]} names, additional clauses such as
	 * {@code graphTypeDependence}), so only the stable parts are checked: The label, the
	 * duplicated value and the error code.
	 * @param name the duplicated value of {@link SimplePerson#getName()}
	 * @return a regular expression matching the whole message of a translated violation
	 * of the unique constraint on {@link SimplePerson#getName()}
	 */
	public static String simplePersonNameNotUnique(String name) {
		return "(?s)(?=.*SimplePerson)(?=.*" + Pattern.quote(name)
				+ ").*Error code 'Neo\\.ClientError\\.Schema\\.ConstraintValidationFailed'.*";
	}

}
