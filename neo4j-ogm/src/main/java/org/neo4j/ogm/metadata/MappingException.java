/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
