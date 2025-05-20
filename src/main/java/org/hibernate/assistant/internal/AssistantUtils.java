package org.hibernate.assistant.internal;

import org.hibernate.metamodel.model.domain.ManagedDomainType;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AssistantUtils {

	/**
	 * Utility method that generates a JSON string representation of the mapping information
	 * contained in the provided {@link Metamodel metamodel} instance. The representation
	 * does not follow a strict scheme, and is more akin to natural language, as it's
	 * mainly meant for consumption by a LLM.
	 *
	 * @param metamodel the metamodel instance containing information on the persistence structures
	 *
	 * @return the JSON representation of the provided {@link Metamodel metamodel}
	 */
	public static String getDomainModelPrompt(Metamodel metamodel) {
		final List<String> entities = new ArrayList<>();
		final List<String> embeddables = new ArrayList<>();
		final List<String> mappedSupers = new ArrayList<>();
		for ( ManagedType<?> managedType : metamodel.getManagedTypes() ) {
			switch ( managedType.getPersistenceType() ) {
				case ENTITY -> entities.add( getEntityTypeDescription( (EntityType<?>) managedType ) );
				case EMBEDDABLE -> embeddables.add( getEmbeddableTypeDescription( (EmbeddableType<?>) managedType ) );
				case MAPPED_SUPERCLASS -> mappedSupers.add( getMappedSuperclassTypeDescription( (MappedSuperclassType<?>) managedType ) );
				default ->
						throw new IllegalStateException( "Unexpected persistence type for managed type [" + managedType + "]" );
			}
		}
		return "{" +
				"\"entities\":" + toJsonArray( entities ) +
				",\"mappedSuperclasses\":" + toJsonArray( mappedSupers ) +
				",\"embeddables\":" + toJsonArray( embeddables ) +
				'}';
	}

	static String toJsonArray(Collection<String> strings) {
		return strings.isEmpty() ? "[]" : "[" + String.join( ",", strings ) + "]";
	}

	public static <T> String getEntityTypeDescription(EntityType<T> entityType) {
		return "{\"name\":\"" + entityType.getName() + "\"," +
				"\"class\":\"" + entityType.getJavaType().getTypeName() + "\"," +
				superclassDescriptor( (ManagedDomainType<?>) entityType ) +
				identifierDescriptor( entityType ) +
				"\"attributes\":" + attributeArray( entityType.getAttributes() ) +
				"}";
	}

	public static String superclassDescriptor(ManagedDomainType<?> managedType) {
		final ManagedDomainType<?> superType = managedType.getSuperType();
		return superType != null ? "\"superclass\":\"" + superType.getJavaType().getTypeName() + "\"," : "";
	}

	public static <T> String getMappedSuperclassTypeDescription(MappedSuperclassType<T> mappedSuperclass) {
		final Class<T> javaType = mappedSuperclass.getJavaType();
		return "{\"name\":\"" + javaType.getSimpleName() + "\"," +
				"\"class\":\"" + javaType.getTypeName() + "\"," +
				superclassDescriptor( (ManagedDomainType<?>) mappedSuperclass ) +
				identifierDescriptor( mappedSuperclass ) +
				"\"attributes\":" + attributeArray( mappedSuperclass.getAttributes() ) +
				"}";
	}

	public static <T> String identifierDescriptor(IdentifiableType<T> identifiableType) {
		final Type<?> idType = identifiableType.getIdType();
		final String description;
		if ( idType != null ) {
			final SingularAttribute<? super T, ?> id = identifiableType.getId( idType.getJavaType() );
			description = "\"identifier\":{\"name\":\"" + id.getName() + "\"," +
					"\"type\":\"" + id.getJavaType().getTypeName() + "\"},";
		}
		else {
			description = "";
		}
		return description;
	}

	public static <T> String getEmbeddableTypeDescription(EmbeddableType<T> embeddableType) {
		final Class<T> javaType = embeddableType.getJavaType();
		return "{\"name\":\"" + javaType.getSimpleName() + "\"," +
				"\"class\":\"" + javaType.getTypeName() + "\"," +
				superclassDescriptor( (ManagedDomainType<?>) embeddableType ) +
				"\"attributes\":" + attributeArray( embeddableType.getAttributes() ) +
				"}";
	}

	public static <T> String attributeArray(Set<Attribute<? super T, ?>> attributes) {
		if ( attributes.isEmpty() ) {
			return "[]";
		}

		final StringBuilder sb = new StringBuilder( "[" );
		for ( final Attribute<? super T, ?> attribute : attributes ) {
			sb.append( "{\"name\":\"" ).append( attribute.getName() )
					.append( "\",\"type\":\"" ).append( attribute.getJavaType().getTypeName() );
			// add key and element types for plural attributes
			if ( attribute instanceof PluralAttribute<?, ?, ?> pluralAttribute ) {
				sb.append( '<' );
				final PluralAttribute.CollectionType collectionType = pluralAttribute.getCollectionType();
				if ( collectionType == PluralAttribute.CollectionType.MAP ) {
					sb.append( ( (MapAttribute<?, ?, ?>) pluralAttribute ).getKeyJavaType().getTypeName() )
							.append( "," );
				}
				sb.append( pluralAttribute.getElementType().getJavaType().getTypeName() ).append( '>' );
			}
			sb.append( "\"}," );
		}
		return sb.deleteCharAt( sb.length() - 1 ).append( ']' ).toString();
	}
}
