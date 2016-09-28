package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;

/**
 * Created by markangrish on 22/09/2016.
 */
public class ModificationEvent {

	private Event source;

	public ModificationEvent(Event source) {
		this.source = source;
	}

	public Event getSource() {
		return source;
	}
}
