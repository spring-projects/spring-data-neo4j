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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.Field;
import java.util.Map;

public class CleanContextCacheTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        Field cacheField = TestContext.class.getDeclaredField("contextCache");
        cacheField.setAccessible(true);
        ContextCache cache = (ContextCache) cacheField.get(testContext);
        Field cacheMapField = ContextCache.class.getDeclaredField("contextKeyToContextMap");
        cacheMapField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, ApplicationContext> cacheMap = (Map<String, ApplicationContext>) cacheMapField.get(cache);
        String[] keys = new String[cacheMap.size()];
        cacheMap.keySet().toArray(keys);
        for (String key : keys) {
            cache.setDirty(key);
        }
    }
}
