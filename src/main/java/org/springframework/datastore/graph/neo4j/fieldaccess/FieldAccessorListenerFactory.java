package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
interface FieldAccessorListenerFactory<E> {
    boolean accept(Field f);

    FieldAccessListener<E, ?> forField(Field f);
}
