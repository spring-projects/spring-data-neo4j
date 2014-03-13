package org.springframework.data.neo4j.support.query;

import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;

import java.util.Map;

/**
 * @author mh
 * @since 12.03.14
 */
public interface CypherQueryEngine extends  QueryEngine<Map<String,Object>> {
    ResultConverter getResultConverter();

    void setResultConverter(ResultConverter resultConverter);
}
