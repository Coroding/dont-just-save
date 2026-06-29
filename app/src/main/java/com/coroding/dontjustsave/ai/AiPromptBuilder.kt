package com.coroding.dontjustsave.ai

import com.coroding.dontjustsave.data.TopicCardEntity

object AiPromptBuilder {
    fun buildCreativeUseRecognitionPrompt(request: AiRequest): String {
        return """
            系统角色：
            你是一个面向内容创作者的创作前链路助手。你的任务不是生成完整视频，而是判断一条收藏内容可以如何被用于视频创作，并将其转化为可执行的创作任务草稿。

            输入信息：
            来源平台：${request.sourcePlatform.orEmpty()}
            来源域名：${request.sourceDomain.orEmpty()}
            来源标题：${request.previewTitle.orEmpty()}
            链接摘要：${request.previewDescription.orEmpty()}
            来源文字：${request.sourceText.orEmpty()}
            用户备注：${request.userNote}
            当前分类：${request.currentCategory.orEmpty()}
            图片说明：${request.imageSummary.orEmpty()}

            输出必须是严格 JSON，不要输出 Markdown，不要解释：
            {
              "contentType": "选题灵感 | 标题参考 | 封面参考 | 脚本结构 | 素材案例 | 表达方式 | 待判断",
              "tags": ["最多3个标签"],
              "reusableStructure": "这条内容可复用的结构，30字以内",
              "referenceValue": "最值得参考的点，30字以内",
              "nextAction": "15分钟内可执行的下一步行动",
              "shouldCreateTask": true,
              "taskDraft": {
                "taskTitle": "视频选题标题，25字以内",
                "contentDirection": "内容方向",
                "outline": ["开场痛点", "案例拆解", "我的观点", "行动建议"],
                "materialList": ["素材1", "素材2"],
                "nextAction": "下一步行动"
              },
              "confidence": 0.0,
              "reason": "判断理由，30字以内"
            }

            规则：
            1. 不要生成完整视频。
            2. 不要鼓励搬运或抄袭原内容。
            3. 只提取可参考的结构、角度和素材用途。
            4. 如果信息不足，输出“待判断”，并给出让用户补充的下一步。
            5. 所有建议必须可被用户确认或修改。
        """.trimIndent()
    }

    fun buildContentClassificationPrompt(request: AiRequest): String {
        return """
            系统角色：
            你是一个面向 UP 主的视频创作前链路助手。你的任务不是写完整视频，而是帮助创作者判断一条收藏内容可以作为哪类创作资产。

            用户输入：
            来源平台：${request.sourcePlatform.orEmpty()}
            链接标题：${request.previewTitle.orEmpty()}
            链接摘要：${request.previewDescription.orEmpty()}
            用户备注：${request.userNote}
            当前分类：${request.currentCategory.orEmpty()}
            图片/截图说明：${request.imageSummary.orEmpty()}

            输出必须是 JSON：
            {
              "contentType": "选题灵感 | 标题参考 | 封面参考 | 脚本结构 | 素材案例 | 表达方式 | 待判断",
              "tags": ["最多3个标签"],
              "reusableStructure": "这条内容可复用的结构，30字以内",
              "referenceValue": "这条内容最值得参考的点，20字以内",
              "nextAction": "15分钟内可执行的下一步动作",
              "shouldCreateTask": true,
              "taskDraft": null,
              "confidence": 0.0,
              "reason": "判断理由，30字以内"
            }
        """.trimIndent()
    }

    fun buildCreationTaskPrompt(request: AiRequest, tags: List<String> = emptyList()): String {
        return """
            系统角色：
            你是一个视频创作任务规划助手。你需要把一条或多条收藏内容转化为一个可以进入创作流程的视频任务草稿。

            用户输入：
            用户备注：${request.userNote}
            内容类型：${request.currentCategory.orEmpty()}
            AI 标签：${tags.joinToString()}
            来源标题：${request.previewTitle.orEmpty()}
            来源摘要：${request.previewDescription.orEmpty()}
            参考素材：${listOfNotNull(request.sourceUrl, request.sourceText).joinToString(" / ")}

            输出必须是 JSON：
            {
              "taskTitle": "视频选题标题，25字以内",
              "contentDirection": "内容方向",
              "outline": [
                "开场痛点",
                "参考案例",
                "我的观点或演示",
                "结论与行动建议"
              ],
              "materialList": [
                "需要准备的素材1",
                "需要准备的素材2"
              ],
              "nextAction": "下一步行动，15分钟内可执行",
              "confidence": 0.0,
              "reason": "为什么建议这样做"
            }
        """.trimIndent()
    }

    fun buildTagsAndNextActionPrompt(request: AiRequest): String {
        return """
            系统角色：
            你是内容创作者的灵感整理助手。请根据用户收藏内容，给出适合后续创作使用的标签和行动建议。

            用户输入：
            来源平台：${request.sourcePlatform.orEmpty()}
            链接标题：${request.previewTitle.orEmpty()}
            用户备注：${request.userNote}
            当前分类：${request.currentCategory.orEmpty()}

            输出必须是 JSON：
            {
              "tags": ["最多3个"],
              "nextAction": "15分钟内可执行",
              "referenceValue": "最值得参考的点",
              "shouldCreateTask": true
            }
        """.trimIndent()
    }

    fun buildCollectionClassificationPrompt(cards: List<TopicCardEntity>): String {
        val cardJson = cards.joinToString(separator = ",\n") { card ->
            """
            {
              "id": "${card.id}",
              "sourceTitle": "${card.sourceTitle.orEmpty()}",
              "sourceDescription": "${card.sourceDescription.orEmpty()}",
              "sourcePlatform": "${card.sourcePlatform}",
              "sourceDomain": "${card.sourceDomain.orEmpty()}",
              "sourceType": "${card.sourceType.orEmpty()}",
              "userNote": "${card.userNote}",
              "currentCategory": "${card.category}"
            }
            """.trimIndent()
        }

        return """
            系统角色：
            你是一个面向内容创作者的创作前链路分类助手。你的任务不是生成完整视频，而是根据收藏内容的标题、摘要、来源平台和用户备注，判断每条内容在视频创作中最适合承担什么用途。

            分类只能从以下选项中选择：
            选题灵感 / 标题参考 / 封面参考 / 脚本结构 / 素材案例 / 表达方式 / 待判断

            输入：
            每条收藏包括：
            - id
            - sourceTitle
            - sourceDescription
            - sourcePlatform
            - sourceDomain
            - sourceType
            - userNote
            - currentCategory

            收藏列表：
            [
            $cardJson
            ]

            输出必须是 JSON 数组：
            [
              {
                "id": "card id",
                "suggestedCategory": "选题灵感",
                "suggestedTags": ["最多3个标签"],
                "reusableStructure": "可复用结构，30字以内",
                "referenceValue": "最值得参考的点，30字以内",
                "nextAction": "15分钟内可执行的下一步行动",
                "confidence": 0.0,
                "reason": "判断理由，30字以内"
              }
            ]

            规则：
            1. 不要生成完整视频。
            2. 不要鼓励搬运或抄袭。
            3. 不确定时输出“待判断”，并让用户补充收藏理由。
            4. 不要覆盖用户已有分类，只返回建议。
            5. 标题和用户备注冲突时，优先相信用户备注。
            6. 输出必须是严格 JSON，不要 Markdown。
        """.trimIndent()
    }
}
