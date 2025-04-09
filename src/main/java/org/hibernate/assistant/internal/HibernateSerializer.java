package org.hibernate.assistant.internal;

import org.hibernate.assistant.AiQuery;
import org.hibernate.dialect.JsonHelper;
import org.hibernate.dialect.JsonHelper.JsonAppender;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Selection;
import java.util.List;
import java.util.Map;

public class HibernateSerializer {
	private final SessionFactoryImplementor factory;

	private Map<String, IdentitySet<Object>> circularityTracker;

	public HibernateSerializer(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public String serializeToString(List<?> resultList, AiQuery<?> query) throws JsonProcessingException {
		if ( resultList.isEmpty() ) {
			return "[]";
		}

		final StringBuilder sb = new StringBuilder();
		final JsonAppender jsonAppender = new JsonAppender( sb, true );
		char separator = '[';
		for ( final Object value : resultList ) {
			sb.append( separator );
			renderValue( value, query, jsonAppender, factory );
			separator = ',';
		}
		sb.append( ']' );
		return sb.toString();
	}

	private void renderValue(
			Object value,
			AiQuery<?> query,
			JsonAppender jsonAppender,
			SessionFactoryImplementor factory) throws JsonProcessingException {
		final SqmSelectStatement<?> sqm = query.getSqmStatement();
		final List<SqmSelection<?>> selections = sqm.getQuerySpec().getSelectClause().getSelections();
		assert !selections.isEmpty();
		if ( selections.size() == 1 ) {
			renderValue( value, selections.getFirst().getSelectableNode(), jsonAppender, factory );
		}
		else {
			// wrap each result tuple in square brackets
			char separator = '[';
			for ( SqmSelection<?> selection : selections ) {
				jsonAppender.append( separator );
				renderValue( value, selection.getSelectableNode(), jsonAppender, factory );
				separator = ',';
			}
			jsonAppender.append( ']' );
		}
	}

	private void renderValue(
			Object value,
			Selection<?> selection,
			JsonAppender jsonAppender,
			SessionFactoryImplementor factory) throws JsonProcessingException {
		switch ( selection ) {
			case SqmRoot<?> root -> {
				final EntityPersister persister = factory.getMappingMetamodel()
						.getEntityDescriptor( root.getEntityName() );
				JsonHelper.toString(
						value,
						persister.getEntityMappingType(),
						factory.getWrapperOptions(),
						jsonAppender
				);
			}
			case SqmPath<?> path -> {
				// extract the attribute from the path
				final AttributeMapping attributeMapping = getAttributeMapping(
						path.getLhs(),
						path.getNavigablePath().getLocalName(),
						factory
				);
				if ( attributeMapping != null ) {
					JsonHelper.toString(
							value,
							attributeMapping.getMappedType(),
							factory.getWrapperOptions(),
							jsonAppender
					);
				}
				else {
					jsonAppender.append( expressibleToString( path, value ) );
				}
			}
			case SqmJpaCompoundSelection<?> compoundSelection -> {
				final List<Selection<?>> compoundSelectionItems = compoundSelection.getCompoundSelectionItems();
				assert compoundSelectionItems.size() > 1;
				char separator = '[';
				for ( int j = 0; j < compoundSelectionItems.size(); j++ ) {
					jsonAppender.append( separator );
					renderValue(
							getValue( value, j ),
							compoundSelectionItems.get( j ),
							jsonAppender,
							factory
					);
					separator = ',';
				}
				jsonAppender.append( ']' );
			}
			case SqmExpressibleAccessor<?> node -> jsonAppender.append( expressibleToString( node, value ) );
			case null, default -> jsonAppender.append( value.toString() ); // best effort
		}
	}

	private static String expressibleToString(SqmExpressibleAccessor<?> node, Object value) {
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
		else if ( path instanceof SqmEntityJoin<?, ?> join ) {
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
}