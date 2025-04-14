package org.hibernate.assistant.rag;

import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;
import org.hibernate.assistant.domain.Employee;
import org.hibernate.assistant.internal.lc4j.HibernateContentRetriever;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.service.AiServices;

import static org.hibernate.assistant.internal.lc4j.HibernateContentRetriever.INJECTOR_PROMPT_TEMPLATE;
import static org.hibernate.assistant.util.LanguageModels.testChatLanguageModel;

@SessionFactory
@DomainModel(annotatedClasses = { Company.class, Address.class, Employee.class })
public class ContentRetrieverTest {
	private final ChatLanguageModel chatModel = testChatLanguageModel();
	private final ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages( 10 );

	@Test
	public void simpleRagTest(SessionFactoryScope scope) {
		final HibernateContentRetriever contentRetriever = HibernateContentRetriever.builder()
				.chatModel( testChatLanguageModel() )
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

		final String response = assistant.chat( "How many company addresses have a street name that starts with the word 'Via'?" );

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
