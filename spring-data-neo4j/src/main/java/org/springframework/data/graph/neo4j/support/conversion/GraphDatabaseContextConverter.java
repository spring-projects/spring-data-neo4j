package org.springframework.data.graph.neo4j.support.conversion;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.EntityPath;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.conversion.DefaultConverter;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.path.ConvertingEntityPath;

/**
 * @author mh
 * @since 28.06.11
 */
public class GraphDatabaseContextConverter extends DefaultConverter {
    private final GraphDatabaseContext ctx;
    private final ConversionService conversionService;

    public GraphDatabaseContextConverter(GraphDatabaseContext ctx) {
        this.ctx = ctx;
        conversionService = this.ctx.getConversionService();
    }

    @Override
    protected Object doConvert(Object value, Class<?> sourceType, Class targetType) {
        if (NodeBacked.class.isAssignableFrom(targetType)) {
            return ctx.createEntityFromState(toNode(value,sourceType),targetType);
        }
        if (RelationshipBacked.class.isAssignableFrom(targetType)) {
            return ctx.createEntityFromState(toRelationship(value,sourceType),targetType);
        }
        if (EntityPath.class.isAssignableFrom(targetType)) {
            return new ConvertingEntityPath(ctx,toPath(value,sourceType));
        }
        final Object result = super.doConvert(value, sourceType, targetType);

        if (result!=null) return result;

        if (conversionService.canConvert(sourceType, targetType)) {
            return conversionService.convert(value,targetType);
        }
        return result;
    }
}
