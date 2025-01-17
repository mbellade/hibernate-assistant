package org.hibernate.assistant.rag;

import dev.langchain4j.model.input.PromptTemplate;

public class HibernateContentInjector {
	public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from(
			"""
					{{userMessage}}
					
					The query returned the following data:
					{{contents}}
					
					Answer the original question using natural language and do not create a query!"""
	);

	public HibernateContentInjector() {
	}
}
