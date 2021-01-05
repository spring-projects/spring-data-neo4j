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
package org.springframework.data.neo4j.conversion.datagraph1018;

import java.util.Optional;

import org.neo4j.ogm.typeconversion.AttributeConverter;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - S&M
 */
public final class EmailStringConverter implements AttributeConverter<Email, String> {

	@Override
	public String toGraphProperty(Email email) {
		return Optional.ofNullable(email).map(Email::getValue).orElse(null);
	}

	@Override
	public Email toEntityAttribute(String s) {
		return Optional.ofNullable(s).map(Email::new).orElse(null);
	}
}
