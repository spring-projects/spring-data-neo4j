package org.springframework.data.neo4j.core.mapping;

import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.util.TypeInformation;
import reactor.util.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RelationshipStatementBuilder {

	private final Neo4jMappingContext neo4jMappingContext;

	private final Neo4jConversionService conversionService;

	public RelationshipStatementBuilder(Neo4jMappingContext neo4jMappingContext, Neo4jConversionService conversionService) {
		this.neo4jMappingContext = neo4jMappingContext;
		this.conversionService = conversionService;
	}


	public CreateRelationshipStatementHolder createStatementForImperativeSimpleRelationshipBatch(Neo4jPersistentEntity<?> neo4jPersistentEntity,
																								 RelationshipDescription relationshipDescription,
																								 List<Object> plainRelationshipRows, boolean canUseElementId) {
		return createStatementForSingleRelationship(neo4jPersistentEntity, (DefaultRelationshipDescription) relationshipDescription,
				plainRelationshipRows, canUseElementId);
	}

	public CreateRelationshipStatementHolder createStatementForImperativeRelationshipsWithPropertiesBatch(boolean isNew,
																										  Neo4jPersistentEntity<?> neo4jPersistentEntity,
																										  RelationshipDescription relationshipDescription,
																										  Object relatedValues,
																										  List<Map<String, Object>> relationshipPropertiesRows,
																										  boolean canUseElementId) {

		List<MappingSupport.RelationshipPropertiesWithEntityHolder> relationshipPropertyValues = ((Collection<?>) relatedValues).stream()
				.map(MappingSupport.RelationshipPropertiesWithEntityHolder.class::cast).collect(Collectors.toList());

		return createStatementForRelationshipWithPropertiesBatch(isNew, neo4jPersistentEntity, relationshipDescription,
				relationshipPropertyValues, relationshipPropertiesRows, canUseElementId);
	}

	public CreateRelationshipStatementHolder createStatementForSingleRelationship(Neo4jPersistentEntity<?> neo4jPersistentEntity,
																				  RelationshipDescription relationshipContext,
																				  Object relatedValue,
																				  boolean isNewRelationship,
																				  boolean canUseElementId) {

		if (relationshipContext.hasRelationshipProperties()) {
			MappingSupport.RelationshipPropertiesWithEntityHolder relatedValueEntityHolder =
					(MappingSupport.RelationshipPropertiesWithEntityHolder) (
							relatedValue instanceof MappingSupport.RelationshipPropertiesWithEntityHolder
									? relatedValue
									: ((Map.Entry<?, ?>) relatedValue).getValue() instanceof List
									? ((List<?>) ((Map.Entry<?, ?>) relatedValue).getValue()).get(0)
									: ((Map.Entry<?, ?>) relatedValue).getValue());

			String dynamicRelationshipType = null;
			if (relationshipContext.isDynamic()) {
				Neo4jPersistentProperty inverse = ((DefaultRelationshipDescription) relationshipContext).getInverse();
				TypeInformation<?> keyType = inverse.getTypeInformation()
						.getRequiredComponentType();
				Object key = ((Map.Entry) relatedValue).getKey();
				dynamicRelationshipType = conversionService.writeValue(key, keyType,
						inverse.getOptionalConverter()).asString();
			}
			return createStatementForRelationshipWithProperties(
					neo4jPersistentEntity, relationshipContext,
					dynamicRelationshipType, relatedValueEntityHolder, isNewRelationship, canUseElementId
			);
		} else {
			return createStatementForSingleRelationship(neo4jPersistentEntity, (DefaultRelationshipDescription) relationshipContext,
					relatedValue, canUseElementId);
		}
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithProperties(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationshipDescription, @Nullable String dynamicRelationshipType,
			MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue, boolean isNewRelationship, boolean canUseElementId) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationshipWithProperties(
				neo4jPersistentEntity, relationshipDescription, isNewRelationship,
				dynamicRelationshipType, canUseElementId);

		Map<String, Object> propMap = new HashMap<>();
		neo4jMappingContext.getEntityConverter().write(relatedValue.getRelationshipProperties(), propMap);

		return new CreateRelationshipStatementHolder(relationshipCreationQuery, propMap);
	}

	private CreateRelationshipStatementHolder createStatementForRelationshipWithPropertiesBatch(
			boolean isNew,
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationshipDescription,
			List<MappingSupport.RelationshipPropertiesWithEntityHolder> relatedValues,
			List<Map<String, Object>> relationshipPropertiesRows,
			boolean canUseElementId) {

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE
				.prepareUpdateOfRelationshipsWithProperties(neo4jPersistentEntity, relationshipDescription, isNew, canUseElementId);
		List<Object> relationshipRows = new ArrayList<>();
		Map<String, Object> relationshipPropertiesEntries = new HashMap<>();
		if (isNew) {
			for (int i = 0; i < relatedValues.size(); i++) {
				MappingSupport.RelationshipPropertiesWithEntityHolder relatedValue = relatedValues.get(i);
				Map<String, Object> propMap = relationshipPropertiesRows.get(i);
				neo4jMappingContext.getEntityConverter().write(relatedValue.getRelationshipProperties(), propMap);
				relationshipRows.add(propMap);
			}
		}
		relationshipPropertiesEntries.put(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM, relationshipRows);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery, relationshipPropertiesEntries);
	}

	private CreateRelationshipStatementHolder createStatementForSingleRelationship(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			DefaultRelationshipDescription relationshipDescription, Object relatedValue, boolean canUseElementId) {

		String relationshipType;
		if (!relationshipDescription.isDynamic()) {
			relationshipType = null;
		} else {
			Neo4jPersistentProperty inverse = relationshipDescription.getInverse();
			TypeInformation<?> keyType = inverse.getTypeInformation().getRequiredComponentType();
			Object key = ((Map.Entry<?, ?>) relatedValue).getKey();
			relationshipType = conversionService.writeValue(key, keyType, inverse.getOptionalConverter()).asString();
		}

		Statement relationshipCreationQuery = CypherGenerator.INSTANCE.prepareSaveOfRelationships(
				neo4jPersistentEntity, relationshipDescription, relationshipType, canUseElementId);
		return new CreateRelationshipStatementHolder(relationshipCreationQuery, Collections.emptyMap());
	}
}