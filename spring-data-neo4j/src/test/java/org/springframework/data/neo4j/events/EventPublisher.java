package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Created by markangrish on 22/09/2016.
 */
@Component
public class EventPublisher extends EventListenerAdapter {

	@Autowired private ApplicationEventPublisher publisher;

	@Override
	public void onPreSave(Event event) {
		this.publisher.publishEvent(new PreSaveEvent(event));
	}

	@Override
	public void onPostSave(Event event) {
		this.publisher.publishEvent(new PostSaveEvent(event));
	}

	@Override
	public void onPreDelete(Event event) {
		this.publisher.publishEvent(new PreDeleteEvent(event));
	}

	@Override
	public void onPostDelete(Event event) {
		this.publisher.publishEvent(new PostDeleteEvent(event));
	}
}
