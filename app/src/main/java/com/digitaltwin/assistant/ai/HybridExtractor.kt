package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.data.prefs.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Two-tier extractor: [RuleBasedExtractor] runs first (free, offline, instant). If it produces no
 * results AND the user has enabled cloud AI AND a Gemini key is configured, [GeminiExtractor]
 * handles the text. This keeps API cost near zero for the common, unambiguous cases.
 */
@Singleton
class HybridExtractor @Inject constructor(
    private val rulesBased: RuleBasedExtractor,
    private val gemini: GeminiExtractor,
    private val settings: SettingsStore,
) : TaskExtractor {

    override suspend fun extract(text: String, context: ExtractionContext): List<ExtractionCandidate> {
        val rulesResult = rulesBased.extract(text, context)
        if (rulesResult.isNotEmpty()) return rulesResult

        val apiKey = settings.geminiApiKey
        if (!settings.cloudAiEnabled || apiKey == null) return emptyList()

        return gemini.extractWithKey(apiKey, text, context)
    }
}
