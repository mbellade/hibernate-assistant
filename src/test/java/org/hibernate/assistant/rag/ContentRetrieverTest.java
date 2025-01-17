package org.hibernate.assistant.rag;

import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.service.AiServices;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static org.hibernate.assistant.HibernateAssistant.CODELLAMA;
import static org.hibernate.assistant.HibernateAssistant.OLLAMA_BASE_URL;
import static org.hibernate.assistant.rag.HibernateContentRetriever.INJECTOR_PROMPT_TEMPLATE;

@SessionFactory
@DomainModel(annotatedClasses = { Company.class, Address.class })
public class ContentRetrieverTest {
	private final ChatLanguageModel chatModel = OllamaChatModel.builder()
			.baseUrl( OLLAMA_BASE_URL )
			.modelName( CODELLAMA )
			.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA )
			.temperature( 0.0 )
			.build();

	private final ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages( 10 );

	@Test
	public void simpleRagTest(SessionFactoryScope scope) {
		final HibernateContentRetriever contentRetriever = HibernateContentRetriever.builder()
				.chatModel( chatModel )
				.chatMemory( chatMemory )
				.sessionFactory( scope.getSessionFactory() )
				.build();
		final RetrievalAugmentor rag = DefaultRetrievalAugmentor.builder()
				.contentRetriever( contentRetriever )
				.contentInjector( DefaultContentInjector.builder().promptTemplate( INJECTOR_PROMPT_TEMPLATE ).build() )
				.build();

		final Assistant assistant = AiServices.builder( Assistant.class )
				.chatLanguageModel( chatModel )
				.chatMemory( chatMemory )
				.retrievalAugmentor( rag )
				.build();

		final String response = assistant.chat( "How many addresses start with the word 'Via'?" );

		// There are 2 addresses that start with the word "Via".
		System.out.println( "Response: " + response );
	}

	interface Assistant {
		String chat(String message);
	}

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) ) );
			entityManager.persist( new Company( 2L, "IBM", new Address( "Segrate", "Circonvallazione Idroscalo" ) ) );
			entityManager.persist( new Company( 3L, "Belladelli Giovanni", new Address( "Pegognaga", "Via Roma" ) ) );
			entityManager.persist( new Company( 4L, "Another Company", null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
