/*
 * Copyright 2011-2021 the original author or authors.
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
