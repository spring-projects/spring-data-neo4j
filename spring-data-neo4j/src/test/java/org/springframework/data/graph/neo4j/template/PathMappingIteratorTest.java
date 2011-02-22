package org.springframework.data.graph.neo4j.template;

import org.junit.Test;
import org.neo4j.graphdb.Path;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author mh
 * @since 22.02.11
 */
public class PathMappingIteratorTest {

    @Test
    public void lazyIteratorShouldNotCallBackBeforeUse() {
        runAndCheckMode(0, 3, IterationController.IterationMode.LAZY);
    }
    @Test
    public void eagerIteratorShouldBeCalledImmediately() {
        runAndCheckMode(3, 3, IterationController.IterationMode.EAGER);
    }
    @Test
    public void eagerStopOnNullIteratorShouldBeCalledImmediatelyAndReturnReducedResult() {
        runAndCheckMode(2, 2, IterationController.IterationMode.EAGER_STOP_ON_NULL);
    }

    private void runAndCheckMode(int countAfterMap, int countAfterIteration, IterationController.IterationMode iterationMode) {
        TestPathMapper lazyMapper = new TestPathMapper(iterationMode);
        Path anyPath = mock(Path.class);
        List<Path> paths = Arrays.asList(anyPath, anyPath, anyPath);
        Iterable<Integer> result = new PathMappingIterator().mapPaths(paths, lazyMapper);
        assertEquals(countAfterMap, lazyMapper.counter);
        for (Integer integer : result);
        assertEquals(countAfterIteration, lazyMapper.counter);
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
            if (counter==2) return null;
            return counter;
        }

        @Override
        public IterationMode getIterationMode() {
            return iterationMode;
        }
    }
}
