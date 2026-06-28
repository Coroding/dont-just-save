package com.coroding.dontjustsave.ai

data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String? = null,
    val modelName: String? = null,
    val enabled: Boolean = false,
)

object AiConfigProvider {
    // TODO AI: replace with persisted settings when SettingsScreen is expanded.
    val defaultConfig = AiConfig(
        baseUrl = "",
        apiKey = null,
        modelName = null,
        enabled = false,
    )
}
