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
package org.springframework.data.falkordb.core.schema;

import org.apiguardian.api.API;

/**
 * Internal ID generator that relies on FalkorDB's internal ID mechanism. This generator
 * does not actually generate IDs but serves as a marker to indicate that FalkorDB's
 * internal ID should be used.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class InternalIdGenerator implements IdGenerator<Long> {

	@Override
	public Long generateId(String primaryLabel, Object entity) {
		// This should never be called as it's handled internally by the framework
		throw new UnsupportedOperationException("Internal ID generation is handled by FalkorDB");
	}

}
