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
package org.springframework.data.neo4j.template;

import org.neo4j.ogm.session.event.Event;
import org.springframework.data.neo4j.events.ModificationEvent;

/**
 * Spring {@code ApplicationListener} used to capture {@link Event}s that occur during a test run. Note that this is
 * abstract because you're supposed to create an anonymous subclass to handle event type 'E' when you use it. This
 * ensures Spring doesn't just send {@link Event}s to everything regardless.
 *
 * @author Adam George
 */
public abstract class TestNeo4jEventListener<E extends ModificationEvent> {

	private ModificationEvent event;

	public void onApplicationEvent(E event) {
		this.event = event;
	}

	public boolean hasReceivedAnEvent() {
		return this.event != null;
	}

	public ModificationEvent getEvent() {
		return event;
	}
}
