package org.springframework.data.neo4j.events;

import org.neo4j.ogm.session.event.Event;
import org.neo4j.ogm.session.event.EventListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Created by markangrish on 22/09/2016.
 */
public class EventPublisher extends EventListenerAdapter {

	private final ApplicationEventPublisher publisher;

	public EventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

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
