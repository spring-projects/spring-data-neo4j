package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.annotations.Indexed;
import org.springframework.datastore.graph.annotations.NodeEntity;
import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;


@Configurable
class IndexingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Autowired
    private IndexService indexService;
    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;

    IndexingNodePropertyFieldAccessorListenerFactory(final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Field f) {
        return isPropertyField(f) && isIndexed(f);
    }

    private boolean isIndexed(final Field f) {
        final NodeEntity entityAnnotation = f.getDeclaringClass().getAnnotation(NodeEntity.class);
        if (entityAnnotation!=null && entityAnnotation.fullIndex()) return true;
        final Indexed propertyAnnotation = f.getAnnotation(Indexed.class);
        return propertyAnnotation!=null && propertyAnnotation.index();
    }

    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<NodeBacked,?> forField(final Field field) {
        return new IndexingNodePropertyFieldAccessorListener(field,indexService);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {
	    protected final Field field;
	    private final IndexService indexService;

	    public IndexingNodePropertyFieldAccessorListener(final Field field, final IndexService indexService) {
	        this.field = field;
	        this.indexService = indexService;
	    }

	    @Override
	    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
	        if (newVal==null) indexService.removeIndex(nodeBacked.getUnderlyingState(),getPropertyName());
	        else indexService.index(nodeBacked.getUnderlyingState(), getPropertyName(),newVal);
	    }

	    // todo
	    private String getPropertyName() {
	        return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
	    }

	}
}
