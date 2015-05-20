package org.neo4j.ogm.session.delegates;

import org.apache.commons.lang.StringUtils;
import org.neo4j.ogm.cypher.query.RowModelQueryWithStatistics;
import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Utils;
import org.neo4j.ogm.session.response.Neo4jResponse;
import org.neo4j.ogm.session.result.QueryStatistics;

import java.util.Map;

/**
 * @author: Vince Bickers
 */
public class ExecuteStatementsDelegate implements Capability.ExecuteStatements {

    private final Neo4jSession session;

    public ExecuteStatementsDelegate(Neo4jSession neo4jSession) {
        this.session = neo4jSession;
    }

    @Override
    public QueryStatistics execute(String cypher, Map<String, Object> parameters) {
        if (StringUtils.isEmpty(cypher)) {
            throw new RuntimeException("Supplied cypher statement must not be null or empty.");
        }

        if (parameters == null) {
            throw new RuntimeException("Supplied Parameters cannot be null.");
        }
        assertNothingReturned(cypher);
        String url  = session.ensureTransaction().url();
        // NOTE: No need to check if domain objects are parameters and flatten them to json as this is done
        // for us using the existing execute() method.
        RowModelQueryWithStatistics parameterisedStatement = new RowModelQueryWithStatistics(cypher, parameters);
        try (Neo4jResponse<QueryStatistics> response = session.requestHandler().execute(parameterisedStatement, url)) {
            return response.next();
        }
    }

    @Override
    public QueryStatistics execute(String statement) {
        if (StringUtils.isEmpty(statement)) {
            throw new RuntimeException("Supplied cypher statement must not be null or empty.");
        }
        assertNothingReturned(statement);
        RowModelQueryWithStatistics parameterisedStatement = new RowModelQueryWithStatistics(statement, Utils.map());
        String url = session.ensureTransaction().url();
        try (Neo4jResponse<QueryStatistics> response = session.requestHandler().execute(parameterisedStatement, url)) {
            return response.next();
        }
    }


    private void assertNothingReturned(String cypher) {
        if (cypher.toUpperCase().contains(" RETURN ")) {
            throw new RuntimeException("execute() must not return data. Use query() instead.");
        }
    }

}
