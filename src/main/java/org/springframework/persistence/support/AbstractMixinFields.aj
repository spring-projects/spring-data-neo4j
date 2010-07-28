package org.springframework.persistence.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.persistence.RelatedEntity;

/**
 * Abstract superaspect to advise field read and write
 * and introduce a mixin interface.
 * 
 * @param <N> type of introduced interface
 * 
 * @author Rod Johnson
 */
privileged abstract public aspect AbstractMixinFields<N> {
		
	protected final Log log = LogFactory.getLog(getClass());
	
	//-------------------------------------------------------------------------
	// ITDs to add behavior and state to classes
	//-------------------------------------------------------------------------
	// Enable Spring DI for all mixed-in objects
	declare @type: N+: @Configurable;
		
	//-------------------------------------------------------------------------
	// Advice for field get/set to delegate to backing Node.
	//-------------------------------------------------------------------------
	protected pointcut entityFieldGet(N entity) : 
			get(* N+.*) && 
			this(entity) &&
			!(get(@RelatedEntity * *)
			|| get(* N.*) 
			|| getsNotToAdvise()); 
	
	/**
	 * Never matches. Subclasses can override to exempt certain field reads from advice
	 */
	protected pointcut getsNotToAdvise();
	
	protected pointcut entityFieldSet(N entity, Object newVal) : 
			set(* N+.*) && 
			this(entity) && 
			args(newVal) &&
			!(set(@RelatedEntity * *) ||
					set(* N.*) ||
					setsNotToAdvise()); 
	
	/**
	 * Never matches. Subclasses can override to exempt certain field writes from advice
	 */
	protected pointcut setsNotToAdvise();
	
}
