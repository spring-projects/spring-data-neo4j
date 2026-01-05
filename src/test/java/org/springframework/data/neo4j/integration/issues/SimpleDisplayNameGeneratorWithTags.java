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
package org.springframework.data.neo4j.integration.issues;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.platform.commons.util.AnnotationUtils;

/**
 * Prepends the value of any tags defined for a given test method to the display name.
 *
 * @author Michael J. Simons
 */
final class SimpleDisplayNameGeneratorWithTags extends DisplayNameGenerator.Simple {

	@Override
	public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass,
			Method testMethod) {

		var displayNameForMethod = testMethod.getName();
		var tags = AnnotationUtils.findRepeatableAnnotations(testMethod, Tag.class);
		if (tags.isEmpty()) {
			return displayNameForMethod;
		}

		return tags.stream().map(Tag::value).collect(Collectors.joining(", ", "", ": " + displayNameForMethod));
	}

}
