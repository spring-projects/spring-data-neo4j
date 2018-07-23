package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;

/**
 * Created by markangrish on 22/09/2016.
 */
public class PreDeleteEvent extends ModificationEvent {

	public PreDeleteEvent(Event event) {
		super(event);
	}
}
