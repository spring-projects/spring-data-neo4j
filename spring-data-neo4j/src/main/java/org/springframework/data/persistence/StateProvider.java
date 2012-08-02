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
 * @author Michael Hunger
 * @since 24.09.2010
 * @deprecated use {@link EntityInstantiator} abstraction instead
 */
@Deprecated
public abstract class StateProvider {
	private final static ThreadLocal stateHolder = new ThreadLocal();

	private StateProvider() {
	}

    @SuppressWarnings("unchecked")
	public static <STATE> void setUnderlyingState(STATE state) {
		if (stateHolder.get() != null)
			throw new IllegalStateException("StateHolder already contains state " + stateHolder.get() + " in thread "
					+ Thread.currentThread());
		stateHolder.set(state);
	}

	public static <STATE> STATE retrieveState() {
        @SuppressWarnings("unchecked") STATE result = (STATE) stateHolder.get();
		stateHolder.remove();
		return result;
	}
}
