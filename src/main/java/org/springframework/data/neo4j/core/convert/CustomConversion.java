/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.core.convert;

import java.util.Objects;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.springframework.util.Assert;

/**
 * This class presents the pair of writing and reading converters for a custom conversion. Bot directions are required.
 *
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Adrenochrom
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class CustomConversion {

	private final Function<?, Value> writingConverter;

	private final Function<Value, ?> readingConverter;

	public CustomConversion(Function<?, Value> writingConverter, Function<Value, ?> readingConverter) {

		Assert.notNull(writingConverter, "A writing converter is required.");
		this.writingConverter = writingConverter;
		Assert.notNull(readingConverter, "A reading converter is required.");
		this.readingConverter = readingConverter;
	}

	public Function<?, Value> getWritingConverter() {
		return writingConverter;
	}

	public Function<Value, ?> getReadingConverter() {
		return readingConverter;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CustomConversion that = (CustomConversion) o;
		return writingConverter.equals(that.writingConverter) &&
				readingConverter.equals(that.readingConverter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(writingConverter, readingConverter);
	}
}
