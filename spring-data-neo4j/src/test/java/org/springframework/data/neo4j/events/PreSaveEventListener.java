package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created by markangrish on 22/09/2016.
 */
@Component
public class PreSaveEventListener {

	@EventListener
	public void onPreSave(Event event) {
	}
}
