package org.example;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Session;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SelectionQuery;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaChatModel;
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

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

/**
 * Hibernate interface that allows interacting with an LLM through LangChain4J.
 * <p>
 * It is highly recommended to use a {@link ChatLanguageModel} that supports
 * <a href="https://docs.langchain4j.dev/tutorials/structured-outputs#json-schema">JSON Schema</a>
 * to improve the chances of extracting a valid query from the LLM's messages. Note that
 * this requires to enable <a href="https://docs.langchain4j.dev/tutorials/ai-services/#json-mode">JSON mode</a>
 * on the provided chat model.
 */
public class HibernateAssistant {
	private static final Logger log = Logger.getLogger( HibernateAssistant.class );

	private static final PromptTemplate METAMODEL_PROMPT_TEMPLATE = PromptTemplate.from(
			"You are an expert in writing Hibernate Query Language (HQL) queries.\n"
					+ "You have access to a entity model with the following structure:\n\n" +
					"{{it}}" +
					"If a user asks a question that can be answered by querying this model, generate an HQL SELECT query.\n" +
					"The query must not include any input parameters. " + // todo : one might want to use parameterized queries, but then how to set them ?
//					"The query must not order its results unless explicitly requested." +
//					"The query must never 'join fetch' attributes. " +
//					"The query must not limit the number of results. " +
//					"The query must not use patterns to match strings." +
					"\nDo not output anything else aside from a valid HQL statement!" );

	public static final String OLLAMA_BASE_URL = "http://localhost:11434";
	public static final String GRANITE_31_8b = "granite3.1-dense:8b";
	public static final String GRANITE_CODE_20b = "granite-code:20b-instruct";
	public static final String GRANITE_CODE_8b = "granite-code:8b-instruct-128k-q4_1"; // used by recommended RH code assistant
	public static final String GRANITE_CODE_3b = "granite-code:3b";
	public static final String LLAMA_32 = "llama3.2:latest";
	public static final String CODELLAMA = "codellama:latest";
	public static final String CODELLAMA_13B_INSTRUCT = "codellama:13b-instruct";

	// todo : construct a natural-language response with the data obtained from HQL
	// todo : "AI-query" object holding the HQL, with the possibility of accessing it instead of executing
	// todo : look into LC4J RetrievalAugmentor APIs and the possibility of providing an extension there

	// todo : another alternative can be generating ddl (with Hibernate's SchemaManager),
	//  and creating plain SQL queries (might be better in some contexts, but less safe)

	// todo (less important) : chat memory can be made persistent (instead of storing in-memory) through an entity mapping

	//	private final AiQueryService service;
	private final ChatLanguageModel chatModel;
	private final SystemMessage metamodelPrompt;
	private final ChatMemory chatMemory;
	private final JpaMetamodel metamodel;

	private HibernateAssistant(ChatLanguageModel chatModel, ChatMemory chatMemory, Metamodel metamodel) {
		this.chatModel = chatModel;
		this.chatMemory = chatMemory;
		this.metamodel = (JpaMetamodel) metamodel;

		this.metamodelPrompt = getMetamodelPrompt( metamodel );
		log.infof( "Metamodel prompt: %s", metamodelPrompt.text() );
		chatMemory.add( metamodelPrompt );

//		AiServices.builder( AiQueryService.class )
//				.chatLanguageModel( chatModel )
//				.chatMemory( chatMemory )
//				.build();

//		this.service = AiServices.create( AiQueryService.class, chatModel );
//		service.chat( metamodelPrompt );
	}

	private static SystemMessage getMetamodelPrompt(Metamodel metamodel) {
		return METAMODEL_PROMPT_TEMPLATE.apply( getDomainModelPrompt( metamodel ) ).toSystemMessage();
	}

	public void clearMemory() {
		this.chatMemory.clear();
		this.chatMemory.add( metamodelPrompt );
	}

	public AiQuery<Object> createAiQuery(String message, Session session) {
		return createAiQuery( message, session, Object.class );
	}

	public <T> AiQuery<T> createAiQuery(String message, Session session, Class<T> resultClass) {
		final ManagedDomainType<T> managedType = resultClass != Object.class && !resultClass.isInterface() ?
				metamodel.findManagedType( resultClass ) :
				null;
		if ( managedType != null ) {
			message += "\nThe query must return objects of type \"" + managedType.getTypeName() + "\".";
		}

		final UserMessage userMessage = UserMessage.from( message );
		chatMemory.add( userMessage );

		log.infof( "User message: %s", message );

//		final AiQuery aiQuery = service.chat( message );

		final ChatRequest chatRequest = ChatRequest.builder()
				.responseFormat( hqlResponseFormat() )
				.messages( chatMemory.messages() )
				.build();

		final ChatResponse chatResponse = chatModel.chat( chatRequest );
		final String response = chatResponse.aiMessage().text();

		log.infof( "Raw model response: %s", response );

		final HqlHolder hqlHolder;
		try {
			hqlHolder = new ObjectMapper().readValue( response, HqlHolder.class );
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}

//		final String HQL = extractHQL( response );

		final String hql = hqlHolder.hqlQuery();

		log.infof( "Extracted HQL: %s", hql );

		return AiQuery.from( hql, resultClass, session );
	}

	record HqlHolder(String hqlQuery) {
	}

	interface AiQueryService {
		HqlHolder chat(String message);
	}

	private static ResponseFormat hqlResponseFormat() {
		return ResponseFormat.builder().type( JSON ) // type can be either TEXT (default) or JSON
				.jsonSchema( JsonSchema.builder().name( "HQL" ) // OpenAI requires specifying the name for the schema
									 .rootElement( JsonObjectSchema.builder() // see [1] below
														   .addStringProperty( "hqlQuery" )
														   .required( "hqlQuery" ) // see [2] below
														   .build() ).build() ).build();
	}

	public static String extractHQL(String response) {
		// sadly, we often get more than pure HQL in the chatbot's response
		// here, we try our best to clean that up
		final String regex = "(?i)\\bSELECT\\b.*?(?:;|\\n|$)";
		final Pattern pattern = Pattern.compile( regex );
		final Matcher matcher = pattern.matcher( response );
		if ( matcher.find() ) {
			return matcher.group().trim();
		}
		return null;
	}

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

	private static <T> String getEntityTypeDescription(EntityType<T> entityType) {
		return "\"" + entityType.getName() + "\" is an entity type.\n" + getJavaTypeDescription( entityType ) + getInheritanceDescription(
				(ManagedDomainType<?>) entityType ) + getIdentifierDescription( entityType ) + getAttributesDescription(
				entityType.getAttributes() );
	}

	private static String getJavaTypeDescription(ManagedType<?> managedType) {
		return "It corresponds to the java class \"" + managedType.getJavaType().getTypeName() + "\"\n";
	}

	private static String getInheritanceDescription(ManagedDomainType<?> managedType) {
		final ManagedDomainType<?> superType = managedType.getSuperType();
		return superType != null ? "It extends from the \"" + superType.getJavaType().getTypeName() + "\" type.\n" : "";
	}

	private static <T> String getMappedSuperclassTypeDescription(MappedSuperclassType<T> mappedSuperclass) {
		return "\"" + mappedSuperclass.getJavaType()
				.getSimpleName() + "\" is a mapped superclass type.\n" + getJavaTypeDescription( mappedSuperclass ) + getInheritanceDescription(
				(ManagedDomainType<?>) mappedSuperclass ) + getIdentifierDescription( mappedSuperclass ) + getAttributesDescription(
				mappedSuperclass.getAttributes() );
	}

	private static <T> String getIdentifierDescription(IdentifiableType<T> identifiableType) {
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

	private static <T> String getEmbeddableTypeDescription(EmbeddableType<T> embeddableType) {
		return "\"" + embeddableType.getJavaType()
				.getSimpleName() + "\" is an embeddable type.\n" + getInheritanceDescription( (ManagedDomainType<?>) embeddableType ) + getJavaTypeDescription(
				embeddableType ) + getAttributesDescription( embeddableType.getAttributes() );
	}

	private static <T> String getAttributesDescription(Set<Attribute<? super T, ?>> attributes) {
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


	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Metamodel metamodel;
		private ChatLanguageModel chatModel;
		private ChatMemory chatMemory;

		private Builder() {
		}

		public Builder metamodel(Metamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public Builder chatModel(ChatLanguageModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public Builder chatMemory(ChatMemory chatMemory) {
			this.chatMemory = chatMemory;
			return this;
		}

		public HibernateAssistant build() {
			return new HibernateAssistant(
					getOrDefault( chatModel, Builder::defaultChatLanguageModel ),
					getOrDefault( chatMemory, Builder::defaultChatMemory ),
					ensureNotNull( metamodel, "Metamodel" )
			);
		}

		private static ChatLanguageModel defaultChatLanguageModel() {
			return OllamaChatModel.builder()
					.baseUrl( OLLAMA_BASE_URL )
					.modelName( CODELLAMA )
					.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA )
					.temperature( 0.0 )
//					.logRequests( true )
//					.logResponses( true )
					.build();
		}

		private static ChatMemory defaultChatMemory() {
			// Just enough for 1 system message and 1 user message
			return MessageWindowChatMemory.withMaxMessages( 2 );
		}
	}
}
