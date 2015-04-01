/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.unit.session.lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.filesystem.Document;
import org.neo4j.ogm.domain.filesystem.Folder;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * These tests define the behaviour of the OGM with regard to
 * stale object detection.
 *
 * The actual test cases are the same as the ones in DegenerateEntityModelTests
 * which are known to correctly configure the underlying database.
 *
 * Because the OGM uses an object cache (to detect dirty objects, and/or deleted
 * relationships), we must ensure that changes to the database by a save() are always
 * accurately reflected by the corresponding get()
 *
 * Example:
 *
 *    f: { name: 'f', documents : [ { name: 'a'},  { name: 'b' } ] }
 *    a: { name: 'a', folder : { name: 'f' }}
 *    b: { name: 'b', folder : { name: 'f' }}
 *
 * If we now deleted 'a's reference to 'f' and saved a, we should
 * expect that when we retrieve 'f' it won't hold a reference to 'a'
 * any longer.
 *
 * @author Vince Bickers
 */
public class StaleObjectTest extends LifecycleTest {

    private static SessionFactory sessionFactory;

    private Folder f;
    private Document a;
    private Document b;

    @Before
    public void init() throws IOException {
        setUp();

        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.filesystem");
        session = sessionFactory.openSession("http://localhost:" + neoPort);

        a = new Document();
        a.setName("a");

        b = new Document();
        b.setName("b");

        f = new Folder();
        f.setName("f");

        f.getDocuments().add(a);
        f.getDocuments().add(b);

        a.setFolder(f);
        b.setFolder(f);

        session.save(f);
        //session.clear();


    }

    @After
    public void tearDownTest() {
        tearDown();
    }

    @Test
    public void testSaveDegenerateDocument() {

        // note that we don't clear the current folder object.
        a.setFolder(null);

        session.save(a);

        Folder p = session.load(Folder.class, f.getId());

        assertEquals("Folder{name='f', documents=1, archived=0}", p.toString());

    }

    @Test
    public void testSaveDegenerateFolder() {

        // note that we don't clear the current document object's folder references.
        f.getDocuments().clear();

        session.save(f);

        Document aa = session.load(Document.class, a.getId());
        Document bb = session.load(Document.class, b.getId());

        assertEquals("Document{folder=null, name='a'}", aa.toString());
        assertEquals("Document{folder=null, name='b'}", bb.toString());


    }
}
