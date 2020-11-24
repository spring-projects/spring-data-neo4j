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
package org.springframework.data.neo4j.core.mapping.callback;

import org.apiguardian.api.API;
import org.springframework.data.mapping.callback.EntityCallback;

/**
 * Entity callback triggered before an Entity is bound to a record (represented by a {@link java.util.Map
 * java.util.Map&lt;String, Object&gt;}).
 *
 * @author Michael J. Simons
 * @param <T> The type of the entity.
 * @since 6.0
 * @soundtrack Bon Jovi - Slippery When Wet
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "6.0")
public interface BeforeBindCallback<T> extends EntityCallback<T> {

	/**
	 * Entity callback method invoked before a domain object is saved. Can return either the same or a modified instance
	 * of the domain object. This method is called before converting the {@code entity} to a {@link java.util.Map}, so the
	 * outcome of this callback is used to create the record for the domain object.
	 *
	 * @param entity the domain object to save.
	 * @return the domain object to be persisted.
	 */
	T onBeforeBind(T entity);
}
