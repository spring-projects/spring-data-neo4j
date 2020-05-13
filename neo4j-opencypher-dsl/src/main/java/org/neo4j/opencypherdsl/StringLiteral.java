/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.opencypherdsl;

import static java.util.regex.Pattern.*;
import static org.apiguardian.api.API.Status.*;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apiguardian.api.API;

/**
 * The string representation of a string literal will be a quoted Cypher string in single tickmarks with
 * escaped reserved characters.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class StringLiteral extends Literal<CharSequence> {

	private static final Pattern RESERVED_CHARS = Pattern.compile("([" + quote("\\'\"") + "])");
	private static final String QUOTED_LITERAL_FORMAT = "'%s'";

	StringLiteral(CharSequence content) {
		super(content);
	}

	@Override
	public String asString() {

		final Optional<String> escapedContent = escapeString(getContent());
		return String.format(Locale.ENGLISH, QUOTED_LITERAL_FORMAT, escapedContent.orElse(""));
	}

	/**
	 * Escapes a string so that it can be used as a string literal in both single tickmarks ({@literal '})
	 * and quotes ({@literal "}).
	 *
	 * @param unescapedString The string to escape
	 * @return An empty optional when the unescaped string is {@literal null}, an escaped string otherwise
	 */
	static Optional<String> escapeString(CharSequence unescapedString) {

		if (unescapedString == null) {
			return Optional.empty();
		}

		final StringBuffer stringBuffer = new StringBuffer();
		Matcher matcher = RESERVED_CHARS.matcher(unescapedString);
		while (matcher.find()) {
			matcher.appendReplacement(stringBuffer, "\\\\\\" + matcher.group(1));
		}
		matcher.appendTail(stringBuffer);
		return Optional.of(stringBuffer.toString());
	}

}
