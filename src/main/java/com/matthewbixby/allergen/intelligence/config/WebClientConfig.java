package com.matthewbixby.allergen.intelligence.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader("User-Agent", "AllergenIntelligence/1.0")
                .build();
    }

    @Bean
    public ChatClient chatClient() {
        String apiKey = System.getenv("OPENAI_API_KEY");

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model("gpt-4o-search-preview")
                .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(chatOptions)
                .build();

        return ChatClient.builder(openAiChatModel).build();
    }
}