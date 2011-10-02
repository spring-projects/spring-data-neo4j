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
package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.NotFoundException;


import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author mh
 * @since 22.09.11
 */
class EntityTypeCache {
    private final Map<String, Class<?>> cache = new WeakHashMap<String, Class<?>>();

    @SuppressWarnings({"unchecked"})
    <Object> Class<Object> getClassForName(String className) {
        try {
            Class<Object> result = (Class<Object>) cache.get(className);
            if (result != null) return result;
            synchronized (cache) {
                result = (Class<Object>) cache.get(className);
                if (result != null) return result;
                result = (Class<Object>) Class.forName(className);
                cache.put(className, result);
                return result;
            }
        } catch (NotFoundException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
