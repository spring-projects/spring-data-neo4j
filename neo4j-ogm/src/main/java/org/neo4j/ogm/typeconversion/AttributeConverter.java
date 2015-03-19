/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.typeconversion;

/**
 * based on JPA AttributeConverter, but with methods
 * appropriate for property graphs, rather than
 * column stores/RDBMS.
 *
 * @param <T> the class of the entity attribute
 * @param <F> the class of the associated graph property
 *
 * @author Vince Bickers
 */
public interface AttributeConverter<T, F> {

    F toGraphProperty(T value);
    T toEntityAttribute(F value);

}
