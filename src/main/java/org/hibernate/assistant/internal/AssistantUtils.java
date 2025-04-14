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
import java.util.Set;

public class AssistantUtils {

	/**
	 * Utility method that generates a textual representation of the mapping information
	 * contained in the provided {@link Metamodel metamodel} instance. The representation
	 * does not follow a strict scheme, and is more akin to natural language, as it's
	 * mainly meant for consumption by a LLM.
	 *
	 * @param metamodel the metamodel instance containing information on the persistence structures
	 * @param quote character to use for quotations, used to delimit object names from general text;
	 * use {@code null} to disable quoting
	 *
	 * @return the textual representation of the provided {@link Metamodel metamodel}
	 */
	public static String getDomainModelPrompt(Metamodel metamodel, Character quote) {
		final StringBuilder sb = new StringBuilder();
		for ( ManagedType<?> managedType : metamodel.getManagedTypes() ) {
			final String typeDescription = switch ( managedType.getPersistenceType() ) {
				case ENTITY -> getEntityTypeDescription( (EntityType<?>) managedType, quote );
				case EMBEDDABLE -> getEmbeddableTypeDescription( (EmbeddableType<?>) managedType, quote );
				case MAPPED_SUPERCLASS -> getMappedSuperclassTypeDescription(
						(MappedSuperclassType<?>) managedType,
						quote
				);
				default ->
						throw new IllegalStateException( "Unexpected persistence type for managed type [" + managedType + "]" );
			};
			sb.append( typeDescription ).append( "\n" );
		}
		return sb.toString();
	}

	public static <T> String getEntityTypeDescription(EntityType<T> entityType, Character quote) {
		return quote( entityType.getName(), quote ) + " is an entity type.\n"
				+ getJavaTypeDescription( entityType, quote )
				+ getInheritanceDescription( (ManagedDomainType<?>) entityType, quote )
				+ getIdentifierDescription( entityType, quote )
				+ getAttributesDescription( entityType.getAttributes(), quote );
	}

	private static String quote(String s, Character quote) {
		return quote != null ? quote + s + quote : s;
	}

	public static String getJavaTypeDescription(ManagedType<?> managedType, Character quote) {
		return "It maps to the java class " + quote( managedType.getJavaType().getTypeName(), quote ) + "\n";
	}

	public static String getInheritanceDescription(ManagedDomainType<?> managedType, Character quote) {
		final ManagedDomainType<?> superType = managedType.getSuperType();
		return superType != null
				? "It extends from the " + quote( superType.getJavaType().getTypeName(), quote ) + " type.\n"
				: "";
	}

	public static <T> String getMappedSuperclassTypeDescription(
			MappedSuperclassType<T> mappedSuperclass,
			Character quote) {
		return quote( mappedSuperclass.getJavaType().getSimpleName(), quote )
				+ " is a mapped superclass type.\n"
				+ getJavaTypeDescription( mappedSuperclass, quote )
				+ getInheritanceDescription( (ManagedDomainType<?>) mappedSuperclass, quote )
				+ getIdentifierDescription( mappedSuperclass, quote )
				+ getAttributesDescription( mappedSuperclass.getAttributes(), quote );
	}

	public static <T> String getIdentifierDescription(IdentifiableType<T> identifiableType, Character quote) {
		final Type<?> idType = identifiableType.getIdType();
		final String description;
		if ( idType != null ) {
			final SingularAttribute<? super T, ?> id = identifiableType.getId( idType.getJavaType() );
			description = "Its identifier attribute is called " + quote( id.getName(), quote )
					+ " and is of type " + quote( id.getJavaType().getTypeName(), quote ) + ".\n";
		}
		else {
			description = "It has no identifier attribute.\n";
		}
		return description;
	}

	public static <T> String getEmbeddableTypeDescription(EmbeddableType<T> embeddableType, Character quote) {
		return quote( embeddableType.getJavaType().getSimpleName(), quote ) + " is an embeddable type.\n"
				+ getInheritanceDescription( (ManagedDomainType<?>) embeddableType, quote )
				+ getJavaTypeDescription( embeddableType, quote )
				+ getAttributesDescription( embeddableType.getAttributes(), quote );
	}

	public static <T> String getAttributesDescription(Set<Attribute<? super T, ?>> attributes, Character quote) {
		final StringBuilder sb = new StringBuilder( "Its attributes are (name => type):\n" );
		for ( final Attribute<? super T, ?> attribute : attributes ) {
			sb.append( "- " ).append( quote( attribute.getName(), quote ) ).append( " => " );
			if ( quote != null ) {
				sb.append( quote );
			}
			sb.append( attribute.getJavaType().getTypeName() );
			// add key and element types for plural attributes
			if ( attribute instanceof PluralAttribute<?, ?, ?> pluralAttribute ) {
				sb.append( '<' );
				final PluralAttribute.CollectionType collectionType = pluralAttribute.getCollectionType();
				if ( collectionType == PluralAttribute.CollectionType.MAP ) {
					sb.append( ( (MapAttribute<?, ?, ?>) pluralAttribute ).getKeyJavaType().getTypeName() )
							.append( ", " );
				}
				sb.append( pluralAttribute.getElementType().getJavaType().getTypeName() ).append( '>' );
			}
			if ( quote != null ) {
				sb.append( quote );
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}
}
