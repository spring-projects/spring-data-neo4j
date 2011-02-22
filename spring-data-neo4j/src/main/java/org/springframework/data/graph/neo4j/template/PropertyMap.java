/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PropertyMap {

    private final Map<String, Object> properties = new HashMap<String, Object>();

    public PropertyMap set(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    public static PropertyMap props() {
        return new PropertyMap();
    }

    public Map<String, Object> toMap() {
        return properties;
    }

    public static Map<String, Object> _(String name, Object value) {
        return Collections.singletonMap(name,value);
    }
}
