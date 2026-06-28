package com.coroding.dontjustsave.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository(
    private val config: AiConfig = AiConfigProvider.defaultConfig,
) {
    suspend fun analyze(request: AiRequest): Result<AiResponse> = withContext(Dispatchers.IO) {
        if (!config.enabled || config.baseUrl.isBlank()) {
            return@withContext Result.success(mockResponse(request))
        }

        // TODO AI: call config.baseUrl with config.apiKey and config.modelName.
        // Keep UI confirmation before applying any response.
        Result.failure(IllegalStateException("真实 AI 接口尚未配置"))
    }

    private fun mockResponse(request: AiRequest): AiResponse {
        val signal = listOfNotNull(
            request.userNote,
            request.previewTitle,
            request.previewDescription,
            request.sourceText,
            request.sourcePlatform,
            request.sourceDomain,
            request.currentCategory,
            request.imageSummary,
        ).joinToString(" ").lowercase()

        return when {
            signal.hasAny("reaction", "反应", "评论", "歌手", "视频", "综艺", "看完") ->
                buildReactionResponse(request)
            signal.hasAny("标题", "爆款", "选题", "热点", "话题", "流量") ->
                buildTitleResponse(request)
            signal.hasAny("封面", "截图", "配色", "字体", "排版", "视觉", "海报") ->
                buildCoverResponse(request)
            signal.hasAny("教程", "步骤", "方法", "怎么做", "教你", "指南", "流程", "脚本", "结构") ->
                buildScriptResponse(request)
            signal.hasAny("素材", "案例", "参考", "评论", "数据", "观点") ->
                buildMaterialResponse(request)
            else -> buildDefaultResponse(request)
        }
    }

    private fun buildReactionResponse(request: AiRequest) = AiResponse(
        contentType = "素材案例",
        tags = listOf("reaction", "视频结构", "案例拆解"),
        reusableStructure = "开场钩子 → 内容片段 → 观点反应 → 总结",
        referenceValue = "适合拆解 reaction 视频节奏设计",
        nextAction = "先拆出这个视频的前 30 秒结构",
        shouldCreateTask = true,
        confidence = 0.88f,
        reason = "包含可拆解的视频案例",
        taskDraft = CreationTaskDraft(
            taskTitle = "拆解一个 reaction 视频为什么吸引人",
            contentDirection = "案例拆解",
            outline = listOf(
                "开场：为什么 reaction 视频容易吸引点击",
                "案例：拆解原视频结构",
                "方法：总结可复用技巧",
                "结尾：给普通创作者的行动建议",
            ),
            materialList = buildMaterialList(request, "标题结构", "封面截图", "评论区反馈"),
            nextAction = "先写出 3 个标题版本",
        ),
    )

    private fun buildTitleResponse(request: AiRequest) = AiResponse(
        contentType = if (request.userNote.contains("选题")) "选题灵感" else "标题参考",
        tags = listOf("标题结构", "选题角度", "爆款案例"),
        reusableStructure = "强问题 + 明确对象 + 结果反差",
        referenceValue = "适合提炼成自己的视频选题",
        nextAction = "先改写 3 个适合自己账号的标题",
        shouldCreateTask = true,
        confidence = 0.84f,
        reason = "标题和选题信号明确",
        taskDraft = CreationTaskDraft(
            taskTitle = request.userNote.take(25).ifBlank { "把爆款标题改成视频选题" },
            contentDirection = "选题策划",
            outline = listOf("原标题为什么有效", "适配自己的受众", "改写 3 个标题", "选择一个进入脚本"),
            materialList = buildMaterialList(request, "原标题", "目标受众", "账号定位"),
            nextAction = "先写出 3 个标题版本",
        ),
    )

    private fun buildCoverResponse(request: AiRequest) = AiResponse(
        contentType = "封面参考",
        tags = listOf("封面结构", "视觉参考", "点击率"),
        reusableStructure = "大字标题 + 主体图像 + 低饱和背景",
        referenceValue = "适合作为视频封面排版参考",
        nextAction = "先标注这个封面里可复用的布局",
        shouldCreateTask = request.userNote.length >= 8,
        confidence = 0.8f,
        reason = "视觉和封面线索突出",
        taskDraft = CreationTaskDraft(
            taskTitle = request.userNote.take(25).ifBlank { "整理一组可复用封面样式" },
            contentDirection = "封面参考",
            outline = listOf("观察封面层级", "标注标题和主体", "复用到自己的选题", "生成封面草图"),
            materialList = buildMaterialList(request, "封面截图", "配色参考", "标题字号"),
            nextAction = "先标注 3 个可复用布局点",
        ),
    )

    private fun buildScriptResponse(request: AiRequest) = AiResponse(
        contentType = "脚本结构",
        tags = listOf("教程结构", "步骤拆解", "方法论"),
        reusableStructure = "问题引入 → 步骤演示 → 注意事项 → 总结",
        referenceValue = "适合复用为教程类视频脚本结构",
        nextAction = "先列出可复用的 4 个步骤",
        shouldCreateTask = true,
        confidence = 0.86f,
        reason = "包含方法或流程线索",
        taskDraft = CreationTaskDraft(
            taskTitle = request.userNote.take(25).ifBlank { "拆出一个教程视频脚本" },
            contentDirection = "教程方法",
            outline = listOf("开场提出问题", "演示关键步骤", "指出常见错误", "总结行动清单"),
            materialList = buildMaterialList(request, "步骤截图", "操作流程", "注意事项"),
            nextAction = "先写出教程前 30 秒脚本",
        ),
    )

    private fun buildMaterialResponse(request: AiRequest) = AiResponse(
        contentType = "素材案例",
        tags = listOf("素材参考", "案例积累", "观点补充"),
        reusableStructure = "案例现象 → 可引用观点 → 我的补充",
        referenceValue = "适合作为视频论据或案例",
        nextAction = "先记录这条素材能支撑哪个观点",
        shouldCreateTask = false,
        confidence = 0.78f,
        reason = "更像素材补充",
        taskDraft = null,
    )

    private fun buildDefaultResponse(request: AiRequest) = AiResponse(
        contentType = request.currentCategory?.takeIf { it.isNotBlank() } ?: "选题灵感",
        tags = listOf("创作灵感", "素材参考", "待完善"),
        reusableStructure = "从收藏内容中提取一个可复用创作角度",
        referenceValue = "需要用户补充具体创作意图",
        nextAction = "先补一句这条内容启发你做什么视频",
        shouldCreateTask = false,
        confidence = 0.72f,
        reason = "信息不足需确认",
        taskDraft = null,
    )

    private fun buildMaterialList(request: AiRequest, vararg extras: String): List<String> {
        return listOfNotNull(
            request.previewTitle?.takeIf { it.isNotBlank() }?.let { "来源标题：$it" },
            request.sourceUrl?.takeIf { it.isNotBlank() }?.let { "原链接：$it" },
            request.sourceText?.takeIf { it.isNotBlank() }?.let { "来源文字：${it.take(60)}" },
            request.imageSummary?.takeIf { it.isNotBlank() },
            *extras.map { "补充素材：$it" }.toTypedArray(),
        ).ifEmpty { listOf("补充原内容链接或截图", "记录自己的创作启发") }
    }

    private fun String.hasAny(vararg keywords: String): Boolean {
        return keywords.any { contains(it.lowercase()) }
    }
}
