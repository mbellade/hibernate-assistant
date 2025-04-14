package org.hibernate.assistant.internal.lc4j;

import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.assistant.HibernateAssistant;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SelectionQuery;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.chain.ConversationalRetrievalChain;
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
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static org.hibernate.assistant.internal.AssistantUtils.getDomainModelPrompt;
import static org.hibernate.assistant.spi.QuerySerializer.serializeToString;
import static org.hibernate.assistant.internal.lc4j.HibernateContentRetriever.INJECTOR_PROMPT_TEMPLATE;

/**
 * Implementation of {@link HibernateAssistant} based on <a href="https://docs.langchain4j.dev/">LangChain4j</a> APIs.
 * The user must provide a {@link ChatLanguageModel} instance that will be used to interact with the LLMs.
 * Optionally, a {@link ChatMemory} can also be provided, otherwise a default {@link MessageWindowChatMemory}
 * with a maximum of {@code 10} messages will be used.
 * <p>
 * It is highly recommended to use a {@link ChatLanguageModel} that supports
 * <a href="https://docs.langchain4j.dev/tutorials/structured-outputs#json-schema">JSON Schema</a>
 * to improve the chances of extracting a valid HQL query from the LLM's responses. Note that this requires
 * enabling <a href="https://docs.langchain4j.dev/tutorials/structured-outputs#json-schema">JSON Schema</a>
 * support on the provided chat model.
 */
public class HibernateAssistantLC4J implements HibernateAssistant {
	private static final Logger log = Logger.getLogger( HibernateAssistantLC4J.class );

	private static final PromptTemplate METAMODEL_PROMPT_TEMPLATE = PromptTemplate.from(
			"""
					You are an expert in writing Hibernate Query Language (HQL) queries.
					You have access to a entity model with the following structure:
					
					{{it}}
					
					If a user asks a question that can be answered by querying this model, generate an HQL SELECT query.
					The query must not include any input parameters.
					Do not output anything else aside from a valid HQL statement!
					""" );

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private ChatLanguageModel chatModel;
		private ChatMemory chatMemory;
		private Metamodel metamodel;
		private PromptTemplate metamodelPromptTemplate;
		private boolean structuredJson = true;

		private Builder() {
		}

		public Builder chatModel(ChatLanguageModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public Builder chatMemory(ChatMemory chatMemory) {
			this.chatMemory = chatMemory;
			return this;
		}

		public Builder metamodel(Metamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public Builder structuredJson(boolean structuredJson) {
			this.structuredJson = structuredJson;
			return this;
		}

		/**
		 * The initial {@link SystemMessage} to instruct the language model about creating HQL queries,
		 * and the structure of the domain metamodel (mapped classes and their structure).
		 * <p>
		 * Must include {@code {{it}}} where the generated mapped objects information will be substituted.
		 * Defaults to {@link #METAMODEL_PROMPT_TEMPLATE}.
		 *
		 * @param metamodelPromptTemplate the custom prompt template to be used
		 *
		 * @return {{@code this}} for chaining calls
		 */
		public Builder metamodelPromptTemplate(PromptTemplate metamodelPromptTemplate) {
			this.metamodelPromptTemplate = metamodelPromptTemplate;
			return this;
		}

		public HibernateAssistantLC4J build() {
			return new HibernateAssistantLC4J( this );
		}

		private static ChatMemory defaultChatMemory() {
			// this can be tweaked, but really should be user-provided
			return MessageWindowChatMemory.withMaxMessages( 10 );
		}
	}

	// note : another alternative use-case can be generating ddl (with Hibernate's SchemaManager),
	//  and creating plain SQL queries (might be better in some contexts, but less safe)

	// note : chat memory can be made persistent (instead of storing in-memory) through an entity mapping!
	//  this could be another (configurable) feature the assistant brings

	//	private final AiQueryService service;
	private final ChatLanguageModel chatModel;
	private final SystemMessage metamodelPrompt;
	private final ChatMemory chatMemory;
	private final JpaMetamodel metamodel;
	private final boolean structuredJson;

	private HibernateAssistantLC4J(
			ChatLanguageModel chatModel,
			ChatMemory chatMemory,
			Metamodel metamodel,
			PromptTemplate metamodelPromptTemplate,
			boolean structuredJson) {
		this.chatModel = chatModel;
		this.chatMemory = chatMemory;
		this.metamodel = (JpaMetamodel) metamodel;
		this.structuredJson = structuredJson;

		this.metamodelPrompt = getMetamodelPrompt( metamodelPromptTemplate, metamodel );
		log.debugf( "Metamodel prompt: %s", metamodelPrompt.text() );
		chatMemory.add( metamodelPrompt );
	}

	private HibernateAssistantLC4J(Builder builder) {
		this(
				ensureNotNull( builder.chatModel, "ChatLanguageModel" ),
				getOrDefault( builder.chatMemory, Builder::defaultChatMemory ),
				ensureNotNull( builder.metamodel, "Metamodel" ),
				getOrDefault( builder.metamodelPromptTemplate, METAMODEL_PROMPT_TEMPLATE ),
				builder.structuredJson
		);
	}

	private static SystemMessage getMetamodelPrompt(PromptTemplate metamodelPromptTemplate, Metamodel metamodel) {
		return metamodelPromptTemplate.apply( getDomainModelPrompt( metamodel, '"' ) ).toSystemMessage();
	}

	@Override
	public void clear() {
		this.chatMemory.clear();
		this.chatMemory.add( metamodelPrompt );
	}

	@Override
	public <T> SelectionQuery<T> createAiQuery(String message, SharedSessionContract session, Class<T> resultType) {
		final ManagedDomainType<T> managedType = resultType != null && resultType != Object.class && !resultType.isInterface() ?
				metamodel.findManagedType( resultType ) :
				null;
		if ( managedType != null ) {
			message += "\nThe query must return objects of type \"" + managedType.getTypeName() + "\".";
		}

		final UserMessage userMessage = UserMessage.from( message );
		chatMemory.add( userMessage );

		final ChatRequest.Builder requestBuilder = ChatRequest.builder().messages( chatMemory.messages() );
		if ( structuredJson ) {
			requestBuilder.responseFormat( hqlResponseFormat() );
		}

		final ChatRequest chatRequest = requestBuilder.build();

		final ChatResponse chatResponse = chatModel.chat( chatRequest );

		final String hql = extractHql( chatResponse, structuredJson );

		log.debugf( "Extracted HQL: %s", hql );

		return session.createSelectionQuery( hql, resultType );
	}

	private static String extractHql(ChatResponse chatResponse, boolean structuredJson) {
		final String response = chatResponse.aiMessage().text();

		log.debugf( "Raw model response: %s", response );

		if ( structuredJson ) {
			final HqlHolder hqlHolder;
			try {
				hqlHolder = new ObjectMapper().readValue( response, HqlHolder.class );
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException( e );
			}

			return hqlHolder.hqlQuery();
		}
		else {
			return extractHql( response );
		}
	}

	private static String extractHql(String response) {
		// Try our best to extract valid HQL from text
		final String regex = "(?i)\\bSELECT\\b.*?(?:;|\\n|$)";
		final Pattern pattern = Pattern.compile( regex );
		final Matcher matcher = pattern.matcher( response );
		if ( matcher.find() ) {
			return matcher.group().trim();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this requires the assistant's {@link ChatMemory} to be able to store at least 3 messages:
	 * the base mapping model system message, the initial request to create the query and the String
	 * representation of the query results.
	 * <p>
	 * You can also use this RAG-like (retrieval-augmented generation) functionality through the
	 * {@link HibernateContentRetriever} that directly plugs into LangChain4J's {@link RetrievalAugmentor} APIs.
	 *
	 * @param message the natural language request
	 * @param sessionFactory Hibernate's session factory
	 *
	 * @return a natural language response based on the results of the query
	 */
	public String executeQuery(String message, SessionFactory sessionFactory) {
		final HibernateContentRetriever contentRetriever = new HibernateContentRetriever(
				this,
				sessionFactory
		);

		final RetrievalAugmentor rag = DefaultRetrievalAugmentor.builder()
				.contentRetriever( contentRetriever )
				.contentInjector( DefaultContentInjector.builder().promptTemplate( INJECTOR_PROMPT_TEMPLATE ).build() )
				.build();
		final ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
				.chatLanguageModel( chatModel )
				.chatMemory( chatMemory )
				.retrievalAugmentor( rag )
				.build();

		return chain.execute( message );
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this requires the assistant's {@link ChatMemory} to be able to store at least 3 messages:
	 * the base mapping model system message, the initial request to create the query and the textual
	 * representation of the query results.
	 */
	@Override
	public String executeQuery(SelectionQuery<?> query, SharedSessionContract session) {
		final String result = executeQueryToString( query, session );

		final String prompt = "The query returned the following data:\n" + result +
				// this seems to be needed, otherwise with some models we just get an HQL query
				"\nAnswer the original question using natural language and do not create a query!";

		log.debugf( "Query result prompt: %s", prompt );

		final UserMessage userMessage = UserMessage.from( prompt );
		chatMemory.add( userMessage );

		final ChatRequest chatRequest = ChatRequest.builder()
				.messages( chatMemory.messages() )
				.build();

		final ChatResponse chatResponse = chatModel.chat( chatRequest );
		return chatResponse.aiMessage().text();
	}

	/**
	 * Executes the given {@link SelectionQuery} as a {@link org.hibernate.query.SelectionQuery}, and provides
	 * a string representation of the response. The string will be created based on Hibernate's
	 * knowledge of the domain model, but it will not print the entire object tree since that
	 * would cause circularity problems. This is a best-effort attempt at providing a useful
	 * string-representation based on data, mainly used to pass it back to a {@link ChatLanguageModel}
	 * like in {@link #executeQuery(SelectionQuery, SharedSessionContract)}.
	 * <p>
	 * If you wish to execute the query manually and obtain the structured results yourself,
	 * you should use {@link SelectionQuery}'s direct execution methods, e.g. {@link SelectionQuery#getResultList()}
	 * or {@link SelectionQuery#getSingleResult()}.
	 *
	 * @param query the AI query to execute
	 * @param session the session in which to execute the query
	 *
	 * @return a natural language response based on the results of the query
	 */
	public String executeQueryToString(SelectionQuery<?> query, SharedSessionContract session) {
		final List<?> resultList = query.getResultList();
		if ( resultList.isEmpty() ) {
			return "The query did not return any results.";
		}

		return serializeToString( resultList, query, (SessionFactoryImplementor) session.getFactory() );
	}

	record HqlHolder(String hqlQuery) {
	}

	private static ResponseFormat hqlResponseFormat() {
		return ResponseFormat.builder().type( JSON ) // type can be either TEXT (default) or JSON
				.jsonSchema( JsonSchema.builder().name( "HQL" ) // OpenAI requires specifying the name for the schema
									 .rootElement( JsonObjectSchema.builder() // see [1] below
														   .addStringProperty( "hqlQuery" )
														   .required( "hqlQuery" ) // see [2] below
														   .build() ).build() ).build();
	}
}
