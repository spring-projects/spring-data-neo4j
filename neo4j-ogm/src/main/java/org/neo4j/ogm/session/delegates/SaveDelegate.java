package org.neo4j.ogm.session.delegates;

import org.neo4j.ogm.cypher.compiler.CypherContext;
import org.neo4j.ogm.mapper.EntityGraphMapper;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.response.Neo4jResponse;
import org.neo4j.ogm.session.transaction.Transaction;

import java.util.Arrays;
import java.util.List;

/**
 * @author: Vince Bickers
 */
public class SaveDelegate implements Capability.Save {

    private final Neo4jSession session;

    public SaveDelegate(Neo4jSession neo4jSession) {
        this.session = neo4jSession;
    }

    @Override
    public <T> void save(T object) {
        if (object.getClass().isArray() || Iterable.class.isAssignableFrom(object.getClass())) {
            saveAll(object, -1);
        } else {
            save(object, -1); // default : full tree of changed objects
        }
    }

    private <T> void saveAll(T object, int depth) {
        List<T> list;
        if (object.getClass().isArray()) {
            list = Arrays.asList(object);
        } else {
            list = (List<T>) object;
        }
        for (T element : list) {
            save(element, depth);
        }
    }

    @Override
    public <T> void save(T object, int depth) {
        if (object.getClass().isArray() || Iterable.class.isAssignableFrom(object.getClass())) {
            saveAll(object, depth);
        } else {
            ClassInfo classInfo = session.metaData().classInfo(object);
            if (classInfo != null) {
                Transaction tx = session.ensureTransaction();
                CypherContext context = new EntityGraphMapper(session.metaData(), session.context()).map(object, depth);
                try (Neo4jResponse<String> response = session.requestHandler().execute(context.getStatements(), tx.url())) {
                    session.responseHandler().updateObjects(context, response, session.mapper());
                    tx.append(context);
                }
            } else {
                session.info(object.getClass().getName() + " is not an instance of a persistable class");
            }
        }
    }

}
