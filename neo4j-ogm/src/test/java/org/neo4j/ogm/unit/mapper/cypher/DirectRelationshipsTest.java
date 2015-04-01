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
package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.cypher.statement.ParameterisedStatements;
import org.neo4j.ogm.domain.filesystem.Document;
import org.neo4j.ogm.domain.filesystem.Folder;
import org.neo4j.ogm.mapper.EntityGraphMapper;
import org.neo4j.ogm.mapper.EntityToGraphMapper;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.metadata.MetaData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This test suite contains tests of the cypher compiler output regarding
 * manipulation of direct relationships, i.e. relationships that are not
 * mediated in the entity model using RelationshipEntity instances.
 *
 * @see DATAGRAPH-588 - Relationship deletion problems
 *
 * @author: Vince Bickers
 */
public class DirectRelationshipsTest {

    private EntityToGraphMapper mapper;
    private static MetaData mappingMetadata;
    private static MappingContext mappingContext;

    @BeforeClass
    public static void setUpTestDatabase() {
        mappingMetadata = new MetaData("org.neo4j.ogm.domain.filesystem");
        mappingContext = new MappingContext(mappingMetadata);
    }

    @Before
    public void setUpMapper() {
        this.mapper = new EntityGraphMapper(mappingMetadata, mappingContext);
    }

    @After
    public void tidyUp() {
        mappingContext.clear();
    }


    @Test
    public void shouldSaveNewFolderDocumentPair() {

        Folder folder = new Folder();
        Document document = new Document();

        folder.getDocuments().add(document);
        document.setFolder(folder);

        expectOnSave(folder,
                "CREATE " +
                        "(_0:`Folder`), " +
                        "(_2:`Document`) " +
                        "WITH _0,_2 MERGE " +
                        "(_0)-[_3:`CONTAINS`]->(_2) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3");

        expectOnSave(document,
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`) " +
                        "WITH _0,_2 " +
                        "MERGE (_2)-[_3:`CONTAINS`]->(_0) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3");

    }

    @Test
    public void shouldSaveNewFolderWithTwoDocuments() {

        Folder folder = new Folder();
        Document doc1 = new Document();
        Document doc2 = new Document();

        folder.getDocuments().add(doc1);
        folder.getDocuments().add(doc2);

        doc1.setFolder(folder);
        doc2.setFolder(folder);

        expectOnSave(folder,
                "CREATE " +
                        "(_0:`Folder`), " +
                        "(_2:`Document`), " +
                        "(_5:`Document`) " +
                        "WITH _0,_2,_5 MERGE (_0)-[_3:`CONTAINS`]->(_2) " +
                        "WITH _0,_2,_3,_5 MERGE (_0)-[_6:`CONTAINS`]->(_5) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3, id(_5) AS _5, id(_6) AS _6");


        expectOnSave(doc1,
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`), " +
                        "(_5:`Document`) " +
                        "WITH _0,_2,_5 MERGE (_2)-[_3:`CONTAINS`]->(_0) " +
                        "WITH _0,_2,_3,_5 MERGE (_2)-[_6:`CONTAINS`]->(_5) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3, id(_5) AS _5, id(_6) AS _6");

        expectOnSave(doc2,
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`), " +
                        "(_4:`Document`) " +
                        "WITH _0,_2,_4 MERGE (_2)-[_5:`CONTAINS`]->(_4) " +
                        "WITH _0,_2,_4,_5 MERGE (_2)-[_6:`CONTAINS`]->(_0) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_4) AS _4, id(_5) AS _5, id(_6) AS _6");

    }


    @Test
    public void shouldNotBeAbleToCreateDuplicateRelationship() {

        Folder folder = new Folder();
        Document document = new Document();

        document.setFolder(folder);

        // we try to store two identical references to the document object. Although this
        // is supported by the graph, it isn't currently supported by the OGM,
        // therefore we expect only one relationship to be persisted

        folder.getDocuments().add(document);
        folder.getDocuments().add(document);

        assertEquals(2, folder.getDocuments().size());

        expectOnSave(folder,
                "CREATE " +
                        "(_0:`Folder`), " +
                        "(_2:`Document`) " +
                        "WITH _0,_2 MERGE " +
                        "(_0)-[_3:`CONTAINS`]->(_2) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3");

        expectOnSave(document,
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`) " +
                        "WITH _0,_2 " +
                        "MERGE (_2)-[_3:`CONTAINS`]->(_0) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3");
    }

    @Test
    public void shouldBeAbleToCreateDifferentRelationshipsToTheSameDocument() {

        Folder folder = new Folder();
        Document document = new Document();

        document.setFolder(folder);

        folder.getDocuments().add(document);
        folder.getArchived().add(document);

        expectOnSave(folder,
                // either
                "CREATE " +
                        "(_0:`Folder`), " +
                        "(_2:`Document`) " +
                        "WITH _0,_2 MERGE (_0)-[_1:`ARCHIVED`]->(_2) " +
                        "WITH _0,_1,_2 MERGE (_0)-[_3:`CONTAINS`]->(_2) " +
                        "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2, id(_3) AS _3",

                // or
                "CREATE " +
                        "(_0:`Folder`), " +
                        "(_2:`Document`) " +
                        "WITH _0,_2 MERGE (_0)-[_3:`CONTAINS`]->(_2) " +
                        "WITH _0,_2,_3 MERGE (_0)-[_4:`ARCHIVED`]->(_2) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3, id(_4) AS _4");

        expectOnSave(document,
                // either
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`) " +
                        "WITH _0,_2 MERGE (_2)-[_3:`ARCHIVED`]->(_0) " +
                        "WITH _0,_2,_3 MERGE (_2)-[_4:`CONTAINS`]->(_0) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3, id(_4) AS _4",
                // or
                "CREATE " +
                        "(_0:`Document`), " +
                        "(_2:`Folder`) " +
                        "WITH _0,_2 MERGE (_2)-[_3:`CONTAINS`]->(_0) " +
                        "WITH _0,_2,_3 MERGE (_2)-[_4:`ARCHIVED`]->(_0) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3, id(_4) AS _4"
                );
    }


    @Test
    public void shouldBeAbleToRemoveTheOnlyRegisteredRelationship() {

        Folder folder = new Folder();
        Document document = new Document();

        folder.getDocuments().add(document);
        document.setFolder(folder);

        folder.setId(0L);
        document.setId(1L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(document, document.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", document.getId()));

        mappingContext.remember(document);
        mappingContext.remember(folder);

        document.setFolder(null);
        folder.getDocuments().clear();

        expectOnSave(folder, "MATCH ($0)-[_0:CONTAINS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _0");

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", document.getId()));
        expectOnSave(document, "MATCH ($0)-[_0:CONTAINS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _0");

    }

    @Test
    public void shouldBeAbleToRemoveAnyRegisteredRelationship() {

        // given
        Folder folder = new Folder();
        Document doc1 = new Document();
        Document doc2 = new Document();

        folder.getDocuments().add(doc1);
        folder.getDocuments().add(doc2);
        doc1.setFolder(folder);
        doc2.setFolder(folder);

        folder.setId(0L);
        doc1.setId(1L);
        doc2.setId(2L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(doc1, doc1.getId());
        mappingContext.registerNodeEntity(doc2, doc2.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc1.getId()));
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId()));

        mappingContext.remember(doc1);
        mappingContext.remember(doc2);
        mappingContext.remember(folder);

        // when
        doc2.setFolder(null);
        folder.getDocuments().remove(doc2);


        // then
        assertEquals(1, folder.getDocuments().size());

        expectOnSave(folder,
                // either  (depending which doc is traversed first)
                "MATCH ($0)-[_2:CONTAINS]->($2) WHERE id($0)=0 AND id($2)=2 DELETE _2",
                // or
                "MATCH ($0)-[_1:CONTAINS]->($2) WHERE id($0)=0 AND id($2)=2 DELETE _1");

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId()));
        expectOnSave(doc1, "MATCH ($0)-[_2:CONTAINS]->($2) WHERE id($0)=0 AND id($2)=2 DELETE _2");

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId()));
        expectOnSave(doc2, "MATCH ($0)-[_0:CONTAINS]->($2) WHERE id($0)=0 AND id($2)=2 DELETE _0");

    }

    @Test
    public void shouldBeAbleToRemoveContainedRelationshipOnly() {

        // given
        Folder folder = new Folder();
        Document doc1 = new Document();

        folder.getDocuments().add(doc1);
        folder.getArchived().add(doc1);
        doc1.setFolder(folder);

        folder.setId(0L);
        doc1.setId(1L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(doc1, doc1.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc1.getId()));
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "ARCHIVED", doc1.getId()));

        mappingContext.remember(doc1);
        mappingContext.remember(folder);

        // when
        folder.getDocuments().remove(doc1);

        // then
        assertEquals(0, folder.getDocuments().size());
        assertEquals(1, folder.getArchived().size());

        expectOnSave(folder,
                // either
                "MATCH ($0)-[_2:CONTAINS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _2",
                // or
                "MATCH ($0)-[_1:CONTAINS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _1");

        // TODO:
        // this is wrong. the CONTAINS rel between the document and the folder is requested to be created,
        // but it ought to be requested to be deleted.
        // the moral of the story is, wherever possible, persist from the container.
        expectOnSave(doc1,
                "MATCH ($0) WHERE id($0)=0 MATCH ($1) WHERE id($1)=1 MERGE ($0)-[_0:`CONTAINS`]->($1) RETURN id(_0) AS _0");

    }

    private void expectOnSave(Object object, String... cypher) {
        ParameterisedStatements statements = new ParameterisedStatements(this.mapper.map(object).getStatements());
        for (String s : cypher) {
            if (s.equals(statements.getStatements().get(0).getStatement())) {
                return;
            }
        }
        fail("unexpected: '" + statements.getStatements().get(0).getStatement() + "'");
    }
}
