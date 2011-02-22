package org.springframework.data.graph.neo4j.template;

import org.junit.Test;
import org.neo4j.graphdb.Path;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author mh
 * @since 22.02.11
 */
public class PathMappingIteratorTest {

    private static final int PATHS_COUNT = 3;

    @Test
    public void lazyIteratorShouldNotCallBackBeforeUse() {
        runAndCheckMode(0, PATHS_COUNT, IterationController.IterationMode.LAZY);
    }

    @Test
    public void eagerIteratorShouldBeCalledImmediately() {
        runAndCheckMode(PATHS_COUNT, PATHS_COUNT, IterationController.IterationMode.EAGER);
    }

    @Test
    public void eagerStopOnNullIteratorShouldBeCalledImmediatelyAndReturnReducedResult() {
        runAndCheckMode(2, 2, IterationController.IterationMode.EAGER_STOP_ON_NULL);
    }
    @Test
    public void eagerStopIgnoresResultIteratorShouldBeCalledImmediatelyAndReturnNoResult() {
        Iterable<Integer> result = runAndCheckMode(PATHS_COUNT, PATHS_COUNT, IterationController.IterationMode.EAGER_IGNORE_RESULTS);
        assertNull("no result",result);
    }

    @Test
    public void defaultPathMapperIsLazy() {
        final AtomicInteger counter=new AtomicInteger();
        PathMapper<Integer> pathMapper = new PathMapper<Integer>() {

            @Override
            public Integer mapPath(Path path) {
                return counter.incrementAndGet();
            }
        };
        Iterable<Integer> result = new PathMappingIterator().mapPaths(paths(), pathMapper);
        assertEquals(0, counter.get());
        for (Integer integer : result) ;
        assertEquals(PATHS_COUNT, counter.get());
    }
    @Test
    public void defaultPathCallbackIsEagerAndIgnoresResult() {
        final AtomicInteger counter=new AtomicInteger();
        PathMapper.WithoutResult pathMapper = new PathMapper.WithoutResult() {

            @Override
            public void eachPath(Path path) {
                counter.incrementAndGet();
            }
        };
        Iterable<Void> result = new PathMappingIterator().mapPaths(paths(), pathMapper);
        assertEquals(PATHS_COUNT, counter.get());
        assertNull("no result",result);
        assertEquals(PATHS_COUNT, counter.get());
    }

    private List<Path> paths() {
        Path anyPath = mock(Path.class);
        Path[] paths=new Path[3];
        Arrays.fill(paths,anyPath);
        return Arrays.asList(paths);
    }

    private Iterable<Integer> runAndCheckMode(int countAfterMap, int countAfterIteration, IterationController.IterationMode iterationMode) {
        TestPathMapper pathMapper = new TestPathMapper(iterationMode);
        Iterable<Integer> result = new PathMappingIterator().mapPaths(paths(), pathMapper);
        assertEquals(countAfterMap, pathMapper.counter);
        if (result!=null) for (Integer integer : result) ;
        assertEquals(countAfterIteration, pathMapper.counter);
        return result;
    }

    private static class TestPathMapper implements PathMapper<Integer>, IterationController {
        public int counter;
        private IterationMode iterationMode;

        public TestPathMapper(IterationMode iterationMode) {
            this.iterationMode = iterationMode;
        }

        @Override
        public Integer mapPath(Path path) {
            counter++;
            if (counter == 2) return null;
            return counter;
        }

        @Override
        public IterationMode getIterationMode() {
            return iterationMode;
        }
    }
}
