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
package org.springframework.data.neo4j.mapping;

/**
 * Interface to be implemented by classes that can instantiate and configure entities. The framework must do this when
 * creating objects resulting from finders, even when there may be no no-arg constructor supplied by the user.
 * 
 * @author Rod Johnson
 */
public interface EntityInstantiator<STATE> {

	/*
		 * The best solution if available is to add a constructor that takes Node
		 * to each GraphEntity. This means generating an aspect beside every
		 * class as Roo presently does.
		 *
		 * An alternative that does not require Roo
		 * is a user-authored constructor taking Node and calling setUnderlyingNode()
		 * but this is less elegant and pollutes the domain object.
		 *
		 * If the user supplies a no-arg constructor, instantiation can occur by invoking it
		 * prior to calling setUnderlyingNode().
		 *
		 * If the user does NOT supply a no-arg constructor, we must rely on Sun-specific
		 * code to instantiate entities without invoking a constructor.
		 */

	<T> T createEntityFromState(STATE s, Class<T> c);

}
