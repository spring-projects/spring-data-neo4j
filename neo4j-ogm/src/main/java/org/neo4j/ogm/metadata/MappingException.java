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

package org.neo4j.ogm.metadata;

/**
 * Specialised {@link RuntimeException} thrown when an unrecoverable issue occurs when mapping between objects and graphs.
 */
public class MappingException extends RuntimeException {

    private static final long serialVersionUID = -9160906479092232033L;

    /**
     * Constructs a new {@link MappingException} with the given reason message and cause.
     *
     * @param reasonMessage A message explaining the reason for this exception
     * @param cause The underlying {@link Exception} that was the root cause of the problem
     */
    public MappingException(String reasonMessage, Exception cause) {
        super(reasonMessage, cause);
    }

    /**
     * Constructs a new {@link MappingException} with the given message.
     *
     * @param message A message describing the reason for this exception
     */
    public MappingException(String message) {
        super(message);
    }

}
