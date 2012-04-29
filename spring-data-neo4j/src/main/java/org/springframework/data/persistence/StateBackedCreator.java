/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.persistence;

import org.springframework.data.convert.EntityInstantiator;

/**
 * encapsulates the instantiator of state-backed classes and populating them with the provided state.
 * <p/>
 * Can be implemented and registered with the concrete AbstractConstructorEntityInstantiator to provide non reflection
 * bases instantiaton for domain classes
 * 
 * @deprecated use {@link EntityInstantiator} abstraction instead.
 */
@Deprecated
public interface StateBackedCreator<T, STATE> {
	T create(STATE n, Class<T> c) throws Exception;
}
