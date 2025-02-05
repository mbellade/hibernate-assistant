package org.hibernate.assistant.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

public class AssistantUtils {
	public static String getDomainModelPrompt(Metamodel metamodel) {
		final StringBuilder sb = new StringBuilder();
		for ( ManagedType<?> managedType : metamodel.getManagedTypes() ) {
			final String typeDescription = switch ( managedType.getPersistenceType() ) {
				case ENTITY -> getEntityTypeDescription( (EntityType<?>) managedType );
				case EMBEDDABLE -> getEmbeddableTypeDescription( (EmbeddableType<?>) managedType );
				case MAPPED_SUPERCLASS -> getMappedSuperclassTypeDescription( (MappedSuperclassType<?>) managedType );
				default ->
						throw new IllegalStateException( "Unexpected persistence type for managed type [" + managedType + "]" );
			};
			sb.append( typeDescription ).append( "\n" );
		}
		return sb.toString();
	}

	public static <T> String getEntityTypeDescription(EntityType<T> entityType) {
		return "\"" + entityType.getName() + "\" is an entity type.\n" + getJavaTypeDescription( entityType ) + getInheritanceDescription(
				(ManagedDomainType<?>) entityType ) + getIdentifierDescription( entityType ) + getAttributesDescription(
				entityType.getAttributes() );
	}

	public static String getJavaTypeDescription(ManagedType<?> managedType) {
		return "It corresponds to the java class \"" + managedType.getJavaType().getTypeName() + "\"\n";
	}

	public static String getInheritanceDescription(ManagedDomainType<?> managedType) {
		final ManagedDomainType<?> superType = managedType.getSuperType();
		return superType != null ? "It extends from the \"" + superType.getJavaType().getTypeName() + "\" type.\n" : "";
	}

	public static <T> String getMappedSuperclassTypeDescription(MappedSuperclassType<T> mappedSuperclass) {
		return "\"" + mappedSuperclass.getJavaType()
				.getSimpleName() + "\" is a mapped superclass type.\n" + getJavaTypeDescription( mappedSuperclass ) + getInheritanceDescription(
				(ManagedDomainType<?>) mappedSuperclass ) + getIdentifierDescription( mappedSuperclass ) + getAttributesDescription(
				mappedSuperclass.getAttributes() );
	}

	public static <T> String getIdentifierDescription(IdentifiableType<T> identifiableType) {
		final Type<?> idType = identifiableType.getIdType();
		final String description;
		if ( idType != null ) {
			final SingularAttribute<? super T, ?> id = identifiableType.getId( idType.getJavaType() );
			description = "Its identifier attribute is called \"" + id.getName() + "\" and is of type \"" + id.getJavaType()
					.getTypeName() + "\".\n";
		}
		else {
			description = "It has no identifier attribute.\n";
		}
		return description;
	}

	public static <T> String getEmbeddableTypeDescription(EmbeddableType<T> embeddableType) {
		return "\"" + embeddableType.getJavaType()
				.getSimpleName() + "\" is an embeddable type.\n" + getInheritanceDescription( (ManagedDomainType<?>) embeddableType ) + getJavaTypeDescription(
				embeddableType ) + getAttributesDescription( embeddableType.getAttributes() );
	}

	public static <T> String getAttributesDescription(Set<Attribute<? super T, ?>> attributes) {
		final StringBuilder sb = new StringBuilder( "Its attributes are (name => type):\n" );
		for ( final Attribute<? super T, ?> attribute : attributes ) {
			sb.append( "- \"" ).append( attribute.getName() ).append( "\" => \"" ).append( attribute.getJavaType()
																								   .getTypeName() );

			// add key and element types for plural attributes
			if ( attribute instanceof PluralAttribute<?, ?, ?> pluralAttribute ) {
				sb.append( "<" );
				final PluralAttribute.CollectionType collectionType = pluralAttribute.getCollectionType();
				if ( collectionType == PluralAttribute.CollectionType.MAP ) {
					sb.append( ( (MapAttribute<?, ?, ?>) pluralAttribute ).getKeyJavaType().getTypeName() )
							.append( ", " );
				}
				sb.append( pluralAttribute.getElementType().getJavaType().getTypeName() ).append( ">" );
			}
			sb.append( "\"\n" );
		}
		return sb.toString();
	}

	/**
	 * Tries to get a meaningful String representation of the result of an HQL query.
	 * We use {@link EntityPrinter} for entities, this allows us to handle associations
	 * cleanly, but it doesn't print the whole object tree - that would pose a problem
	 * of circularity, so we'll have to explore options to handle that.
	 */
	public static String serializeToString(Object result, Class<?> resultType, SessionFactoryImplementor sf) {
		if ( result == null ) {
			return "<null>";
		}

		if ( resultType != null && resultType != Object.class ) {
			if ( resultType.isArray() ) {
				Object[] array = (Object[]) result;
				List<String> results = new ArrayList<>( array.length );
				for ( Object r : array ) {
					results.add( serializeToString( r, r == null ? null : r.getClass(), sf ) );
				}
				return "[" + String.join( ",", results ) + "]";
			}
			else {
				final EntityPersister entityDescriptor = sf.getMappingMetamodel().findEntityDescriptor(
						resultType
				);
				if ( entityDescriptor != null ) {
					// todo : this is not enough, we need to serialize trees including their associations accounting for:
					//  - lazy associations (to-one, to-many)
					//  - circular dependencies (avoid infinite recursion)
					return new EntityPrinter( sf ).toString( entityDescriptor.getEntityName(), result );
				}
				else {
					// try to resolve based on Hibernate's knowledge of the type
					final JavaType<Object> descriptor = sf.getTypeConfiguration()
							.getJavaTypeRegistry()
							.getDescriptor( resultType );
					if ( descriptor != null ) {
						// todo : we need special handling for embeddables, to render each value
						// todo: we could also have special handling for mapped-superclass typed results
						return descriptor.toString( result );
					}
				}
			}
		}

		// As a last stand, just rely on the object's toString() method
		return result.toString();
	}
}
