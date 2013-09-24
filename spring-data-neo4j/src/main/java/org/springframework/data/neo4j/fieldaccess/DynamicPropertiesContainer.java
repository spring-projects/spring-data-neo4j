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
package org.springframework.data.neo4j.fieldaccess;

import java.util.HashMap;
import java.util.Map;

public class DynamicPropertiesContainer implements DynamicProperties {

	private final Map<String, Object> map = new HashMap<String, Object>();
    private boolean dirty;

    public DynamicPropertiesContainer() {
		
	}
	
	public DynamicPropertiesContainer(Map<String, Object> map) {
		this.map.putAll(map);
	}
	
	@Override
	public boolean hasProperty(String key) {
		return map.containsKey(key);
	}

	@Override
	public Object getProperty(String key) {
		return map.get(key);
	}

	@Override
	public Object getProperty(String key, Object defaultValue) {
		if(!hasProperty(key)) {
			return defaultValue;
		}
		return getProperty(key);
	}

	@Override
	public void setProperty(String key, Object value) {
		map.put(key, value);
	}

	@Override
	public Object removeProperty(String key) {
		return map.remove(key);
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return map.keySet();
	}

	@Override
	public Map<String, Object> asMap() {
		return new HashMap<String, Object>(map);
	}

	@Override
	public void setPropertiesFrom(Map<String, Object> m) {
		map.clear();
		map.putAll(m);
        setDirty(true);
    }

	@Override
	public DynamicProperties createFrom(Map<String, Object> map) {
		return new DynamicPropertiesContainer(map);
	}

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
