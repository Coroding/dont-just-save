package com.coroding.dontjustsave.ai

data class AiRequest(
    val sourceUrl: String?,
    val sourceText: String?,
    val sourcePlatform: String?,
    val sourceDomain: String?,
    val userNote: String,
    val imageSummary: String?,
    val currentCategory: String?,
    val previewTitle: String?,
    val previewDescription: String?,
    val taskMode: String,
)

data class AiResponse(
    val contentType: String,
    val tags: List<String>,
    val reusableStructure: String,
    val referenceValue: String,
    val nextAction: String,
    val shouldCreateTask: Boolean,
    val confidence: Float,
    val reason: String,
    val taskDraft: CreationTaskDraft?,
    val taskTitle: String? = taskDraft?.taskTitle,
    val contentDirection: String? = taskDraft?.contentDirection,
    val outline: List<String>? = taskDraft?.outline,
    val materialList: List<String>? = taskDraft?.materialList,
)

data class CreationTaskDraft(
    val taskTitle: String?,
    val contentDirection: String?,
    val outline: List<String>?,
    val materialList: List<String>?,
    val nextAction: String?,
)

object AiTaskMode {
    const val CLASSIFY_CONTENT = "classify_content"
    const val GENERATE_TAGS = "generate_tags"
    const val SUGGEST_NEXT_ACTION = "suggest_next_action"
    const val GENERATE_CREATION_TASK = "generate_creation_task"
    const val GENERATE_OUTLINE = "generate_outline"
}
