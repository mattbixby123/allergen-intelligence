package com.matthewbixby.allergen.intelligence.service;

import com.matthewbixby.allergen.intelligence.model.User;
import com.matthewbixby.allergen.intelligence.repository.UserRepository;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageTrackingService {

    private final UserRepository userRepository;
    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private final com.knuddels.jtokkit.api.Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    /**
     * Estimate tokens for a given text using tiktoken (same as OpenAI uses)
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.encode(text).size();
    }

    /**
     * Track OpenAI API usage for a user
     */
    @Transactional
    public void trackUsage(String userEmail, String prompt, String response) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            int promptTokens = estimateTokens(prompt);
            int responseTokens = estimateTokens(response);
            int totalTokens = promptTokens + responseTokens;

            // Update user's total token usage
            user.setTotalTokensUsed(user.getTotalTokensUsed() + totalTokens);
            user.setAnalysesRun(user.getAnalysesRun() + 1);

            userRepository.save(user);

            log.info("Tracked usage for {}: {} tokens ({} prompt + {} response), Total analyses: {}",
                    userEmail, totalTokens, promptTokens, responseTokens, user.getAnalysesRun());

        } catch (Exception e) {
            log.error("Error tracking usage for {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Track analysis without specific prompt/response (just increment count)
     */
    @Transactional
    public void trackAnalysis(String userEmail, int estimatedTokens) {
        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            user.setTotalTokensUsed(user.getTotalTokensUsed() + estimatedTokens);
            user.setAnalysesRun(user.getAnalysesRun() + 1);

            userRepository.save(user);

            log.info("Tracked analysis for {}: {} tokens, Total: {}",
                    userEmail, estimatedTokens, user.getAnalysesRun());

        } catch (Exception e) {
            log.error("Error tracking analysis for {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Get current user's usage stats
     */
    public User getUserUsage(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));
    }

    /**
     * Calculate estimated cost based on token usage
     * GPT-4o rates: $2.50 per 1M input tokens, $10 per 1M output tokens
     * We'll use average of $5 per 1M tokens for simplicity
     */
    public double calculateEstimatedCost(int totalTokens) {
        return (totalTokens / 1_000_000.0) * 5.0;
    }
}