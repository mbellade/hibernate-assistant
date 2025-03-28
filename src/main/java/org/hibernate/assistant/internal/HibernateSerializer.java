package org.hibernate.assistant.internal;

import org.hibernate.Hibernate;
import org.hibernate.assistant.AiQuery;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.type.descriptor.java.JavaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class HibernateSerializer {
	private final SessionFactoryImplementor factory;
	private final ObjectMapper objectMapper;

	private Map<String, IdentitySet<Object>> circularityTracker;

	public HibernateSerializer(SessionFactoryImplementor factory) {
		this.factory = factory;
		this.objectMapper = new ObjectMapper();
	}

	public String serializeToString(List<?> resultList, AiQuery<?> query) throws JsonProcessingException {
		if ( resultList.isEmpty() ) {
			return "[]";
		}

		final String result;
		if ( resultList.size() == 1 ) {
			result = renderValue( resultList.getFirst(), query, objectMapper, factory );
		}
		else {
			final List<String> results = new ArrayList<>( resultList.size() );
			for ( final Object value : resultList ) {
				results.add( renderValue( value, query, objectMapper, factory ) );
			}
			result = results.toString();
		}

		if ( circularityTracker != null ) {
			circularityTracker.clear();
			circularityTracker = null;
		}

		return result;
	}

	private String renderValue(
			Object value,
			AiQuery<?> query,
			ObjectMapper objectMapper,
			SessionFactoryImplementor factory) throws JsonProcessingException {
		final SqmSelectStatement<?> sqm = query.getSqmStatement();
		final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
		final ArrayList<String> result = new ArrayList<>( selections.size() );
		for ( SqmSelection<?> selection : selections ) {
			result.add( renderValue( value, selection.getSelectableNode(), objectMapper, factory ) );
		}
		return result.size() == 1 ? result.getFirst() : result.toString();
	}

	private String renderValue(
			Object value,
			Selection<?> selection,
			ObjectMapper objectMapper,
			SessionFactoryImplementor factory) throws JsonProcessingException {
		return switch ( selection ) {
			case SqmRoot<?> root -> {
				final EntityPersister persister = factory.getMappingMetamodel()
						.getEntityDescriptor( root.getEntityName() );
				yield objectMapper.writeValueAsString( getEntity( value, persister ) );
			}
			case SqmPath<?> path -> {
				// extract the attribute from the path
				final AttributeMapping attributeMapping = getAttributeMapping(
						path.getLhs(),
						path.getNavigablePath().getLocalName(),
						factory
				);
				yield attributeMapping != null ?
						objectMapper.writeValueAsString( getAttributeValue( value, attributeMapping ) ) :
						toString( path, value );
			}
			case SqmJpaCompoundSelection<?> compoundSelection -> {
				final List<Selection<?>> compoundSelectionItems = compoundSelection.getCompoundSelectionItems();
				assert compoundSelectionItems.size() > 1;
				final List<String> results = new ArrayList<>( compoundSelectionItems.size() );
				for ( int j = 0; j < compoundSelectionItems.size(); j++ ) {
					results.add( renderValue(
							getValue( value, j ),
							compoundSelectionItems.get( j ),
							objectMapper,
							factory
					) );
				}
				yield results.toString();
			}
			case SqmExpressibleAccessor<?> node -> toString( node, value );
			case null, default -> value.toString(); // best effort
		};
	}

	private static String toString(SqmExpressibleAccessor<?> node, Object value) {
		//noinspection unchecked
		final SqmExpressible<Object> expressible = (SqmExpressible<Object>) node.getExpressible();
		return expressible != null ?
				expressible.getExpressibleJavaType().toString( value ) :
				value.toString(); // best effort
	}

	private static Object getValue(Object value, int index) {
		if ( value.getClass().isArray() ) {
			return ( (Object[]) value )[index];
		}
		else if ( value instanceof Tuple tuple ) {
			return tuple.get( index );
		}
		else {
			if ( index > 0 ) {
				throw new IllegalArgumentException( "Index out of range: " + index );
			}
			return value;
		}
	}

	private static AttributeMapping getAttributeMapping(
			SqmPath<?> path,
			String propertyName,
			SessionFactoryImplementor factory) {
		if ( path instanceof SqmRoot<?> root ) {
			return getAttributeMapping( root.getEntityName(), propertyName, factory );
		}
		else if ( path instanceof SqmEntityJoin<?> join ) {
			return getAttributeMapping( join.getEntityName(), propertyName, factory );
		}
		else {
			// must be an embedded
			final AttributeMapping attributeMapping = getAttributeMapping(
					path.getLhs(),
					path.getNavigablePath().getLocalName(),
					factory
			);
			final EmbeddedAttributeMapping embedded = attributeMapping != null ?
					attributeMapping.asEmbeddedAttributeMapping() :
					null;
			if ( embedded != null ) {
				return embedded.getEmbeddableTypeDescriptor().findAttributeMapping( propertyName );
			}
		}
		return null;
	}

	private static AttributeMapping getAttributeMapping(
			String entityName,
			String propertyName,
			SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( entityName );
		return entityDescriptor.findAttributeMapping( propertyName );
	}

	private Object getAttributeValue(Object value, ValuedModelPart part) {
		// null / unfeched / lazy properties
		if ( value == null ) {
			return "null";
		}
		else if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return value.toString();
		}
		else if ( !Hibernate.isInitialized( value ) ) {
			return "<uninitialized>";
		}

		// basic
		final BasicValuedModelPart basic = part.asBasicValuedModelPart();
		if ( basic != null ) {
			return getBasicValue( value, basic );
		}

		// entity
		if ( part instanceof EntityValuedModelPart entity ) {
			return getEntity( value, entity.getEntityMappingType().getEntityPersister() );
		}

		final AttributeMapping attributeMapping = part.asAttributeMapping();
		if ( attributeMapping != null ) {
			// embedded
			final EmbeddedAttributeMapping embedded = attributeMapping.asEmbeddedAttributeMapping();
			if ( embedded != null ) {
				return getManagedTypeProperties( value, embedded.getEmbeddableTypeDescriptor() );
			}

			// plural
			final PluralAttributeMapping plural = attributeMapping.asPluralAttributeMapping();
			if ( plural != null ) {
				final CollectionPart element = plural.getElementDescriptor();
				final CollectionSemantics<?, ?> collectionSemantics = plural.getMappedType().getCollectionSemantics();
				switch ( collectionSemantics.getCollectionClassification() ) {
					case MAP:
						final PersistentMap<?, ?> pm = (PersistentMap<?, ?>) value;
						final Map<Object, Object> map = new HashMap<>( pm.size() );
						collectMapProperties( pm, plural.getIndexDescriptor(), element, map );
						return map;
					case SORTED_MAP:
					case ORDERED_MAP:
						final PersistentMap<?, ?> pm1 = (PersistentMap<?, ?>) value;
						final SortedMap<Object, Object> sortedMap = new TreeMap<>();
						collectMapProperties( pm1, plural.getIndexDescriptor(), element, sortedMap );
						return sortedMap;
					default:
						final PersistentCollection<?> pc = (PersistentCollection<?>) value;
						final List<Object> list = new ArrayList<>( pc.getSize() );
						pc.entries( plural.getCollectionDescriptor() ).forEachRemaining( v -> list.add(
								getCollectionPartValue( v, element ) )
						);
						return list;
				}
			}
		}

		throw new IllegalArgumentException( "Unsupported attribute type '" + part.getPartName() + "'" );
	}

	private static Object getBasicValue(Object value, BasicValuedModelPart basic) {
		if ( value instanceof Number ) {
			// numeric values can be left as-is
			return value;
		}
		else {
			//noinspection unchecked
			final JavaType<Object> javaType = (JavaType<Object>) basic.getJavaType();
			return javaType.toString( value );
		}
	}

	private Object getEntity(Object value, EntityPersister persister) {
		// We need to handle circular relationships here
		final boolean encountered = wasEntityEncountered( value, persister );
		if ( !encountered ) {
			final Map<String, Object> props = new HashMap<>( persister.getNumberOfAttributeMappings() + 1 );
			// extract the identifier separately
			final EntityIdentifierMapping identifierMapping = persister.getIdentifierMapping();
			props.put(
					identifierMapping.getAttributeName(),
					getAttributeValue( identifierMapping.getIdentifier( value ), identifierMapping )
			);
			// process all remaining attributes
			props.putAll( getManagedTypeProperties( value, persister ) );
			circularityTracker.get( persister.getEntityName() ).remove( value );
			return props;
		}
		else {
			return entityIdentityString( value, persister );
		}
	}

	private Map<String, Object> getManagedTypeProperties(Object value, ManagedMappingType managedType) {
		final Map<String, Object> properties = new HashMap<>( managedType.getNumberOfAttributeMappings() );
		managedType.forEachAttributeMapping( a -> properties.put(
				a.getAttributeName(),
				getAttributeValue( a.getValue( value ), a )
		) );
		return properties;
	}

	private static String entityIdentityString(Object value, EntityPersister entityType) {
		final EntityIdentifierMapping identifierMapping = entityType.getIdentifierMapping();
		final Object identifier = identifierMapping.getIdentifier( value );
		// note : using #toLoggableString should be enough here
		return entityType.getEntityName() + "#" + entityType.getIdentifierType().toLoggableString(
				identifier,
				entityType.getFactory()
		);
	}

	private Object getCollectionPartValue(Object value, CollectionPart collectionPart) {
		final BasicValuedModelPart basic = collectionPart.asBasicValuedModelPart();
		if ( basic != null ) {
			return getBasicValue( value, basic );
		}

		if ( collectionPart instanceof EmbeddedCollectionPart embedded ) {
			return getManagedTypeProperties( value, embedded.getEmbeddableTypeDescriptor() );
		}

		if ( collectionPart instanceof EntityCollectionPart entity ) {
			return getEntity( value, entity.getEntityMappingType().getEntityPersister() );
		}

		// todo : many-to-any ?
		throw new IllegalArgumentException( "Unsupported collection part type '" + collectionPart.getClass()
				.getName() + "'" );
	}

	private <K, E> void collectMapProperties(
			PersistentMap<K, E> map,
			CollectionPart key,
			CollectionPart value,
			Map<Object, Object> properties) {
		for ( final Map.Entry<K, E> entry : map.entrySet() ) {
			properties.put(
					getCollectionPartValue( entry.getKey(), key ),
					getCollectionPartValue( entry.getValue(), value )
			);
		}
	}

	/**
	 * Tracks the provided {@code entity} using {@link #circularityTracker},
	 * and returns {@code true} if it was previously encountered.
	 *
	 * @param entity the entity instance to track
	 * @param entityType the type of the entity instance
	 *
	 * @return {@code true} if the entity was already encountered, {@code false} otherwise
	 */
	private boolean wasEntityEncountered(Object entity, EntityMappingType entityType) {
		if ( circularityTracker == null ) {
			circularityTracker = new HashMap<>();
		}
		return !circularityTracker.computeIfAbsent( entityType.getEntityName(), k -> new IdentitySet<>() )
				.add( entity );
	}
}