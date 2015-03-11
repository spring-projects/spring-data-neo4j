package org.neo4j.cineasts.service;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author mh
 * @since 02.03.11
 */
@Service
public class Neo4jDatabaseCleaner {

    @Autowired
    Session session;

    public void cleanDb() {
        session.execute("MATCH n OPTIONAL MATCH n-[r]-m delete n,r,m");
    }
}
