/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.support.conversion;

import org.springframework.data.mapping.model.MappingException;

/**
 * Exception which occurs whilst trying to build and map a POJO
 * from a Query result.
 *
 * @author Nicki Watt
 * @since 06.08.2013
 */
public class POJOResultBuildingException extends MappingException {

    private static final long serialVersionUID = 1L;

    public POJOResultBuildingException(String message, Throwable t) {
        super( message , t );
    }

}
