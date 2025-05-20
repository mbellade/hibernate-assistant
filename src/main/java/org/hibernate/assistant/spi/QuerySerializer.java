package org.hibernate.assistant.spi;

import org.hibernate.dialect.JsonHelper;
import org.hibernate.dialect.JsonHelper.JsonAppender;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Selection;
import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

public class QuerySerializer {
	public static String serializeToString(
			List<?> resultList,
			SelectionQuery<?> query,
			SessionFactoryImplementor factory) {
		if ( resultList.isEmpty() ) {
			return "[]";
		}

		final StringBuilder sb = new StringBuilder();
		final JsonAppender jsonAppender = new JsonAppender( sb, true );
		char separator = '[';
		for ( final Object value : resultList ) {
			sb.append( separator );
			renderValue( value, (SqmSelectionQuery<?>) query, jsonAppender, factory );
			separator = ',';
		}
		sb.append( ']' );
		return sb.toString();
	}

	private static void renderValue(
			Object value,
			SqmSelectionQuery<?> query,
			JsonAppender jsonAppender,
			SessionFactoryImplementor factory) {
		final SqmStatement<?> sqm = query.getSqmStatement();
		if ( !( sqm instanceof SqmSelectStatement<?> sqmSelect ) ) {
			throw new IllegalArgumentException( "Query is not a select statement." );
		}
		final List<SqmSelection<?>> selections = sqmSelect.getQuerySpec().getSelectClause().getSelections();
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

	private static void renderValue(
			Object value,
			Selection<?> selection,
			JsonAppender jsonAppender,
			SessionFactoryImplementor factory) {
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
				final ValuedModelPart subPart = getSubPart(
						path.getLhs(),
						path.getNavigablePath().getLocalName(),
						factory
				);
				if ( subPart != null ) {
					JsonHelper.toString( value, subPart, factory.getWrapperOptions(), jsonAppender, null );
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
			case null, default -> jsonAppender.append( "\"" ).append( value.toString() ).append( "\"" ); // best effort
		}
	}

	private static String expressibleToString(SqmExpressibleAccessor<?> node, Object value) {
		//noinspection unchecked
		final SqmExpressible<Object> expressible = (SqmExpressible<Object>) node.getExpressible();
		final String result = expressible != null ?
				expressible.getExpressibleJavaType().toString( value ) :
				value.toString(); // best effort
		// avoid quoting numbers as they can be represented in JSON
		return value instanceof Number ? result : "\"" + result + "\"";
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

	private static ValuedModelPart getSubPart(
			SqmPath<?> path,
			String propertyName,
			SessionFactoryImplementor factory) {
		if ( path instanceof SqmRoot<?> root ) {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel()
					.getEntityDescriptor( root.getEntityName() );
			return entityDescriptor.findAttributeMapping( propertyName );
		}
		else {
			// try to derive the subpart from the lhs
			final ValuedModelPart subPart = getSubPart(
					path.getLhs(),
					path.getNavigablePath().getLocalName(),
					factory
			);
			if ( subPart instanceof EmbeddableValuedModelPart embeddable ) {
				return embeddable.getEmbeddableTypeDescriptor().findAttributeMapping( propertyName );
			}
			else if ( subPart instanceof EntityValuedModelPart entity ) {
				return entity.getEntityMappingType().findAttributeMapping( propertyName );
			}
			else if ( subPart instanceof PluralAttributeMapping plural ) {
				final CollectionPart.Nature nature = castNonNull( CollectionPart.Nature.fromNameExact( propertyName ) );
				return switch ( nature ) {
					case ELEMENT -> plural.getElementDescriptor();
					case ID -> plural.getIdentifierDescriptor();
					case INDEX -> plural.getIndexDescriptor();
				};
			}
		}
		return null;
	}
}