package org.springframework.data.neo4j.transactions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test uses {@link org.springframework.context.support.SimpleThreadScope} which does not support destruction
 * callbacks.
 *
 * You should not use this implementation of a thread-scoped bean in production. More generally, if you
 * want to use a thread-scoped bean, you must be aware of the fact that its lifecycle cannot be managed
 * automatically by Spring.
 *
 * @see <a href="https://github.com/spring-by-example/spring-by-example/tree/master/modules/sbe-thread-scope/src/main/java/org/springbyexample/bean/scope/thread">this SpringByExample module</a> for further information.
 *
 * @author vince
 */
@ContextConfiguration(classes = {ThreadScopeSessionBeanContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class ThreadScopeSessionBeanTest extends MultiDriverTestClass {

    private final String[] sessions = new String[2];

    @Autowired
    private Session session; // thread-scoped bean, proxied

    @Test
    public void ShouldUseDifferentSessionForSessionBeansOnDifferentThreads() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 2; i++) {
            final int j = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    sessions[j] = session.toString(); // captures the session's JVM identity
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        Assert.assertFalse(sessions[0].equals(sessions[1]));
    }

    @Test
    public void shouldUseSameSessionForSessionBeansOnSameThread() {

        for (int i = 0; i < 2; i++) {
            sessions[i] = session.toString();
        }

        Assert.assertTrue(sessions[0].equals(sessions[1])); // captures the session's JVM identity

    }

}
