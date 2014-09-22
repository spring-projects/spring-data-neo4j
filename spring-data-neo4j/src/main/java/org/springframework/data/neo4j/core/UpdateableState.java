package org.springframework.data.neo4j.core;

import java.util.Collection;
import java.util.Map;

/**
 * @author mh
 * @since 21.09.14
 */
public interface UpdateableState {
    void refresh();
    void flush();
    void track();
    void addPropertiesBatch(Map<String, Object> properties);
    void addAllLabelsBatch(Collection<String> labels);
}
