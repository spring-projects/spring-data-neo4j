package org.springframework.data.graph.neo4j.conversion;

/**
* @author mh
* @since 28.06.11
*/
public interface ResultConverter<T, R> {
    R convert(T value, Class<R> type);

    ResultConverter NO_OP_RESULT_CONVERTER = new ResultConverter() {
        @Override
        public Object convert(Object value, Class type) {
            return null;
        }
    };

}
