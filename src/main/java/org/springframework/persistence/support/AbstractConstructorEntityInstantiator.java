package org.springframework.persistence.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ClassUtils;

/**
 * Try for a constructor taking state: failing that, try a no-arg
 * constructor and then setUnderlyingNode().
 * 
 * @author Rod Johnson
 */
public abstract class AbstractConstructorEntityInstantiator<BACKING_INTERFACE, STATE> implements EntityInstantiator<BACKING_INTERFACE, STATE> {
	
	private final Log log = LogFactory.getLog(getClass());
	
	final public <T extends BACKING_INTERFACE> T createEntityFromState(STATE n, Class<T> c) {
		try {
			return fromStateInternal(n, c);
		}  catch (InstantiationException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}

	final private <T extends BACKING_INTERFACE> T fromStateInternal(STATE n, Class<T> c) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		// TODO this is fragile
		Class<? extends STATE> stateInterface = (Class<? extends STATE>) n.getClass().getInterfaces()[0];
		Constructor<T> nodeConstructor = ClassUtils.getConstructorIfAvailable(c, stateInterface);
		if (nodeConstructor != null) {
			// TODO is this the correct way to instantiate or does Spring have a preferred way?
			log.info("Using " + c + " constructor taking " + stateInterface);
 			return nodeConstructor.newInstance(n);
		}
		
		Constructor<T> noArgConstructor = ClassUtils.getConstructorIfAvailable(c);
		if (noArgConstructor != null) {
			log.info("Using " + c + " no-arg constructor");
			T t = noArgConstructor.newInstance();
			setState(t, n);
			return t;
		}
		
		throw new IllegalArgumentException(getClass().getSimpleName() + ": entity " + c + " must have either a constructor taking [" + stateInterface + 
				"] or a no-arg constructor and state set method");
	}
	
	/**
	 * Subclasses must implement to set state
	 * @param entity
	 * @param s
	 */
	protected abstract void setState(BACKING_INTERFACE entity, STATE s);

}
