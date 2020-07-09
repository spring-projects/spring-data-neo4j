/**
 * Contains the infrastructure for the event system. The event system comes in two flavours: Events that are based on
 * Spring's application event system and callbacks that are based on Spring Data's callback system. Application events
 * can be configured to run asynchronously, which make them a bad fit in transactional workloads.
 * <p>
 * As a rule of thumb, use Entity callbacks for modifying entities before persisting and application events otherwise.
 * The best option however to react in a transactional way to changes of an entity is to implement
 * {@link org.springframework.data.domain.DomainEvents} on an aggregate root.
 *
 * @author Michael J. Simons
 * @since 1.0
 * @soundtrack Bon Jovi - Slippery When Wet
 */
@NonNullApi
package org.springframework.data.neo4j.repository.event;

import org.springframework.lang.NonNullApi;
