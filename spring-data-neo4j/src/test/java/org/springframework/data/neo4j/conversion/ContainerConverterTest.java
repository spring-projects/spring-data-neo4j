package org.springframework.data.neo4j.conversion;

import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author mh
 * @since 30.03.14
 */
public class ContainerConverterTest {
    @Test
    public void testSliceFirst() throws Exception {
        Slice<Integer> slice = ContainerConverter.slice(asList(1, 2, 3), new PageRequest(0, 1));
        assertThat(slice.getContent(), hasItem(1));
        assertThat(slice.getSize(), is(1));
        assertThat(slice.hasNext(), is(true));
    }
    @Test
    public void testSliceLast() throws Exception {
        Slice<Integer> slice = ContainerConverter.slice(asList(1,2,3), new PageRequest(2, 1));
        assertThat(slice.getContent(), hasItem(3));
        assertThat(slice.getSize(), is(1));
        assertThat(slice.hasNext(), is(false));
    }
    @Test
    public void testSliceAll() throws Exception {
        Slice<Integer> slice = ContainerConverter.slice(asList(1, 2, 3), new PageRequest(0, 3));
        assertThat(slice.getContent(), hasItems(1, 2, 3));
        assertThat(slice.getSize(), is(3));
        assertThat(slice.hasNext(), is(false));
    }
}
