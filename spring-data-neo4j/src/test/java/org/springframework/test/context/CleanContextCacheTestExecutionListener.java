package org.springframework.test.context;

import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.applet.AppletContext;
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
        Map<String, AppletContext> cacheMap = (Map<String, AppletContext>) cacheMapField.get(cache);
        String[] keys = new String[cacheMap.size()];
        cacheMap.keySet().toArray(keys);
        for (String key : keys) {
            cache.setDirty(key);
        }
    }
}
