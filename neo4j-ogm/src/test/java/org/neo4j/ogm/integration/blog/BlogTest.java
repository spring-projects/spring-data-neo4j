package org.neo4j.ogm.integration.blog;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.ogm.domain.blog.Post;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author: Vince Bickers
 */
public class BlogTest {

    @ClassRule
    public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule();

    private Session session;

    @Before
    public void init() throws IOException {
        session = new SessionFactory("org.neo4j.ogm.domain.blog").openSession(neo4jRule.baseNeoUrl());
    }

    @Test
    public void shouldTraverseListOfBlogPosts() {

        Post p1 = new Post("first");
        Post p2 = new Post("second");
        Post p3 = new Post("third");
        Post p4 = new Post("fourth");

        p1.setNext(p2);
        p2.setNext(p3);
        p3.setNext(p4);

        assertEquals(p1, p2.getPrevious());

        assertEquals(p2, p1.getNext());
        assertEquals(p2, p3.getPrevious());

        assertEquals(p3, p2.getNext());
        assertEquals(p3, p4.getPrevious());

        assertEquals(p4, p3.getNext());


        session.save(p1);

        session.clear();

        Post f3 = session.load(Post.class, p3.getId(), -1);
        Post f2 = f3.getPrevious();
        Post f1 = f2.getPrevious();
        Post f4 = f3.getNext();

        assertNull(f1.getPrevious());
        assertEquals(p1.getId(), f2.getPrevious().getId());
        assertEquals(p2.getId(), f3.getPrevious().getId());
        assertEquals(p3.getId(), f4.getPrevious().getId());

        assertEquals(p2.getId(), f1.getNext().getId());
        assertEquals(p3.getId(), f2.getNext().getId());
        assertEquals(p4.getId(), f3.getNext().getId());
        assertNull(f4.getNext());

    }
}
