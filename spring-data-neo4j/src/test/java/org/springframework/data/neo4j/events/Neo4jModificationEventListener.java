package org.springframework.data.neo4j.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Created by markangrish on 22/09/2016.
 */
@Component
public class Neo4jModificationEventListener {

	private int preSaveEventCount = 0;
	private int postSaveEventCount = 0;
	private int preDeleteEventCount = 0;
	private int postDeleteEventCount = 0;
	private PreSaveEvent preSaveEvent;
	private PostSaveEvent postSaveEvent;
	private PreDeleteEvent preDeleteEvent;
	private PostDeleteEvent postDeleteEvent;

	@EventListener
	public void onPreSaveEvent(PreSaveEvent event) {
		preSaveEvent = event;
		preSaveEventCount++;
	}

	@EventListener
	public void onPostSaveEvent(PostSaveEvent event) {
		postSaveEventCount++;
		postSaveEvent = event;
	}

	@EventListener
	public void onPreDeleteEvent(PreDeleteEvent event) {
		preDeleteEventCount++;
		preDeleteEvent = event;
	}

	@EventListener
	public void onPostDeleteEvent(PostDeleteEvent event) {
		postDeleteEventCount++;
		postDeleteEvent = event;
	}

	public boolean receivedPreSaveEvent() {
		return preSaveEventCount == 1;
	}

	public boolean receivedPostSaveEvent() {
		return postSaveEventCount == 1;
	}

	public boolean receivedPreDeleteEvent() {
		return preDeleteEventCount == 1;
	}

	public boolean receivedPostDeleteEvent() {
		return postDeleteEventCount == 1;
	}

	public PreSaveEvent getPreSaveEvent() {
		return preSaveEvent;
	}

	public PostSaveEvent getPostSaveEvent() {
		return postSaveEvent;
	}

	public PreDeleteEvent getPreDeleteEvent() {
		return preDeleteEvent;
	}

	public PostDeleteEvent getPostDeleteEvent() {
		return postDeleteEvent;
	}
}
