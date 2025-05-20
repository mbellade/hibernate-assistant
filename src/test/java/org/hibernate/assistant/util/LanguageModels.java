package org.hibernate.assistant.util;

import org.hibernate.assistant.HibernateAssistant;
import org.hibernate.assistant.internal.lc4j.HibernateAssistantLC4J;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.persistence.metamodel.Metamodel;
import java.util.Locale;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

public class LanguageModels {
	public static final String OLLAMA_BASE_URL = "http://localhost:11434";
	public static final String GRANITE_31_8b = "granite3.1-dense:8b";
	public static final String GRANITE_CODE_20b = "granite-code:20b-instruct";
	public static final String GRANITE_CODE_8b = "granite-code:8b-instruct-128k-q4_1"; // used by recommended RH code assistant
	public static final String GRANITE_CODE_3b = "granite-code:3b";
	public static final String LLAMA_32 = "llama3.2";
	public static final String CODELLAMA = "codellama";
	public static final String QWEN_25_CODER = "qwen2.5-coder";
	public static final String GPT_4O_MINI = "gpt-4o-mini";

	public static ChatModel testChatModel() {
		final String modelType = System.getProperty( "hibernate.assistant.model-type" );
		if ( modelType != null ) {
			final String baseUrl = System.getProperty( "hibernate.assistant.model-base-url" );
			final String modelName = System.getProperty( "hibernate.assistant.model-name" );
			final boolean structuredJson = Boolean.parseBoolean( System.getProperty( "hibernate.assistant.structured-json" ) );
			return switch ( modelType.toUpperCase( Locale.ROOT ) ) {
				case "OLLAMA" -> {
					final OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
							.baseUrl( baseUrl )
							.modelName( modelName )
							.temperature( 0.0 );
					if ( structuredJson ) {
						builder.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA );
					}
					yield builder.build();
				}
				case "OPENAI" -> {
					final OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = OpenAiChatModel.builder()
							.apiKey( System.getProperty( "hibernate.assistant.openai.api-key" ) )
							.modelName( modelName )
							.temperature( 0.0 );
					if ( structuredJson ) {
						openAiChatModelBuilder.responseFormat( "json_schema" ).strictJsonSchema( true );
					}
					yield openAiChatModelBuilder.build();
				}
				default -> throw new IllegalStateException( "Unsupported model type: " + modelType );
			};
		}

		// default configuration if no system properties are found
		return OllamaChatModel.builder()
				.baseUrl( OLLAMA_BASE_URL )
				.modelName( QWEN_25_CODER )
				.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA )
				.temperature( 0.0 )
				.build();
	}

	public static HibernateAssistant testAssistant(Metamodel metamodel) {
		return testAssistant( metamodel, null );
	}

	public static HibernateAssistant testAssistant(Metamodel metamodel, ChatMemory memory) {
		return HibernateAssistantLC4J.builder()
				.chatModel( testChatModel() )
				.metamodel( metamodel )
				.chatMemory( memory )
				.build();
	}
}
