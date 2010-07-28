package org.springframework.persistence.support;


/**
 * Interface to be implemented by classes that can instantiate and
 * configure entities. 
 * The framework must do this when creating objects resulting from finders,
 * even when there may be no no-arg constructor supplied by the user.
 * 
 * @author Rod Johnson
 */
public interface EntityInstantiator<BACKING_INTERFACE,STATE> {
	
	/*
	 * The best solution if available is to add a constructor that takes Node
	 * to each GraphEntity. This means generating an aspect beside every
	 * class as Roo presently does. 
	 * 
	 * An alternative that does not require Roo
	 * is a user-authored constructor taking Node and calling setUnderlyingNode()
	 * but this is less elegant and pollutes the domain object.
	 * 
	 * If the user supplies a no-arg constructor, instantiation can occur by invoking it
	 * prior to calling setUnderlyingNode().
	 * 
	 * If the user does NOT supply a no-arg constructor, we must rely on Sun-specific
	 * code to instantiate entities without invoking a constructor.
	 */
	
	<T extends BACKING_INTERFACE> T createEntityFromState(STATE s, Class<T> c);

}
