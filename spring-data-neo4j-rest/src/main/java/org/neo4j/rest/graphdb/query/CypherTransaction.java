package org.neo4j.rest.graphdb.query;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.neo4j.rest.graphdb.*;

import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Arrays.asList;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

/**
 * @author mh
 * @since 24.09.14
 */
public class CypherTransaction {

    @SuppressWarnings("unchecked")
    public enum ResultType {
        /** all nodes and rels collated in one bag **/
        graph() {
            @Override
            public List<Object> get(Map data) {
                Map graph = (Map) data.get(name());
                List result = new ArrayList((List)graph.get("nodes"));
                result.addAll((List) graph.get("relationships"));
                return result;
            }
        }, row, rest;

        public List<Object> get(Map data) {
            return (List<Object>) data.get(name());
        }
    }

    public CypherTransaction(String baseUri, ResultType type) {
        this.type = type;
        this.request = new ExecutingRestRequest(baseUri);
    }
    public CypherTransaction(RestAPICypherImpl restAPI, ResultType type) {
        this.type = type;
        this.request = restAPI.getRestRequest();
    }

    public static class Result implements Iterable<Map<String,Object>> {
        private final List<String> columns;
        private final Iterable<List<Object>> rows;
        private final Statement statement;

        Result(List<String> columns, Iterable<List<Object>> rows, Statement statement) {
            this.columns = columns;
            this.rows = rows;
            this.statement = statement;
        }

        private static List<Result> toResults(List<Map> resultsData, List<Statement> statements, ResultType type) {
            List<Result> results=new ArrayList<>();
            for (int i = 0; i < resultsData.size(); i++) {
                results.add(toResult(resultsData.get(i), statements.get(i), type));
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        private static Result toResult(Map resultData, Statement statement, final ResultType type) {
            List<String> columns = (List<String>) resultData.get("columns");
            List<Map> rowsData = (List<Map>) resultData.get("data");
            Iterable<List<Object>> rows = new IterableWrapper<List<Object>,Map>(rowsData) {
                protected List<Object> underlyingObjectToObject(Map map) {
                    return type.get(map);
                }
            };
            return new Result(columns, rows, statement);
        }

        public List<String> getColumns() {
            return columns;
        }

        public Iterable<List<Object>> getRows() {
            return rows;
        }

        public Statement getStatement() {
            return statement;
        }

        @Override
        public Iterator<Map<String, Object>> iterator() {
            return new IteratorWrapper<Map<String, Object>,List<Object>>(rows.iterator()) {
                protected Map<String, Object> underlyingObjectToObject(List<Object> objects) {
                    Map<String, Object> row = new LinkedHashMap<>(columns.size());
                    for (int i = 0; i < columns.size(); i++) {
                        row.put(columns.get(i), objects.get(i));
                    }
                    return row;
                }
            };
        }

        public boolean hasData() {
            return rows.iterator().hasNext();
        }
    }

    public static class Statement {
        private final String statement;
        private final ResultType type;
        private final Map<String, Object> parameters;

        public Statement(String query, Map<String, Object> parameters, ResultType type) {
            this.statement = query;
            this.type = type;
            this.parameters = parameters == null ? Collections.<String,Object>emptyMap() : parameters;
        }

        public String getStatement() {
            return statement;
        }

        public Map<String, Object> getParameters() {
            return new LinkedHashMap<>(parameters);
        }

        public List<String> getResultDataContents() {
            return Collections.singletonList(type.name());
        }
    }

    private final ResultType type;
    private String transactionUrl = null;
    private String commitUrl = null;
    private final RestRequest request;
    private final List<Statement> statements = new ArrayList<>(10);

    public void add(String statement, Map<String,Object> params) {
        statements.add(new Statement(statement,params,type));
    }


    public Result send(String statement, Map<String,Object> params) {
        add(statement,params);
        List<Result> results = send(transactionUrl());
        if (results.size() > 0) return results.get(results.size() - 1);
        throw new CypherTransactionExecutionException("Error Sending",asList(new Statement(statement,params,type)),errors("No.Results","No Results after single send"));
    }

    public Result commit(String statement, Map<String,Object> params) {
        add(statement,params);
        List<Result> results = commit();
        if (results.size() > 0) return results.get(results.size() - 1);
        else throw new CypherTransactionExecutionException("Error Sending",asList(new Statement(statement,params,type)),errors("No.Results","No Results after single commit"));
    }

    public List<Result> send() {
        return send(transactionUrl());
    }

    public List<Result> commit() {
        try {
            if (statements.isEmpty()) add("return 1",null); // TODO hacking workaround b/c of periodic commit check in server accesses the first of an empty statement list with an NPE
            return send(commitUrl());
        } finally {
            commitUrl = null;
        }
    }

    private List<Result> send(String url) {
        try {
            RequestResult result = request.post(url, map("statements", statements));
            if (result.statusIs(Response.Status.OK) || result.statusIs(Response.Status.CREATED)) {
                ArrayList<Statement> statementsCopy = new ArrayList<>(statements);
                return Result.toResults(handleResult(result,statementsCopy), statementsCopy, type);
            } else {
                List<Map<String, String>> errors = errors("Http." + result.getStatus(), result.getText());
                throw new CypherTransactionExecutionException("Error executing statements: " + result.getStatus() +
                        " " + result.getText(),statements, errors);
            }
        } finally {
            statements.clear();
        }
    }

    private List<Map<String, String>> errors(String code, String message) {
        return asList(stringMap("code", code, "message", message));
    }

    public void rollback() {
        if (transactionUrl != null) {
            request.delete(transactionUrl);
        }
        transactionUrl = null;
        commitUrl = null;
    }

    private String commitUrl() {
        return (commitUrl == null) ? "transaction/commit" : commitUrl;
    }

    private String transactionUrl() {
        return (transactionUrl==null) ? "transaction" : transactionUrl;
    }

    @SuppressWarnings("unchecked")
    private List<Map> handleResult(RequestResult result, ArrayList<Statement> statements) {
        Map<?, ?> resultData = result.toMap();
        List<Map<String,String>> errors = (List<Map<String, String>>) resultData.get("errors");
        if (errors != null && !errors.isEmpty()) throw new CypherTransactionExecutionException("Error executing cypher statements ",statements, errors);
        if (result.statusIs(ClientResponse.Status.CREATED)) transactionUrl = result.getLocation();
        commitUrl = (String) resultData.get("commit");
        return (List<Map>) resultData.get("results");
    }

    @Override
    public String toString() {
        return "Transaction: "+transactionUrl;
    }
}
