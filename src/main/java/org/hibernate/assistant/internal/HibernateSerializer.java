package org.hibernate.assistant.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.assistant.AiQuery;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

public class HibernateSerializer {
	private final SessionFactoryImplementor factory;

	public HibernateSerializer(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public String serializeToString(Object result, AiQuery<?> query, Class<?> resultType) {
		if ( result == null ) {
			return "<null>";
		}

		if ( resultType != null && resultType != Object.class ) {
			if ( resultType.isArray() ) {
				Object[] array = (Object[]) result;
				List<String> results = new ArrayList<>( array.length );
				for ( Object r : array ) {
					results.add( serializeToString( r, query, r == null ? null : r.getClass() ) );
				}
				return "[" + String.join( ",", results ) + "]";
			}
			else {
				final JpaMetamodelImplementor metamodel = factory.getRuntimeMetamodels().getJpaMetamodel();
				final ManagedDomainType<?> managedType = metamodel.findManagedType( resultType );
				if ( managedType instanceof EntityDomainType<?> entityType ) {
					final EntityPersister entityDescriptor = metamodel.getMappingMetamodel().getEntityDescriptor(
							entityType.getHibernateEntityName() );
					return toString( result, entityDescriptor.getEntityName() );
				}
				else if ( managedType instanceof EmbeddableDomainType<?> embeddable ) {
					// todo : we need to retrieve the ComponentType for this embeddable
					final ComponentType componentType = getComponentType( query, factory );
					if ( componentType != null ) {
						return toString( result, componentType );
					}
				}
				// todo : we should also specially handle mapped-superclass typed results
				else {
					// resort to resolving based on Hibernate's knowledge of the type
					final JavaType<Object> descriptor = factory.getTypeConfiguration()
							.getJavaTypeRegistry()
							.getDescriptor( resultType );
					if ( descriptor != null ) {
						// todo : we might do better than this if we could resolve the org.hibernate.Type
						return descriptor.toString( result );
					}
				}
			}
		}

		// As a last stand, just rely on the object's toString() method
		return result.toString();
	}

	/**
	 * Renders an entity to a string.
	 *
	 * @param entity an actual entity object, not a proxy!
	 * @param entityName the entity name
	 *
	 * @return the entity rendered to a string
	 */
	private String toString(Object entity, String entityName) throws HibernateException {
		final EntityPersister entityPersister = factory.getMappingMetamodel()
				.getEntityDescriptor( entityName );

		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Not a valid entity name: " + entityName );
		}
		else if ( !entityPersister.isInstance( entity ) ) {
			throw new IllegalArgumentException( "Provided object is not an instance of " + entityName );
		}

		final Map<String, String> result = new HashMap<>();

		if ( entityPersister.hasIdentifierProperty() ) {
			result.put(
					entityPersister.getIdentifierPropertyName(),
					entityPersister.getIdentifierType().toLoggableString(
							entityPersister.getIdentifier( entity, (SharedSessionContractImplementor) null ),
							factory
					)
			);
		}

		final Type[] types = entityPersister.getPropertyTypes();
		final String[] names = entityPersister.getPropertyNames();
		final Object[] values = entityPersister.getValues( entity );
		renderAttributeValues( types, names, values, result );
		return entityName + "=" + result;
	}

	private String toString(Object value, Type type) {
		assert type != null;
		// We know the property is initialized here, explore associations
		switch ( type ) {
			case EntityType entityType -> {
				return toString( value, entityType.getName() );
			}
			case CollectionType collectionType -> {
				final List<String> list = new ArrayList<>();
				final Type elementType = collectionType.getElementType( factory );
				final Iterator<?> elementsIterator = collectionType.getElementsIterator( value );
				while ( elementsIterator.hasNext() ) {
					final Object element = elementsIterator.next();
					list.add( toString( element, elementType ) );
				}
				return list.toString();
			}
			case ComponentType componentType -> {
				return toString( value, componentType );
			}
			default -> {
				return type.toLoggableString( value, factory );
			}
		}

		// todo : handle mapped-superclass types ?
	}

	private String toString(Object value, ComponentType componentType) {
		final Map<String, String> result = new HashMap<>();
		final Type[] types = componentType.getSubtypes();
		final String[] names = componentType.getPropertyNames();
		final Object[] values = componentType.getPropertyValues( value );
		renderAttributeValues( types, names, values, result );
		return result.toString();
	}

	private void renderAttributeValues(Type[] types, String[] names, Object[] values, Map<String, String> result) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( !names[i].startsWith( "_" ) ) {
				final String strValue;
				if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
					strValue = values[i].toString();
				}
				else if ( !Hibernate.isInitialized( values[i] ) ) {
					strValue = "<uninitialized>";
				}
				else {
					strValue = toString( values[i], types[i] );
				}
				result.put( names[i], strValue );
			}
		}
	}

	private static ComponentType getComponentType(AiQuery<?> query, SessionFactoryImplementor factory) {
		// tries to extract the ComponentType from the query, assuming it's the only selection
		final SqmSelectStatement<?> sqm = query.getSqmStatement();
		final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
		if ( selections.size() == 1 ) {
			final SqmSelectableNode<?> selectableNode = selections.getFirst().getSelectableNode();
			if ( selectableNode instanceof SqmPath<?> path ) {
				return getComponentType( path, factory );
			}
		}
		return null;
	}

	private static ComponentType getComponentType(SqmPath<?> path, SessionFactoryImplementor factory) {
		// todo : this needs to be reviewed / improved to handle most cases
		// tries to extract the ComponentType from a path
		final org.hibernate.type.Type propertyType;
		final SqmPath<?> lhs = path.getLhs();
		final String localName = path.getNavigablePath().getLocalName();
		if ( lhs instanceof SqmRoot<?> root ) {
			propertyType = getPropertyType( root.getEntityName(), localName, factory );
		}
		else if ( lhs instanceof SqmEntityJoin<?> join ) {
			propertyType = getPropertyType( join.getEntityName(), localName, factory );
		}
		else {
			// assume lhs is another component
			final ComponentType componentType = getComponentType( lhs, factory );
			assert componentType != null;
			final int propertyIndex = componentType.getPropertyIndex( localName );
			propertyType = componentType.getSubtypes()[propertyIndex];
		}

		if ( propertyType instanceof ComponentType componentType ) {
			return componentType;
		}
		else {
			return null;
		}
	}

	private static Type getPropertyType(String entityName, String propertyName, SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( entityName );
		//noinspection removal
		return entityDescriptor.getPropertyType( propertyName );
	}
}