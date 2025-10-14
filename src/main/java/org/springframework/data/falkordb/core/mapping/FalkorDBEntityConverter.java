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
package org.springframework.data.falkordb.core.mapping;

import java.util.Map;

import org.apiguardian.api.API;

import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.falkordb.core.FalkorDBClient;

/**
 * This orchestrates the built-in store conversions and any additional Spring converters
 * for FalkorDB graph entities.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface FalkorDBEntityConverter
		extends EntityReader<Object, FalkorDBClient.Record>, EntityWriter<Object, Map<String, Object>> {

}
