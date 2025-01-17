package org.hibernate.assistant;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class HqlContentRetriever implements ContentRetriever {
	private static final Logger log = Logger.getLogger( HibernateAssistant.class );

	private final HibernateAssistant assistant;
	private final SessionFactoryImplementor sessionFactory;

	public HqlContentRetriever(HibernateAssistant assistant, SessionFactory sessionFactory) {
		// todo alternate constructor with LC4J-typed objects, and create the assistant ourselves
		this.assistant = assistant;
		this.sessionFactory = (SessionFactoryImplementor) sessionFactory;
	}

	@Override
	public List<Content> retrieve(Query naturalLanguageQuery) {
		// todo we could implement retries, and pass the error message to the chat model
//		String errorMessage = null;
//		int attemptsLeft = maxRetries + 1;
//		while (attemptsLeft > 0) {
//			attemptsLeft--;

		final String result = sessionFactory.fromSession( session -> {
			final AiQuery<Object> aiQuery = assistant.createAiQuery( naturalLanguageQuery.text(), session );

			try {
				return assistant.executeQueryToString( aiQuery, session );
			}
			catch (Exception e) {
				log.errorf( e, "Error executing query, hql: %s", aiQuery.getHql() );
				return null;
			}
		} );

		return result == null ? emptyList() : singletonList( Content.from( result ) );
	}
}
