package org.neo4j.rest.graphdb.query;

import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 28.09.14
 */
public class CypherTransactionExecutionException extends RuntimeException {
    private final List<CypherTransaction.Statement> statements;
    private final List<Map<String, String>> errors;

    public CypherTransactionExecutionException(String msg, List<CypherTransaction.Statement> statements, List<Map<String,String>> errors) {
        super(msg + errors.toString());
        this.statements = statements;
        this.errors = errors;
    }

    public List<CypherTransaction.Statement> getStatements() {
        return statements;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }

    public boolean contains(String code, String message) {
        for (Map<String, String> error : errors) {
            if (!code.equals(error.get("code"))) continue;
            String msg = error.get("message");
            if (msg != null && msg.contains(message)) return true;
        }
        return false;
    }
}
