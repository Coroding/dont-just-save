package com.coroding.dontjustsave

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coroding.dontjustsave.ai.AiRepository
import com.coroding.dontjustsave.ai.AiRequest
import com.coroding.dontjustsave.ai.AiResponse
import com.coroding.dontjustsave.ai.AiTaskMode
import com.coroding.dontjustsave.ai.CreationTaskDraft
import com.coroding.dontjustsave.data.AppDatabase
import com.coroding.dontjustsave.data.CreationTaskDao
import com.coroding.dontjustsave.data.CreationTaskEntity
import com.coroding.dontjustsave.data.TopicCardDao
import com.coroding.dontjustsave.data.TopicCardEntity
import com.coroding.dontjustsave.ui.theme.AppColors
import com.coroding.dontjustsave.ui.theme.AppGradients
import com.coroding.dontjustsave.ui.theme.CategoryColorMapper
import com.coroding.dontjustsave.ui.theme.DontJustSaveTheme
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MoreMenuAction(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

private val Categories = listOf(
    "选题灵感",
    "标题参考",
    "封面参考",
    "脚本结构",
    "素材案例",
    "表达方式",
    "待判断",
)

private val StatusOptions = listOf(
    StatusOption(label = "收集箱", value = "inbox"),
    StatusOption(label = "已转任务", value = "planned"),
    StatusOption(label = "以后再看", value = "later"),
    StatusOption(label = "已放弃", value = "dropped"),
    StatusOption(label = "已完成", value = "done"),
)

private const val ALL_CATEGORIES = "全部"
private const val DEFAULT_STATUS = "inbox"
private const val DEFAULT_TASK_STATUS = "待完善"
private const val SOURCE_PREVIEW_LENGTH = 100
private const val REVIEW_WINDOW_DAYS = 7L
private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
private val PageTitleSize = 32.sp
private val SectionTitleSize = 22.sp
private val CardTitleSize = 20.sp
private val BodyTextSize = 15.sp
private val CaptionTextSize = 13.sp

private val CreationTaskStatuses = listOf(
    "待完善",
    "待写脚本",
    "待生成素材",
    "待剪辑",
    "已发布",
    "已搁置",
)

private val CoverAspectRatios = listOf("4:3", "16:9", "3:4")

private data class StatusOption(
    val label: String,
    val value: String,
)

private val DeepNavy = AppColors.PrimaryText
private val CreamBackground = AppColors.AppBackground
private val CardCream = AppColors.CardBackground
private val SageGreen = AppColors.SoftMint
private val SoftMint = AppColors.SoftMint
private val WarmYellow = AppColors.HoneyCream
private val SoftPink = AppColors.FairyPink
private val MistBlue = AppColors.CrystalBlue
private val Lavender = AppColors.DreamPurple
private val Coral = AppColors.PeachMilk
private val SecondaryText = AppColors.SecondaryText
private val TertiaryText = AppColors.TertiaryText
private val SoftOutline = AppColors.Outline

private val CategoryColors = listOf(
    AppColors.DreamPurple,
    AppColors.CrystalBlue,
    AppColors.FairyPink,
    AppColors.PeachMilk,
    AppColors.HoneyCream,
    AppColors.SoftMint,
    AppColors.SoftLavender,
)

class MainActivity : ComponentActivity() {
    private val incomingShareState = mutableStateOf<ShareContent?>(null)
    private val clipboardShareState = mutableStateOf<ShareContent?>(null)
    private var lastPromptedClipboardUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingShareState.value = ShareContentParser.parse(intent)

        val database = AppDatabase.getDatabase(applicationContext)
        val topicCardDao = database.topicCardDao()
        val creationTaskDao = database.creationTaskDao()
        setContent {
            DontJustSaveTheme {
                DontJustSaveApp(
                    topicCardDao = topicCardDao,
                    creationTaskDao = creationTaskDao,
                    incomingShare = incomingShareState.value,
                    clipboardShare = clipboardShareState.value,
                    onShareHandled = { incomingShareState.value = null },
                    onClipboardShareHandled = { shareContent ->
                        lastPromptedClipboardUrl = shareContent.sourceUrl
                        clipboardShareState.value = null
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateClipboardPrompt()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingShareState.value = ShareContentParser.parse(intent)
    }

    private fun updateClipboardPrompt() {
        val shareContent = readClipboardShare(this) ?: return
        val sourceUrl = shareContent.sourceUrl ?: return
        if (sourceUrl == lastPromptedClipboardUrl) {
            return
        }
        clipboardShareState.value = shareContent
    }
}

private sealed class AppPage {
    data object Home : AppPage()
    data class QuickCapture(val sharedContent: ShareContent) : AppPage()
    data object Inbox : AppPage()
    data class TopicDetail(val cardId: String) : AppPage()
    data object CreationTasks : AppPage()
    data class CreationTaskDetail(val taskId: String) : AppPage()
    data object Review : AppPage()
}

@Composable
private fun DontJustSaveApp(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    incomingShare: ShareContent?,
    clipboardShare: ShareContent?,
    onShareHandled: () -> Unit,
    onClipboardShareHandled: (ShareContent) -> Unit,
) {
    var currentPage by remember { mutableStateOf<AppPage>(AppPage.Home) }
    val pageStack = remember { mutableStateListOf<AppPage>() }
    val appScope = rememberCoroutineScope()

    fun navigateTo(page: AppPage) {
        if (page == currentPage) {
            return
        }
        pageStack.add(currentPage)
        currentPage = page
    }

    fun navigateBack(fallbackPage: AppPage? = null) {
        if (pageStack.isNotEmpty()) {
            currentPage = pageStack.removeAt(pageStack.lastIndex)
            return
        }
        if (fallbackPage != null && currentPage != fallbackPage) {
            currentPage = fallbackPage
        }
    }

    fun clearAndNavigateTo(
        page: AppPage,
        backPage: AppPage? = null,
    ) {
        pageStack.clear()
        if (backPage != null && backPage != page) {
            pageStack.add(backPage)
        }
        currentPage = page
    }

    fun startLinkPreviewFetch(topicCard: TopicCardEntity) {
        if (topicCard.sourceUrl.isNullOrBlank()) {
            return
        }
        appScope.launch {
            fetchAndStoreLinkPreview(topicCardDao, topicCard)
        }
    }

    BackHandler(enabled = pageStack.isNotEmpty()) {
        navigateBack()
    }

    LaunchedEffect(incomingShare) {
        if (incomingShare != null) {
            clearAndNavigateTo(
                page = AppPage.QuickCapture(incomingShare),
                backPage = AppPage.Home,
            )
            onShareHandled()
        }
    }

    when (val page = currentPage) {
        AppPage.Home -> HomeScreen(
            topicCardDao = topicCardDao,
            clipboardShare = clipboardShare,
            onQuickCaptureClick = {
                navigateTo(AppPage.QuickCapture(ShareContent.manual()))
            },
            onInboxClick = { navigateTo(AppPage.Inbox) },
            onCreationTasksClick = { navigateTo(AppPage.CreationTasks) },
            onReviewClick = { navigateTo(AppPage.Review) },
            onCardClick = { cardId -> navigateTo(AppPage.TopicDetail(cardId)) },
            onSaveClipboardClick = { shareContent ->
                onClipboardShareHandled(shareContent)
                navigateTo(AppPage.QuickCapture(shareContent))
            },
            onIgnoreClipboardClick = { shareContent ->
                onClipboardShareHandled(shareContent)
            },
        )

        is AppPage.QuickCapture -> QuickCaptureScreen(
            topicCardDao = topicCardDao,
            creationTaskDao = creationTaskDao,
            sharedContent = page.sharedContent,
            onSaved = {
                clearAndNavigateTo(
                    page = AppPage.Inbox,
                    backPage = AppPage.Home,
                )
            },
            onTaskGenerated = { taskId ->
                clearAndNavigateTo(
                    page = AppPage.CreationTaskDetail(taskId),
                    backPage = AppPage.Home,
                )
            },
            onCancel = { navigateBack(fallbackPage = AppPage.Home) },
            onOpenExisting = { cardId -> navigateTo(AppPage.TopicDetail(cardId)) },
            onStartLinkPreviewFetch = ::startLinkPreviewFetch,
        )

        AppPage.Inbox -> InboxScreen(
            topicCardDao = topicCardDao,
            creationTaskDao = creationTaskDao,
            onBackClick = { navigateBack(fallbackPage = AppPage.Home) },
            onCardClick = { cardId -> navigateTo(AppPage.TopicDetail(cardId)) },
            onManualCaptureClick = {
                navigateTo(AppPage.QuickCapture(ShareContent.manual()))
            },
        )

        is AppPage.TopicDetail -> TopicDetailScreen(
            topicCardDao = topicCardDao,
            creationTaskDao = creationTaskDao,
            cardId = page.cardId,
            onBackClick = { navigateBack(fallbackPage = AppPage.Inbox) },
            onSaved = {
                clearAndNavigateTo(
                    page = AppPage.Inbox,
                    backPage = AppPage.Home,
                )
            },
            onTaskGenerated = { taskId ->
                clearAndNavigateTo(
                    page = AppPage.CreationTaskDetail(taskId),
                    backPage = AppPage.Inbox,
                )
            },
        )

        AppPage.CreationTasks -> CreationTaskPoolScreen(
            creationTaskDao = creationTaskDao,
            onBackClick = { navigateBack(fallbackPage = AppPage.Home) },
            onTaskClick = { taskId -> navigateTo(AppPage.CreationTaskDetail(taskId)) },
            onInboxClick = { navigateTo(AppPage.Inbox) },
            onCaptureClick = { navigateTo(AppPage.QuickCapture(ShareContent.manual())) },
        )

        is AppPage.CreationTaskDetail -> CreationTaskDetailScreen(
            topicCardDao = topicCardDao,
            creationTaskDao = creationTaskDao,
            taskId = page.taskId,
            onBackClick = { navigateBack(fallbackPage = AppPage.CreationTasks) },
        )

        AppPage.Review -> ReviewScreen(
            topicCardDao = topicCardDao,
            creationTaskDao = creationTaskDao,
            onBackClick = { navigateBack(fallbackPage = AppPage.Home) },
            onInboxClick = {
                clearAndNavigateTo(
                    page = AppPage.Inbox,
                    backPage = AppPage.Home,
                )
            },
            onTaskGenerated = { taskId ->
                clearAndNavigateTo(
                    page = AppPage.CreationTaskDetail(taskId),
                    backPage = AppPage.Home,
                )
            },
        )
    }
}

@Composable
private fun HomeScreen(
    topicCardDao: TopicCardDao,
    clipboardShare: ShareContent?,
    onQuickCaptureClick: () -> Unit,
    onInboxClick: () -> Unit,
    onCreationTasksClick: () -> Unit,
    onReviewClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onSaveClipboardClick: (ShareContent) -> Unit,
    onIgnoreClipboardClick: (ShareContent) -> Unit,
) {
    val topicCards by topicCardDao.observeAll().collectAsState(initial = emptyList())
    val weekStart = remember { System.currentTimeMillis() - REVIEW_WINDOW_DAYS * ONE_DAY_MILLIS }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAddingSamples by rememberSaveable { mutableStateOf(false) }
    val weeklyNewCount = topicCards.count { it.createdAt >= weekStart }
    val plannedCount = topicCards.count { it.status == "planned" }
    val reviewCount = topicCards.count {
        it.createdAt >= weekStart && (it.status == "inbox" || it.status == "later")
    }
    val recentCards = topicCards.take(2)

    Scaffold(containerColor = CreamBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AppPageHeader(
                title = "别只收藏",
                subtitle = "把刷到的内容，变成可创作的视频任务",
            )
            if (clipboardShare != null) {
                ClipboardLinkPromptCard(
                    shareContent = clipboardShare,
                    onSaveClick = { onSaveClipboardClick(clipboardShare) },
                    onIgnoreClick = { onIgnoreClipboardClick(clipboardShare) },
                )
            }
            HomeHeroCard()

            PrimaryButton(
                text = "记录创作灵感",
                onClick = onQuickCaptureClick,
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeEntryCard(
                        title = "灵感收集箱",
                        subtitle = "整理选题素材",
                        onClick = onInboxClick,
                        modifier = Modifier.weight(1f),
                    )
                    HomeEntryCard(
                        title = "创作任务池",
                        subtitle = "推进视频任务",
                        onClick = onCreationTasksClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                HomeEntryCard(
                    title = "开始复盘",
                    subtitle = "把近期灵感筛成下一条视频",
                    onClick = onReviewClick,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardStatCard(
                    label = "本周新增",
                    count = weeklyNewCount,
                    modifier = Modifier.weight(1f),
                )
                DashboardStatCard(
                    label = "已转任务",
                    count = plannedCount,
                    modifier = Modifier.weight(1f),
                )
                DashboardStatCard(
                    label = "待复盘",
                    count = reviewCount,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "最近创作灵感",
                fontSize = SectionTitleSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            if (recentCards.isEmpty()) {
                HomeEmptyGuide()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentCards.forEach { topicCard ->
                        HomeRecentCard(
                            topicCard = topicCard,
                            onClick = { onCardClick(topicCard.id) },
                        )
                    }
                }
            }

            TextButton(
                onClick = {
                    isAddingSamples = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                topicCardDao.insertAll(createDemoTopicCards())
                            }
                        }
                        isAddingSamples = false
                        val message = if (result.isSuccess) {
                            "示例数据已准备好"
                        } else {
                            "示例数据添加失败"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isAddingSamples,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = if (isAddingSamples) "正在添加示例..." else "添加 3 条演示数据")
            }
        }
    }
}

@Composable
private fun HomeHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(AppGradients.CreamPurple)
                .padding(20.dp),
        ) {
            DecorativeBlob(
                color = WarmYellow,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(58.dp),
            )
            DecorativeBlob(
                color = SoftMint,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(38.dp),
            )
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "AI 创作前链路助手",
                    fontSize = CardTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                )
                Text(
                    text = "识别收藏内容的创作用途，提取可复用结构，再生成可确认的任务草稿。",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourcePlatformChip(text = "用途识别")
                    SourcePlatformChip(text = "素材管理")
                    SourcePlatformChip(text = "任务草稿")
                }
            }
        }
    }
}

@Composable
private fun AppPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            fontSize = PageTitleSize,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
        )
        Text(
            text = subtitle,
            fontSize = BodyTextSize,
            color = SecondaryText,
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.HoneyCream,
            contentColor = DeepNavy,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DeepNavy,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HomeEntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                fontSize = BodyTextSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = subtitle,
                fontSize = CaptionTextSize,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(SoftOutline)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClipboardLinkPromptCard(
    shareContent: ShareContent,
    onSaveClick: () -> Unit,
    onIgnoreClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "检测到剪贴板链接",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${shareContent.sourcePlatform} · ${shareContent.sourceUrl.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSaveClick) {
                    Text(text = "保存为灵感")
                }
                OutlinedButton(onClick = onIgnoreClick) {
                    Text(text = "忽略")
                }
            }
        }
    }
}

private fun createDemoTopicCards(): List<TopicCardEntity> {
    val now = System.currentTimeMillis()
    return listOf(
        TopicCardEntity(
            id = "demo-ai-tool-experience",
            title = "AI 工具选题",
            sourceText = "一款 AI 笔记工具的上手体验：适合新手 UP 主先整理素材，再拆成视频任务。",
            sourceUrl = "https://example.com/ai-tool",
            sourcePlatform = "网页",
            sourceDomain = "example.com",
            shareType = "link",
            imageUri = null,
            userNote = "做一期 AI 工具体验：新手 UP 主如何把素材变成视频选题。",
            category = "选题灵感",
            status = "inbox",
            createdAt = now,
            updatedAt = now,
        ),
        TopicCardEntity(
            id = "demo-case-study",
            title = "标题结构参考",
            sourceText = "一个知识博主连续 7 天更新选题池的案例，重点是标题、封面和开场节奏。",
            sourceUrl = "https://example.com/case-study",
            sourcePlatform = "网页",
            sourceDomain = "example.com",
            shareType = "link",
            imageUri = null,
            userNote = "拆解一个 UP 主如何把零散素材变成系列视频任务。",
            category = "标题参考",
            status = "planned",
            createdAt = now - ONE_DAY_MILLIS,
            updatedAt = now - ONE_DAY_MILLIS,
        ),
        TopicCardEntity(
            id = "demo-verify-opinion",
            title = "待判断素材",
            sourceText = "有人说选题池越多，真正产出的视频越少。这个观点需要找案例判断。",
            sourceUrl = null,
            sourcePlatform = "未知来源",
            sourceDomain = null,
            shareType = "text",
            imageUri = null,
            userNote = "判断这个观点能不能做成一期创作者效率类视频。",
            category = "待判断",
            status = "later",
            createdAt = now - 2 * ONE_DAY_MILLIS,
            updatedAt = now - 2 * ONE_DAY_MILLIS,
        ),
    )
}

@Composable
private fun HomeRecentCard(
    topicCard: TopicCardEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = topicCard.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip(text = topicCard.category, selected = false, onClick = {})
                StatusChip(text = statusLabel(topicCard.status), selected = false, onClick = {})
            }
            Text(
                text = topicCard.userNote.take(60),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeEmptyGuide() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
    ) {
        Text(
            text = "保存第一条视频灵感，看看它能变成什么创作任务。",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeEntryButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.HoneyCream,
            contentColor = DeepNavy,
        ),
    ) {
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCaptureScreen(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    sharedContent: ShareContent,
    onSaved: () -> Unit,
    onTaskGenerated: (String) -> Unit,
    onCancel: () -> Unit,
    onOpenExisting: (String) -> Unit,
    onStartLinkPreviewFetch: (TopicCardEntity) -> Unit,
) {
    var currentShareContent by remember { mutableStateOf(sharedContent) }
    var userNote by rememberSaveable(sharedContent.rawText, sharedContent.sourceUrl, sharedContent.imageUri) {
        mutableStateOf("")
    }
    var selectedCategory by rememberSaveable(sharedContent.rawText, sharedContent.sourceUrl, sharedContent.imageUri) {
        mutableStateOf(Categories.first())
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showReplaceSourceDialog by rememberSaveable { mutableStateOf(false) }
    var pendingTopicCard by remember { mutableStateOf<TopicCardEntity?>(null) }
    var duplicateExistingCard by remember { mutableStateOf<TopicCardEntity?>(null) }
    var quickCroppedImagePath by rememberSaveable(sharedContent.rawText, sharedContent.sourceUrl, sharedContent.imageUri) {
        mutableStateOf<String?>(null)
    }
    var selectedCoverAspectRatio by rememberSaveable(sharedContent.rawText, sharedContent.sourceUrl, sharedContent.imageUri) {
        mutableStateOf("4:3")
    }
    var aiSuggestion by remember { mutableStateOf<AiResponse?>(null) }
    var appliedAiSuggestion by remember { mutableStateOf<AiResponse?>(null) }
    var isAiLoading by rememberSaveable { mutableStateOf(false) }
    var aiErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val aiRepository = remember { AiRepository() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            tryTakePersistableImagePermission(context, uri.toString())
            currentShareContent = currentShareContent.withPickedImage(uri.toString(), localImagePath = null)
            ImageStorageHelper.deleteLocalImageIfExists(quickCroppedImagePath)
            quickCroppedImagePath = null
            errorMessage = null
            scope.launch {
                val localPath = withContext(Dispatchers.IO) {
                    ImageStorageHelper.copyImageToPrivateStorage(context, uri)
                }
                if (currentShareContent.imageUri != uri.toString()) {
                    ImageStorageHelper.deleteLocalImageIfExists(localPath)
                } else if (localPath != null) {
                    currentShareContent = currentShareContent.withPickedImage(
                        imageUri = uri.toString(),
                        localImagePath = localPath,
                    )
                } else {
                    errorMessage = "图片复制失败，将尝试使用原始图片来源"
                }
            }
        }
    }

    LaunchedEffect(currentShareContent.imageUri) {
        currentShareContent.imageUri?.let { imageUri ->
            tryTakePersistableImagePermission(context, imageUri)
        }
    }

    fun pasteClipboardLink() {
        val clipboardContent = readClipboardShare(context)
        if (clipboardContent == null) {
            errorMessage = "剪贴板中没有可识别链接"
            return
        }
        currentShareContent = clipboardContent
        ImageStorageHelper.deleteLocalImageIfExists(quickCroppedImagePath)
        quickCroppedImagePath = null
        errorMessage = null
    }

    fun runAiAnalysis() {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isBlank()) {
            aiErrorMessage = "建议先补一句创作启发，结果会更准确。"
            return
        }
        isAiLoading = true
        aiErrorMessage = null
        // TODO metrics: track ai_recognition_clicked.
        scope.launch {
            val result = aiRepository.analyze(
                AiRequest(
                    sourceUrl = currentShareContent.sourceUrl,
                    sourceText = currentShareContent.rawText,
                    sourcePlatform = currentShareContent.sourcePlatform,
                    sourceDomain = currentShareContent.sourceDomain,
                    userNote = trimmedNote,
                    imageSummary = if (currentShareContent.userImageModel() != null) "用户提供了一张图片作为封面参考" else null,
                    currentCategory = selectedCategory,
                    previewTitle = null,
                    previewDescription = null,
                    taskMode = AiTaskMode.CLASSIFY_CONTENT,
                ),
            )
            result
                .onSuccess { response ->
                    aiSuggestion = response
                    aiErrorMessage = null
                    // TODO metrics: track ai_suggestion_generated.
                }
                .onFailure { error ->
                    aiErrorMessage = error.message ?: "AI 识别失败，不影响保存。"
                }
            isAiLoading = false
        }
    }

    fun applyAiSuggestion(response: AiResponse) {
        selectedCategory = response.contentType
        aiSuggestion = response
        appliedAiSuggestion = response
        aiErrorMessage = null
        // TODO metrics: track ai_suggestion_applied and ai_tag_acceptance.
    }

    fun cropQuickCover() {
        val imageUri = currentShareContent.imageUri
        val localImagePath = currentShareContent.localImagePath
        if (imageUri.isNullOrBlank() && localImagePath.isNullOrBlank()) {
            errorMessage = "请先添加截图/图片。"
            return
        }
        scope.launch {
            val preparedContent = currentShareContent.ensureLocalImageCopy(context) { message ->
                errorMessage = message
            }
            currentShareContent = preparedContent
            val croppedPath = withContext(Dispatchers.IO) {
                ImageStorageHelper.createCroppedCover(
                    context = context,
                    localImagePath = preparedContent.localImagePath,
                    imageUri = preparedContent.imageUri,
                    aspectRatio = selectedCoverAspectRatio,
                )
            }
            if (croppedPath != null) {
                ImageStorageHelper.deleteLocalImageIfExists(quickCroppedImagePath)
                quickCroppedImagePath = croppedPath
                Toast.makeText(context, "已裁剪为 $selectedCoverAspectRatio 封面", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "裁剪失败，请换一张图片再试。"
            }
        }
    }

    fun saveCurrentCard(
        generateTask: Boolean,
        taskSuggestion: AiResponse? = appliedAiSuggestion,
    ) {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isBlank()) {
            errorMessage = "请先补一句视频创作灵感。"
            return
        }

        isSaving = true
        errorMessage = null
        scope.launch {
            val preparedContent = currentShareContent.ensureLocalImageCopy(context) { message ->
                errorMessage = message
            }
            currentShareContent = preparedContent
            val now = System.currentTimeMillis()
            val topicCard = TopicCardEntity(
                id = UUID.randomUUID().toString(),
                title = trimmedNote.take(20),
                sourceText = preparedContent.rawText?.takeIf { it.isNotBlank() },
                sourceUrl = preparedContent.sourceUrl?.takeIf { it.isNotBlank() },
                sourcePlatform = preparedContent.sourcePlatform,
                sourceDomain = preparedContent.sourceDomain,
                shareType = preparedContent.normalizedShareType(),
                imageUri = preparedContent.imageUri,
                localImagePath = preparedContent.localImagePath,
                croppedImagePath = quickCroppedImagePath,
                coverAspectRatio = selectedCoverAspectRatio,
                previewStatus = if (!preparedContent.sourceUrl.isNullOrBlank()) {
                    "loading"
                } else {
                    "idle"
                },
                userNote = trimmedNote,
                category = taskSuggestion?.contentType ?: selectedCategory,
                aiTags = taskSuggestion?.tags?.joinToString(","),
                aiReusableStructure = taskSuggestion?.reusableStructure,
                aiReferenceValue = taskSuggestion?.referenceValue,
                aiReason = taskSuggestion?.reason,
                aiConfidence = taskSuggestion?.confidence,
                nextAction = taskSuggestion?.nextAction,
                status = DEFAULT_STATUS,
                createdAt = now,
                updatedAt = now,
            )

            try {
                val existingCard = topicCard.sourceUrl?.let { sourceUrl ->
                    withContext(Dispatchers.IO) {
                        topicCardDao.findBySourceUrl(sourceUrl)
                    }
                }
                if (existingCard != null && !generateTask) {
                    pendingTopicCard = topicCard
                    duplicateExistingCard = existingCard
                    return@launch
                }
                saveTopicCard(topicCardDao, topicCard)
                onStartLinkPreviewFetch(topicCard)
                if (generateTask) {
                    val task = createCreationTaskFromCard(topicCard, taskSuggestion)
                    withContext(Dispatchers.IO) {
                        creationTaskDao.insert(task)
                    }
                    // TODO metrics: track ai_task_draft_created and card_to_task_conversion.
                    Toast.makeText(context, "已生成创作任务", Toast.LENGTH_SHORT).show()
                    onTaskGenerated(task.id)
                } else {
                    onSaved()
                }
            } catch (error: Exception) {
                errorMessage = "保存失败：${error.message ?: "未知错误"}"
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "记录") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = DeepNavy,
                    navigationIconContentColor = DeepNavy,
                ),
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text(text = "取消")
                    }
                },
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp),
                colors = CardDefaults.cardColors(containerColor = CardCream),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrimaryButton(
                        text = if (isSaving) "保存中..." else "保存为灵感",
                        onClick = { saveCurrentCard(generateTask = false) },
                        enabled = !isSaving,
                    )
                    SecondaryButton(
                        text = "生成创作任务",
                        onClick = { saveCurrentCard(generateTask = true) },
                        enabled = !isSaving,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "记录一个视频灵感",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = "把链接、截图或文字先收进来，再转成创作任务。",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
            )

            SourcePreviewCard(
                sharedContent = currentShareContent,
                croppedImagePath = quickCroppedImagePath,
                selectedAspectRatio = selectedCoverAspectRatio,
                onAspectRatioSelected = { selectedCoverAspectRatio = it },
                onPasteLinkClick = {
                    if (!currentShareContent.sourceUrl.isNullOrBlank()) {
                        showReplaceSourceDialog = true
                    } else {
                        pasteClipboardLink()
                    }
                },
                onPickImageClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemoveImageClick = {
                    ImageStorageHelper.deleteLocalImageIfExists(quickCroppedImagePath)
                    quickCroppedImagePath = null
                    currentShareContent = currentShareContent.withoutPickedImage()
                },
                onCropCoverClick = ::cropQuickCover,
            )

            Text(
                text = "这个内容启发你做什么视频？",
                fontSize = CardTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = DeepNavy,
            )
            OutlinedTextField(
                value = userNote,
                onValueChange = {
                    userNote = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "例如：可以做一期 AI 工具测评 / reaction 视频 / 教程拆解") },
                minLines = 4,
                maxLines = 5,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepNavy,
                    unfocusedBorderColor = SoftOutline,
                    focusedContainerColor = CardCream,
                    unfocusedContainerColor = CardCream,
                ),
            )

            Text(
                text = "分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CategorySelector(
                categories = Categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
            )

            AiSuggestionCard(
                aiSuggestion = aiSuggestion,
                isLoading = isAiLoading,
                errorMessage = aiErrorMessage,
                onAnalyzeClick = ::runAiAnalysis,
                onApplyClick = { response -> applyAiSuggestion(response) },
                onGenerateTaskClick = { response ->
                    applyAiSuggestion(response)
                    saveCurrentCard(generateTask = true, taskSuggestion = response)
                },
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

        }
    }

    if (showReplaceSourceDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceSourceDialog = false },
            title = { Text(text = "是否用剪贴板链接替换当前来源？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceSourceDialog = false
                        pasteClipboardLink()
                    },
                ) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceSourceDialog = false }) {
                    Text(text = "取消")
                }
            },
        )
    }

    if (duplicateExistingCard != null && pendingTopicCard != null) {
        AlertDialog(
            onDismissRequest = {
                duplicateExistingCard = null
                pendingTopicCard = null
            },
            title = { Text(text = "这个链接已经收藏过，是否仍然保存？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = duplicateExistingCard?.title.orEmpty())
                    Text(
                        text = duplicateExistingCard?.sourceUrl.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cardToSave = pendingTopicCard ?: return@TextButton
                        duplicateExistingCard = null
                        pendingTopicCard = null
                        scope.launch {
                            try {
                                saveTopicCard(topicCardDao, cardToSave)
                                onStartLinkPreviewFetch(cardToSave)
                                onSaved()
                            } catch (error: Exception) {
                                errorMessage = "保存失败：${error.message ?: "未知错误"}"
                            }
                        }
                    },
                ) {
                    Text(text = "仍然保存")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val existingId = duplicateExistingCard?.id ?: return@TextButton
                            duplicateExistingCard = null
                            pendingTopicCard = null
                            onOpenExisting(existingId)
                        },
                    ) {
                        Text(text = "查看已有")
                    }
                    TextButton(
                        onClick = {
                            duplicateExistingCard = null
                            pendingTopicCard = null
                        },
                    ) {
                        Text(text = "取消")
                    }
                }
            },
        )
    }
}

@Composable
private fun SourcePreviewCard(
    sharedContent: ShareContent,
    croppedImagePath: String?,
    selectedAspectRatio: String,
    onAspectRatioSelected: (String) -> Unit,
    onPasteLinkClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onCropCoverClick: () -> Unit,
) {
    val userImageModel = croppedImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: sharedContent.userImageModel()
    val hasImage = userImageModel != null
    val imageBadge = if (!croppedImagePath.isNullOrBlank()) "已裁剪封面" else "用户图片"
    val sourceText = when {
        !sharedContent.sourceUrl.isNullOrBlank() -> sharedContent.sourceUrl
        hasImage -> "已接收截图/图片"
        !sharedContent.rawText.isNullOrBlank() -> sharedContent.rawText.take(120)
        else -> "手动记录"
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "来源预览",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy,
                )
                SourcePlatformChip(text = sharedContent.sourcePlatform)
            }

            if (hasImage) {
                PreviewCoverImage(
                    imageModel = userImageModel,
                    badge = imageBadge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(coverPreviewHeight(selectedAspectRatio, wide = true)),
                    cornerRadius = 20.dp,
                )
                Text(
                    text = "这张图片将作为卡片封面",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                SourceTextFallback(text = sourceText)
            }

            if (hasImage) {
                Text(
                    text = "选择封面比例",
                    style = MaterialTheme.typography.labelMedium,
                    color = DeepNavy,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoverAspectRatios.forEach { ratio ->
                        StatusChip(
                            text = ratio,
                            selected = selectedAspectRatio == ratio,
                            onClick = { onAspectRatioSelected(ratio) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPasteLinkClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeepNavy,
                    ),
                ) {
                    Text(text = "粘贴链接")
                }
                if (hasImage) {
                    MoreActionsButton(
                        actions = listOf(
                            MoreMenuAction(
                                label = "替换截图/图片",
                                onClick = onPickImageClick,
                            ),
                            MoreMenuAction(
                                label = "裁剪封面",
                                onClick = onCropCoverClick,
                            ),
                            MoreMenuAction(
                                label = "移除图片",
                                destructive = true,
                                onClick = onRemoveImageClick,
                            ),
                        ),
                    )
                } else {
                    OutlinedButton(
                        onClick = onPickImageClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DeepNavy,
                        ),
                    ) {
                        Text(text = "添加截图/图片")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(
                text = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxScreen(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    onBackClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onManualCaptureClick: () -> Unit,
) {
    val topicCards by topicCardDao.observeAll().collectAsState(initial = emptyList())
    val creationTasks by creationTaskDao.observeAll().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategoryFilter by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }
    var selectedStatusFilter by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }
    var cardPendingDelete by remember { mutableStateOf<TopicCardEntity?>(null) }
    val categoryFilters = remember { listOf(ALL_CATEGORIES) + Categories }
    val statusFilters = remember { listOf(StatusOption(label = ALL_CATEGORIES, value = ALL_CATEGORIES)) + StatusOptions }
    val plannedCount = topicCards.count { it.status == "planned" }
    val laterCount = topicCards.count { it.status == "later" }
    val filteredCards = topicCards.filter { topicCard ->
        val categoryMatches = selectedCategoryFilter == ALL_CATEGORIES ||
            topicCard.category == selectedCategoryFilter
        val statusMatches = selectedStatusFilter == ALL_CATEGORIES ||
            topicCard.status == selectedStatusFilter
        categoryMatches && statusMatches
    }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "灵感收集箱") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = DeepNavy,
                    navigationIconContentColor = DeepNavy,
                ),
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            InboxHeader(
                totalCount = topicCards.size,
                plannedCount = plannedCount,
                laterCount = laterCount,
            )
            FilterSection(
                categoryFilters = categoryFilters,
                selectedCategory = selectedCategoryFilter,
                onCategorySelected = { selectedCategoryFilter = it },
                statusFilters = statusFilters,
                selectedStatus = selectedStatusFilter,
                onStatusSelected = { selectedStatusFilter = it },
            )

            if (topicCards.isEmpty()) {
                EmptyInbox(onManualCaptureClick = onManualCaptureClick)
            } else if (filteredCards.isEmpty()) {
                EmptyFilteredInbox()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredCards, key = { it.id }) { topicCard ->
                        TopicCardItem(
                            topicCard = topicCard,
                            creationTask = creationTasks.firstOrNull { it.hasSourceCard(topicCard.id) },
                            onClick = { onCardClick(topicCard.id) },
                            onDeleteClick = { cardPendingDelete = topicCard },
                        )
                    }
                }
            }
        }
    }

    DeleteTopicCardDialog(
        visible = cardPendingDelete != null,
        onDismiss = { cardPendingDelete = null },
        onConfirm = {
            val cardToDelete = cardPendingDelete ?: return@DeleteTopicCardDialog
            scope.launch {
                val result = runCatching {
                    deleteTopicCard(
                        topicCardDao = topicCardDao,
                        cardId = cardToDelete.id,
                        localImagePath = cardToDelete.localImagePath,
                        croppedImagePath = cardToDelete.croppedImagePath,
                    )
                }
                cardPendingDelete = null
                if (result.isFailure) {
                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
}

@Composable
private fun InboxHeader(
    totalCount: Int,
    plannedCount: Int,
    laterCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
                text = "灵感收集箱",
                fontSize = PageTitleSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = "把刷到的选题、封面、标题和素材整理成创作资产",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryText,
            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InboxStatCard(
                label = "全部卡片",
                count = totalCount,
                modifier = Modifier.weight(1f),
            )
            InboxStatCard(
                label = "已转任务",
                count = plannedCount,
                modifier = Modifier.weight(1f),
            )
            InboxStatCard(
                label = "以后再看",
                count = laterCount,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InboxStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FilterSection(
    categoryFilters: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    statusFilters: List<StatusOption>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "内容类型",
            fontSize = CaptionTextSize,
            fontWeight = FontWeight.SemiBold,
            color = TertiaryText,
        )
        FilterChipRow(
            labels = categoryFilters,
            selectedLabel = selectedCategory,
            onSelected = onCategorySelected,
        )
        Text(
            text = "状态",
            fontSize = CaptionTextSize,
            fontWeight = FontWeight.SemiBold,
            color = TertiaryText,
        )
        StatusFilterChipRow(
            statusFilters = statusFilters,
            selectedStatus = selectedStatus,
            onStatusSelected = onStatusSelected,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipRow(
    labels: List<String>,
    selectedLabel: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            CategoryChip(
                text = label,
                selected = selectedLabel == label,
                onClick = { onSelected(label) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterChipRow(
    statusFilters: List<StatusOption>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        statusFilters.forEach { status ->
            StatusChip(
                text = status.label,
                selected = selectedStatus == status.value,
                onClick = { onStatusSelected(status.value) },
            )
        }
    }
}

@Composable
private fun EmptyInbox(onManualCaptureClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "还没有灵感素材",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "从浏览器或内容平台分享链接、文字或截图，开始建立你的 UP 主创作资产。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Button(onClick = onManualCaptureClick) {
            Text(text = "手动记录")
        }
    }
}

@Composable
private fun EmptyFilteredInbox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "当前筛选下没有卡片",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopicCardItem(
    topicCard: TopicCardEntity,
    creationTask: CreationTaskEntity?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val context = LocalContext.current
    val noteSummary = topicCard.userNote.take(60)
    val sourceLabel = topicCard.sourceDomain
        ?: topicCard.sourceUrl?.let(::extractDomain)
        ?: if (!topicCard.imageUri.isNullOrBlank()) "截图来源" else "文本来源"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            DecorativeBlob(
                color = categoryColor(topicCard.category).copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(38.dp),
            )
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            AssetCoverPreview(
                topicCard = topicCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            )
            Text(
                text = topicCard.title,
                fontSize = CardTitleSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryChip(text = topicCard.category, selected = false, onClick = {})
                SourcePlatformChip(text = topicCard.sourcePlatform)
                SourcePlatformChip(text = shareTypeLabel(topicCard.shareType))
                StatusChip(text = statusLabel(topicCard.status), selected = false, onClick = {})
            }
            Text(
                text = noteSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!topicCard.aiTags.isNullOrBlank()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topicCard.aiTags.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { tag -> SourcePlatformChip(text = "AI $tag") }
                }
            }
            if (!topicCard.nextAction.isNullOrBlank()) {
                Text(
                    text = "AI 下一步：${topicCard.nextAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (creationTask != null) {
                Text(
                    text = "下一步行动：${creationTask.nextAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepNavy,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(text = "已生成任务", selected = false, onClick = {})
            } else {
                StatusChip(text = "未生成任务", selected = false, onClick = {})
            }
            if (!topicCard.sourceUrl.isNullOrBlank()) {
                LinkPreviewCard(
                    topicCard = topicCard,
                    compact = true,
                    onClick = {
                        OpenSourceHelper.openSource(
                            context = context,
                            sourceUrl = topicCard.sourceUrl,
                            sourcePlatform = topicCard.sourcePlatform,
                            sourceDomain = topicCard.sourceDomain,
                        ) { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            } else if (topicCard.coverImageModel() != null) {
                ImageSourcePreviewCard(imageModel = topicCard.coverImageModel())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${topicCard.sourcePlatform} · $sourceLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatRelativeDate(topicCard.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryText,
                )
                MoreActionsButton(
                    actions = listOf(
                        MoreMenuAction(
                            label = "打开来源",
                            onClick = {
                                if (topicCard.sourceUrl.isNullOrBlank()) {
                                    Toast.makeText(context, "这条灵感没有来源链接", Toast.LENGTH_SHORT).show()
                                } else {
                                    OpenSourceHelper.openSource(
                                        context = context,
                                        sourceUrl = topicCard.sourceUrl,
                                        sourcePlatform = topicCard.sourcePlatform,
                                        sourceDomain = topicCard.sourceDomain,
                                    ) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        ),
                        MoreMenuAction(
                            label = "生成创作任务",
                            onClick = onClick,
                        ),
                        MoreMenuAction(
                            label = "改状态",
                            onClick = {
                                Toast.makeText(context, "请进入详情页修改状态", Toast.LENGTH_SHORT).show()
                            },
                        ),
                        MoreMenuAction(
                            label = "删除",
                            destructive = true,
                            onClick = onDeleteClick,
                        ),
                    ),
                )
            }
            }
        }
    }
}

@Composable
private fun MoreActionsButton(
    actions: List<MoreMenuAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.textButtonColors(
                contentColor = SecondaryText,
            ),
        ) {
            Text(
                text = "更多",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            color = if (action.destructive) Coral else DeepNavy,
                        )
                    },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun DeleteTopicCardDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "删除这条灵感？") },
        text = { Text(text = "删除后无法恢复。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Coral,
                ),
            ) {
                Text(text = "删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
    )
}

@Composable
private fun ImageSourcePreviewCard(imageModel: Any?) {
    if (imageModel == null) {
        return
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CreamBackground,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "截图来源",
                style = MaterialTheme.typography.labelMedium,
                color = DeepNavy,
                fontWeight = FontWeight.Bold,
            )
            PreviewCoverImage(
                imageModel = imageModel,
                badge = "用户图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                cornerRadius = 16.dp,
            )
        }
    }
}

@Composable
private fun AssetCoverPreview(
    topicCard: TopicCardEntity,
    modifier: Modifier = Modifier,
) {
    val imageModel = topicCard.coverImageModel()
    val badge = when {
        !topicCard.croppedImagePath.isNullOrBlank() -> "已裁剪封面"
        !topicCard.localImagePath.isNullOrBlank() || !topicCard.imageUri.isNullOrBlank() -> "用户图片"
        !topicCard.previewImageUrl.isNullOrBlank() -> "链接预览"
        else -> "渐变占位"
    }
    if (imageModel != null) {
        PreviewCoverImage(
            imageModel = imageModel,
            badge = badge,
            modifier = modifier,
            cornerRadius = 20.dp,
        )
    } else {
        PreviewCoverPlaceholder(
            text = topicCard.category,
            modifier = modifier,
            cornerRadius = 20.dp,
        )
    }
}

@Composable
private fun LinkPreviewCard(
    topicCard: TopicCardEntity,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val sourceDomain = topicCard.sourceDomain
        ?: topicCard.sourceUrl?.let(::extractDomain)
        ?: topicCard.sourceUrl
        ?: "未知域名"
    val title = topicCard.previewTitle ?: sourceDomain
    val coverImageModel = topicCard.coverImageModel()
    val coverBadge = when {
        !topicCard.croppedImagePath.isNullOrBlank() -> "已裁剪封面"
        !topicCard.localImagePath.isNullOrBlank() || !topicCard.imageUri.isNullOrBlank() -> "用户图片"
        else -> "链接预览"
    }
    val cardModifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CreamBackground,
        ),
    ) {
        val hasImage = coverImageModel != null
        if (compact) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasImage) {
                    PreviewCoverImage(
                        imageModel = coverImageModel,
                        badge = coverBadge,
                        modifier = Modifier.size(width = 96.dp, height = 78.dp),
                        cornerRadius = 16.dp,
                    )
                } else {
                    PreviewCoverPlaceholder(
                        text = topicCard.category,
                        modifier = Modifier.size(width = 92.dp, height = 76.dp),
                        cornerRadius = 16.dp,
                    )
                }
                LinkPreviewTextContent(
                    sourceLine = "${topicCard.sourcePlatform} · $sourceDomain",
                    title = title,
                    description = topicCard.previewDescription,
                    status = topicCard.previewStatus,
                    sourceUrl = topicCard.sourceUrl,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasImage) {
                    PreviewCoverImage(
                        imageModel = coverImageModel,
                        badge = coverBadge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 120.dp else 218.dp),
                        cornerRadius = if (compact) 16.dp else 20.dp,
                    )
                }
                if (!hasImage) {
                    PreviewCoverPlaceholder(
                        text = topicCard.category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                        cornerRadius = if (compact) 16.dp else 20.dp,
                    )
                }
                LinkPreviewTextContent(
                    sourceLine = "${topicCard.sourcePlatform} · $sourceDomain",
                    title = title.ifBlank { sourceDomain },
                    description = topicCard.previewDescription,
                    status = topicCard.previewStatus,
                    sourceUrl = topicCard.sourceUrl,
                )
            }
        }
    }
}

@Composable
private fun SourceTextFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CreamBackground)
            .padding(14.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LinkPreviewTextContent(
    sourceLine: String,
    title: String,
    description: String?,
    status: String,
    sourceUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = sourceLine,
            style = MaterialTheme.typography.labelMedium,
            color = DeepNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        when (status) {
            "loading" -> Text(
                text = "正在生成链接预览...",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
            )

            "failed" -> {
                Text(
                    text = sourceUrl.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepNavy,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "链接预览暂不可用",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
            }

            else -> {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCoverPlaceholder(
    text: String,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(AppGradients.PurpleBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = DeepNavy,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PreviewCoverImage(
    imageModel: Any?,
    badge: String,
    modifier: Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = "卡片封面",
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.SoftLavender.copy(alpha = 0.7f)),
            contentScale = ContentScale.Crop,
            loading = {
                PreviewCoverFallback(text = "加载中")
            },
            error = {
                PreviewCoverFallback(text = badge)
            },
            success = {
                SubcomposeAsyncImageContent()
            },
        )
        Text(
            text = badge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.DreamPurple.copy(alpha = 0.78f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun TopicCardEntity.coverImageModel(): Any? {
    return croppedImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: localImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: imageUri?.takeIf { it.isNotBlank() }
        ?: previewImageUrl?.takeIf { it.isNotBlank() }
}

private fun ShareContent.userImageModel(): Any? {
    return localImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: imageUri?.takeIf { it.isNotBlank() }
}

private fun coverPreviewHeight(
    aspectRatio: String,
    wide: Boolean,
) = when (aspectRatio) {
    "16:9" -> if (wide) 188.dp else 120.dp
    "3:4" -> if (wide) 260.dp else 132.dp
    else -> if (wide) 210.dp else 126.dp
}

@Composable
private fun PreviewCoverFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SoftLavender.copy(alpha = 0.74f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = SecondaryText,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DecorativeBlob(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp, 28.dp, 16.dp, 26.dp))
            .background(color),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val baseColor = categoryColor(text)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = baseColor.copy(alpha = 0.28f),
            labelColor = DeepNavy,
            selectedContainerColor = baseColor.copy(alpha = 0.72f),
            selectedLabelColor = DeepNavy,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = baseColor.copy(alpha = 0.36f),
            selectedBorderColor = baseColor,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val baseColor = statusColor(text)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = baseColor.copy(alpha = 0.22f),
            labelColor = DeepNavy,
            selectedContainerColor = baseColor.copy(alpha = 0.58f),
            selectedLabelColor = DeepNavy,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = baseColor.copy(alpha = 0.72f),
            selectedBorderColor = DeepNavy,
        ),
    )
}

@Composable
private fun SourcePlatformChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MistBlue.copy(alpha = 0.22f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = DeepNavy,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

private fun categoryColor(category: String): Color {
    return CategoryColorMapper.colorFor(category)
}

private fun statusColor(statusLabel: String): Color {
    return when (statusLabel) {
        StatusOptions.getOrNull(0)?.label -> MistBlue
        StatusOptions.getOrNull(1)?.label -> WarmYellow
        StatusOptions.getOrNull(2)?.label -> Lavender
        StatusOptions.getOrNull(3)?.label -> SoftPink
        StatusOptions.getOrNull(4)?.label -> SoftMint
        else -> SoftOutline
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreationTaskPoolScreen(
    creationTaskDao: CreationTaskDao,
    onBackClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onInboxClick: () -> Unit,
    onCaptureClick: () -> Unit,
) {
    val creationTasks by creationTaskDao.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "创作任务池") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = DeepNavy,
                    navigationIconContentColor = DeepNavy,
                ),
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (creationTasks.isEmpty()) {
            EmptyCreationTaskPool(
                onInboxClick = onInboxClick,
                onCaptureClick = onCaptureClick,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(creationTasks, key = { it.id }) { task ->
                    CreationTaskItem(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCreationTaskPool(
    onInboxClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "还没有创作任务",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "从灵感卡片点击“生成创作任务”，这里会出现视频选题、脚本大纲和素材清单。",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
        )
        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(text = "去灵感收集箱", onClick = onInboxClick)
        SecondaryButton(text = "记录一个灵感", onClick = onCaptureClick)
    }
}

@Composable
private fun CreationTaskItem(
    task: CreationTaskEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (task.generatedByAi) {
                    SourcePlatformChip(text = "AI 生成草稿")
                }
                CategoryChip(text = task.contentDirection, selected = false, onClick = {})
                StatusChip(text = task.status, selected = false, onClick = {})
                SourcePlatformChip(text = "关联素材 ${task.sourceCardIdList().size}")
            }
            Text(
                text = "下一步：${task.nextAction}",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            task.outline.lines().filter { it.isNotBlank() }.take(2).forEach { outlineLine ->
                Text(
                    text = "大纲：$outlineLine",
                    style = MaterialTheme.typography.bodySmall,
                    color = TertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            task.materialList.lines().filter { it.isNotBlank() }.take(2).forEach { material ->
                Text(
                    text = "素材：$material",
                    style = MaterialTheme.typography.bodySmall,
                    color = TertiaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatTime(task.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = SecondaryText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreationTaskDetailScreen(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    taskId: String,
    onBackClick: () -> Unit,
) {
    val task by creationTaskDao.observeById(taskId).collectAsState(initial = null)
    val topicCards by topicCardDao.observeAll().collectAsState(initial = emptyList())

    if (task == null) {
        MissingCreationTaskScreen(onBackClick = onBackClick)
        return
    }

    val currentTask = task ?: return
    val scope = rememberCoroutineScope()
    val sourceCardIds = currentTask.sourceCardIdList()
    val relatedCards = topicCards.filter { it.id in sourceCardIds }
    var selectedStatus by rememberSaveable(currentTask.id, currentTask.status) {
        mutableStateOf(currentTask.status)
    }
    var errorMessage by rememberSaveable(currentTask.id) { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentTask.generatedByAi) "AI 生成的创作任务草稿" else "任务详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CreamBackground,
                    titleContentColor = DeepNavy,
                    navigationIconContentColor = DeepNavy,
                ),
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DetailSection(title = "视频选题") {
                if (currentTask.generatedByAi) {
                    Text(
                        text = "该任务由收藏内容整理生成，用户可继续修改标题、脚本大纲和素材清单。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                    )
                }
                Text(
                    text = currentTask.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                )
                CategoryChip(text = currentTask.contentDirection, selected = false, onClick = {})
                Text(
                    text = "下一步行动：${currentTask.nextAction}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                )
            }

            DetailSection(title = "脚本大纲") {
                currentTask.outline.lines().filter { it.isNotBlank() }.forEach { outlineLine ->
                    Text(
                        text = outlineLine,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepNavy,
                    )
                }
            }

            DetailSection(title = "素材清单") {
                currentTask.materialList.lines().filter { it.isNotBlank() }.forEach { material ->
                    Text(
                        text = material,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            DetailSection(title = "关联灵感卡片") {
                if (relatedCards.isEmpty()) {
                    Text(
                        text = "未找到关联卡片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                    )
                } else {
                    relatedCards.forEach { card ->
                        RelatedTopicCard(topicCard = card)
                    }
                }
            }

            DetailSection(title = "任务状态") {
                TaskStatusSelector(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { newStatus ->
                        selectedStatus = newStatus
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    creationTaskDao.update(
                                        currentTask.copy(
                                            status = newStatus,
                                            updatedAt = System.currentTimeMillis(),
                                        ),
                                    )
                                }
                            }
                            if (result.isFailure) {
                                errorMessage = "状态更新失败"
                            }
                        }
                    },
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            content()
        }
    }
}

@Composable
private fun RelatedTopicCard(topicCard: TopicCardEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CreamBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = topicCard.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = topicCard.userNote,
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourcePlatformChip(text = topicCard.sourcePlatform)
            SourcePlatformChip(text = shareTypeLabel(topicCard.shareType))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CreationTaskStatuses.forEach { status ->
            StatusChip(
                text = status,
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingCreationTaskScreen(onBackClick: () -> Unit) {
    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(text = "任务详情") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "找不到这个创作任务。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TopicTag(text: String) {
    CategoryChip(text = text, selected = false, onClick = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewScreen(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    onBackClick: () -> Unit,
    onInboxClick: () -> Unit,
    onTaskGenerated: (String) -> Unit,
) {
    val since = remember { System.currentTimeMillis() - REVIEW_WINDOW_DAYS * ONE_DAY_MILLIS }
    val reviewCards by topicCardDao.observeReviewCandidates(since).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var reviewedIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var processedCount by rememberSaveable { mutableStateOf(0) }
    var plannedCount by rememberSaveable { mutableStateOf(0) }
    var droppedCount by rememberSaveable { mutableStateOf(0) }
    var isFinished by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val currentCard = reviewCards.firstOrNull { it.id !in reviewedIds }
    val reviewTotal = reviewedIds.size + reviewCards.count { it.id !in reviewedIds }

    LaunchedEffect(currentCard, reviewedIds, reviewCards) {
        if (!isFinished && currentCard == null && reviewedIds.isNotEmpty()) {
            isFinished = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "复盘") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            isFinished -> ReviewCompleteScreen(
                processedCount = processedCount,
                plannedCount = plannedCount,
                droppedCount = droppedCount,
                onInboxClick = onInboxClick,
                modifier = Modifier.padding(innerPadding),
            )

            currentCard == null -> EmptyReviewScreen(
                modifier = Modifier.padding(innerPadding),
            )

            else -> ReviewCardScreen(
                topicCard = currentCard,
                aiSuggestion = buildReviewAiRecommendation(currentCard),
                currentNumber = reviewedIds.size + 1,
                totalCount = reviewTotal,
                errorMessage = errorMessage,
                onGenerateTaskClick = {
                    scope.launch {
                        val result = runCatching {
                            val aiSuggestion = buildReviewAiRecommendation(currentCard)
                            val task = createCreationTaskFromCard(currentCard, aiSuggestion)
                            withContext(Dispatchers.IO) {
                                creationTaskDao.insert(task)
                                topicCardDao.update(
                                    currentCard.copy(
                                        status = "planned",
                                        aiTags = aiSuggestion.tags.joinToString(","),
                                        aiReusableStructure = aiSuggestion.reusableStructure,
                                        aiReferenceValue = aiSuggestion.referenceValue,
                                        aiReason = aiSuggestion.reason,
                                        aiConfidence = aiSuggestion.confidence,
                                        nextAction = aiSuggestion.nextAction,
                                        updatedAt = System.currentTimeMillis(),
                                    ),
                                )
                            }
                            task
                        }
                        if (result.isSuccess) {
                            // TODO metrics: track 用户复盘率, 收藏内容到创作任务转化率, and 从收藏到形成选题的平均时间 here.
                            // TODO metrics: track review_ai_recommendation_shown and card_to_task_conversion.
                            reviewedIds = reviewedIds + currentCard.id
                            processedCount += 1
                            plannedCount += 1
                            errorMessage = null
                            Toast.makeText(context, "已生成创作任务", Toast.LENGTH_SHORT).show()
                            onTaskGenerated(result.getOrThrow().id)
                        } else {
                            errorMessage = "生成任务失败，请稍后再试。"
                        }
                    }
                },
                onJoinExistingTaskClick = {
                    // P0 placeholder: joining cards to existing tasks needs a picker and sourceCardIds merge.
                    Toast.makeText(context, "下一版支持加入已有任务", Toast.LENGTH_SHORT).show()
                },
                onLaterClick = {
                    scope.launch {
                        val success = updateReviewStatus(topicCardDao, currentCard, "later")
                        if (success) {
                            // TODO metrics: track 用户复盘率 here.
                            reviewedIds = reviewedIds + currentCard.id
                            processedCount += 1
                            errorMessage = null
                        } else {
                            errorMessage = "更新失败，请稍后再试。"
                        }
                    }
                },
                onDropClick = {
                    scope.launch {
                        val success = updateReviewStatus(topicCardDao, currentCard, "dropped")
                        if (success) {
                            // TODO metrics: track 用户复盘率 here.
                            reviewedIds = reviewedIds + currentCard.id
                            processedCount += 1
                            droppedCount += 1
                            errorMessage = null
                        } else {
                            errorMessage = "更新失败，请稍后再试。"
                        }
                    }
                },
                onSkipClick = {
                    reviewedIds = reviewedIds + currentCard.id
                    processedCount += 1
                    errorMessage = null
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ReviewCardScreen(
    topicCard: TopicCardEntity,
    aiSuggestion: AiResponse,
    currentNumber: Int,
    totalCount: Int,
    errorMessage: String?,
    onGenerateTaskClick: () -> Unit,
    onJoinExistingTaskClick: () -> Unit,
    onLaterClick: () -> Unit,
    onDropClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceSummary = topicCard.sourceUrl
        ?: topicCard.sourceText?.take(SOURCE_PREVIEW_LENGTH)
        ?: topicCard.imageUri
        ?: "暂无来源"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "$currentNumber / $totalCount",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "这个能变成哪类视频任务？",
            fontSize = SectionTitleSize,
            fontWeight = FontWeight.Bold,
            color = DeepNavy,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = CardCream,
            ),
            shape = RoundedCornerShape(24.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssetCoverPreview(
                    topicCard = topicCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                )
                Text(
                    text = topicCard.title,
                    fontSize = CardTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                )
                TopicTag(text = topicCard.category)
                TopicTag(text = topicCard.sourcePlatform)
                Text(
                    text = topicCard.userNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepNavy,
                )
                Text(
                    text = sourceSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatTime(topicCard.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = AppColors.HoneyCream.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(22.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "AI 推荐：${aiSuggestion.contentType}",
                    fontSize = CardTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                )
                Text(
                    text = "理由：${aiSuggestion.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                )
                Text(
                    text = "建议：${if (aiSuggestion.shouldCreateTask) "生成创作任务" else "先补充启发后再判断"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepNavy,
                )
                Text(
                    text = "下一步：${aiSuggestion.nextAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TertiaryText,
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onGenerateTaskClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.DreamPurple,
                contentColor = Color.White,
            ),
        ) {
            Text(text = "生成创作任务")
        }
        OutlinedButton(
            onClick = onJoinExistingTaskClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "加入已有任务")
        }
        OutlinedButton(
            onClick = onLaterClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "以后再看")
        }
        OutlinedButton(
            onClick = onDropClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "放弃")
        }
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "跳过")
        }
    }
}

@Composable
private fun EmptyReviewScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "本周没有待复盘内容",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "先收集几条灵感，再回来筛选值得生成任务的选题。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewCompleteScreen(
    processedCount: Int,
    plannedCount: Int,
    droppedCount: Int,
    onInboxClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "复盘完成",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(18.dp))
        ReviewSummaryRow(label = "本次处理", count = processedCount)
        ReviewSummaryRow(label = "生成创作任务", count = plannedCount)
        ReviewSummaryRow(label = "放弃", count = droppedCount)
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "后续可由 AI 推荐最值得制作的 1–3 个选题。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onInboxClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "返回灵感收集箱")
        }
    }
}

@Composable
private fun ReviewSummaryRow(
    label: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private suspend fun updateReviewStatus(
    topicCardDao: TopicCardDao,
    topicCard: TopicCardEntity,
    status: String,
): Boolean {
    return runCatching {
        withContext(Dispatchers.IO) {
            topicCardDao.update(
                topicCard.copy(
                    status = status,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }.isSuccess
}

@Composable
private fun AiPlaceholderCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "AI 整理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "即将支持：自动识别选题用途、封面/标题参考、脚本结构和下一步创作动作。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // TODO metrics: track AI标签采纳率 when real AI tagging is connected.
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "暂不可用")
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    aiSuggestion: AiResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onAnalyzeClick: () -> Unit,
    onApplyClick: (AiResponse) -> Unit,
    onGenerateTaskClick: (AiResponse) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "AI 创作用途识别卡",
                fontSize = CardTitleSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Text(
                text = "当前使用本地 mock 建议。AI 会识别这条内容适合作为选题、标题、封面、脚本还是素材，并生成下一步行动；确认后才会应用。",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
            )
            if (aiSuggestion == null) {
                OutlinedButton(
                    onClick = onAnalyzeClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeepNavy,
                    ),
                ) {
                    Text(text = if (isLoading) "识别中..." else "AI 识别")
                }
            } else {
                AiStructuredResult(aiSuggestion = aiSuggestion)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { onApplyClick(aiSuggestion) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(text = "应用 AI 建议")
                    }
                    Button(
                        onClick = { onGenerateTaskClick(aiSuggestion) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.HoneyCream,
                            contentColor = DeepNavy,
                        ),
                    ) {
                        Text(text = "生成任务草稿")
                    }
                }
                TextButton(
                    onClick = onAnalyzeClick,
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(text = if (isLoading) "重新识别中..." else "重新识别")
                }
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AiStructuredResult(aiSuggestion: AiResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryChip(text = "推荐类型：${aiSuggestion.contentType}", selected = false, onClick = {})
            aiSuggestion.tags.take(3).forEach { tag ->
                SourcePlatformChip(text = "AI $tag")
            }
        }
        AiField(label = "可复用结构", value = aiSuggestion.reusableStructure)
        AiField(label = "参考价值", value = aiSuggestion.referenceValue)
        AiField(label = "下一步行动", value = aiSuggestion.nextAction)
        AiField(
            label = "是否建议生成任务",
            value = if (aiSuggestion.shouldCreateTask) "是，适合转成创作任务草稿" else "否，建议先作为素材继续观察",
        )
        AiField(label = "判断理由", value = aiSuggestion.reason)
        Text(
            text = "置信度 ${(aiSuggestion.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = TertiaryText,
        )
    }
}

@Composable
private fun AiField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TertiaryText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DeepNavy,
        )
    }
}

@Composable
private fun StoredAiInsightCard(
    topicCard: TopicCardEntity,
    linkedTask: CreationTaskEntity?,
) {
    val storedSuggestion = topicCard.toStoredAiResponse() ?: buildReviewAiRecommendation(topicCard)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.HoneyCream.copy(alpha = 0.46f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(SoftOutline),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "AI 已整理为创作资产",
                fontSize = CardTitleSize,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            AiStructuredResult(aiSuggestion = storedSuggestion)
            Text(
                text = if (linkedTask != null) "关联任务状态：已转为创作任务" else "关联任务状态：尚未生成任务草稿",
                style = MaterialTheme.typography.labelMedium,
                color = TertiaryText,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailScreen(
    topicCardDao: TopicCardDao,
    creationTaskDao: CreationTaskDao,
    cardId: String,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onTaskGenerated: (String) -> Unit,
) {
    val topicCard by topicCardDao.observeById(cardId).collectAsState(initial = null)
    val creationTasks by creationTaskDao.observeAll().collectAsState(initial = emptyList())

    if (topicCard == null) {
        MissingTopicCardScreen(onBackClick = onBackClick)
        return
    }

    val card = topicCard ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userNote by rememberSaveable(card.id) { mutableStateOf(card.userNote) }
    var selectedCategory by rememberSaveable(card.id) { mutableStateOf(card.category) }
    var selectedStatus by rememberSaveable(card.id) { mutableStateOf(card.status) }
    var errorMessage by rememberSaveable(card.id) { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable(card.id) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable(card.id) { mutableStateOf(false) }
    var detailImageUri by rememberSaveable(card.id) { mutableStateOf(card.imageUri) }
    var detailLocalImagePath by rememberSaveable(card.id) { mutableStateOf(card.localImagePath) }
    var detailCroppedImagePath by rememberSaveable(card.id) { mutableStateOf(card.croppedImagePath) }
    var selectedCoverAspectRatio by rememberSaveable(card.id) { mutableStateOf(card.coverAspectRatio) }
    var aiSuggestion by remember { mutableStateOf<AiResponse?>(null) }
    var appliedAiSuggestion by remember { mutableStateOf<AiResponse?>(null) }
    var isAiLoading by rememberSaveable(card.id) { mutableStateOf(false) }
    var aiErrorMessage by rememberSaveable(card.id) { mutableStateOf<String?>(null) }
    val aiRepository = remember { AiRepository() }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            tryTakePersistableImagePermission(context, uri.toString())
            val oldLocalPath = detailLocalImagePath
            val oldCroppedPath = detailCroppedImagePath
            detailImageUri = uri.toString()
            detailLocalImagePath = null
            detailCroppedImagePath = null
            errorMessage = null
            scope.launch {
                val localPath = withContext(Dispatchers.IO) {
                    ImageStorageHelper.copyImageToPrivateStorage(context, uri)
                }
                if (detailImageUri != uri.toString()) {
                    ImageStorageHelper.deleteLocalImageIfExists(localPath)
                } else if (localPath != null) {
                    ImageStorageHelper.deleteLocalImageIfExists(oldLocalPath)
                    ImageStorageHelper.deleteLocalImageIfExists(oldCroppedPath)
                    detailLocalImagePath = localPath
                } else {
                    errorMessage = "图片复制失败，将尝试使用原始图片来源"
                }
            }
        }
    }
    val displayCard = card.copy(
        imageUri = detailImageUri,
        localImagePath = detailLocalImagePath,
        croppedImagePath = detailCroppedImagePath,
        coverAspectRatio = selectedCoverAspectRatio,
    )
    val existingTask = creationTasks.firstOrNull { it.hasSourceCard(card.id) }

    fun runAiAnalysis() {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isBlank()) {
            aiErrorMessage = "建议先补一句创作启发，结果会更准确。"
            return
        }
        isAiLoading = true
        aiErrorMessage = null
        // TODO metrics: track ai_recognition_clicked.
        scope.launch {
            val result = aiRepository.analyze(
                AiRequest(
                    sourceUrl = card.sourceUrl,
                    sourceText = card.sourceText,
                    sourcePlatform = card.sourcePlatform,
                    sourceDomain = card.sourceDomain,
                    userNote = trimmedNote,
                    imageSummary = if (displayCard.coverImageModel() != null) "这张卡片有封面图片" else null,
                    currentCategory = selectedCategory,
                    previewTitle = card.previewTitle,
                    previewDescription = card.previewDescription,
                    taskMode = AiTaskMode.CLASSIFY_CONTENT,
                ),
            )
            result
                .onSuccess { response ->
                    aiSuggestion = response
                    aiErrorMessage = null
                    // TODO metrics: track ai_suggestion_generated.
                }
                .onFailure { error ->
                    aiErrorMessage = error.message ?: "AI 识别失败，不影响保存。"
                }
            isAiLoading = false
        }
    }

    fun applyAiSuggestion(response: AiResponse) {
        selectedCategory = response.contentType
        aiSuggestion = response
        appliedAiSuggestion = response
        aiErrorMessage = null
        // TODO metrics: track ai_suggestion_applied and ai_tag_acceptance.
    }

    fun cropDetailCover() {
        if (detailImageUri.isNullOrBlank() && detailLocalImagePath.isNullOrBlank()) {
            errorMessage = "请先添加截图/图片。"
            return
        }
        scope.launch {
            if (!detailImageUri.isNullOrBlank() && detailLocalImagePath.isNullOrBlank()) {
                val localPath = withContext(Dispatchers.IO) {
                    ImageStorageHelper.copyImageToPrivateStorage(context, Uri.parse(detailImageUri))
                }
                if (localPath != null) {
                    detailLocalImagePath = localPath
                }
            }
            val croppedPath = withContext(Dispatchers.IO) {
                ImageStorageHelper.createCroppedCover(
                    context = context,
                    localImagePath = detailLocalImagePath,
                    imageUri = detailImageUri,
                    aspectRatio = selectedCoverAspectRatio,
                )
            }
            if (croppedPath != null) {
                ImageStorageHelper.deleteLocalImageIfExists(detailCroppedImagePath)
                detailCroppedImagePath = croppedPath
                Toast.makeText(context, "已裁剪为 $selectedCoverAspectRatio 封面", Toast.LENGTH_SHORT).show()
            } else {
                errorMessage = "裁剪失败，请换一张图片再试。"
            }
        }
    }

    fun generateTaskFromDetail(taskSuggestion: AiResponse? = appliedAiSuggestion) {
        val trimmedNote = userNote.trim()
        if (trimmedNote.isBlank()) {
            errorMessage = "选题灵感不能为空。"
            return
        }
        if (existingTask != null) {
            onTaskGenerated(existingTask.id)
            return
        }

        isSaving = true
        errorMessage = null
        scope.launch {
            if (!detailImageUri.isNullOrBlank() && detailLocalImagePath.isNullOrBlank()) {
                val localPath = withContext(Dispatchers.IO) {
                    ImageStorageHelper.copyImageToPrivateStorage(context, Uri.parse(detailImageUri))
                }
                if (localPath != null) {
                    detailLocalImagePath = localPath
                } else {
                    errorMessage = "图片复制失败，将尝试使用原始图片来源"
                }
            }
            val updatedCard = card.copy(
                title = trimmedNote.take(20),
                userNote = trimmedNote,
                category = selectedCategory,
                status = "planned",
                imageUri = detailImageUri,
                localImagePath = detailLocalImagePath,
                croppedImagePath = detailCroppedImagePath,
                coverAspectRatio = selectedCoverAspectRatio,
                aiTags = taskSuggestion?.tags?.joinToString(",") ?: card.aiTags,
                aiReusableStructure = taskSuggestion?.reusableStructure ?: card.aiReusableStructure,
                aiReferenceValue = taskSuggestion?.referenceValue ?: card.aiReferenceValue,
                aiReason = taskSuggestion?.reason ?: card.aiReason,
                aiConfidence = taskSuggestion?.confidence ?: card.aiConfidence,
                nextAction = taskSuggestion?.nextAction ?: card.nextAction,
                shareType = when {
                    !card.sourceUrl.isNullOrBlank() && (!detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank()) -> "mixed"
                    !detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank() -> "image"
                    else -> card.shareType
                },
                updatedAt = System.currentTimeMillis(),
            )
            val task = createCreationTaskFromCard(updatedCard, taskSuggestion)

            try {
                withContext(Dispatchers.IO) {
                    topicCardDao.update(updatedCard)
                    creationTaskDao.insert(task)
                }
                // TODO metrics: track ai_task_draft_created and card_to_task_conversion.
                Toast.makeText(context, "已生成创作任务", Toast.LENGTH_SHORT).show()
                onTaskGenerated(task.id)
            } catch (error: Exception) {
                errorMessage = "生成任务失败：${error.message ?: "未知错误"}"
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = card.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (!card.sourceUrl.isNullOrBlank()) {
                LinkPreviewCard(
                    topicCard = displayCard,
                    compact = false,
                    onClick = {
                        OpenSourceHelper.openSource(
                            context = context,
                            sourceUrl = card.sourceUrl,
                            sourcePlatform = card.sourcePlatform,
                            sourceDomain = card.sourceDomain,
                        ) { message ->
                            errorMessage = message
                        }
                    },
                )
            } else {
                TopicSourceCard(
                    sourcePlatform = card.sourcePlatform,
                    sourceUrl = card.sourceUrl,
                    sourceText = card.sourceText,
                    imageUri = detailImageUri,
                    localImagePath = detailLocalImagePath,
                    croppedImagePath = detailCroppedImagePath,
                    coverAspectRatio = selectedCoverAspectRatio,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = if (detailImageUri.isNullOrBlank() && detailLocalImagePath.isNullOrBlank()) "添加截图/图片" else "替换图片")
                }
                if (!detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            ImageStorageHelper.deleteLocalImageIfExists(detailLocalImagePath)
                            ImageStorageHelper.deleteLocalImageIfExists(detailCroppedImagePath)
                            detailImageUri = null
                            detailLocalImagePath = null
                            detailCroppedImagePath = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Coral,
                        ),
                    ) {
                        Text(text = "移除图片")
                    }
                }
            }

            if (!detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank()) {
                Text(
                    text = "封面比例",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepNavy,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoverAspectRatios.forEach { ratio ->
                        StatusChip(
                            text = ratio,
                            selected = selectedCoverAspectRatio == ratio,
                            onClick = { selectedCoverAspectRatio = ratio },
                        )
                    }
                }
                OutlinedButton(
                    onClick = { cropDetailCover() },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DeepNavy,
                    ),
                ) {
                    Text(text = "裁剪封面")
                }
            }

            if (!card.sourceUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        OpenSourceHelper.openSource(
                            context = context,
                            sourceUrl = card.sourceUrl,
                            sourcePlatform = card.sourcePlatform,
                            sourceDomain = card.sourceDomain,
                        ) { message ->
                            errorMessage = message
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "打开来源")
                }
            }

            StoredAiInsightCard(
                topicCard = card,
                linkedTask = existingTask,
            )

            AiSuggestionCard(
                aiSuggestion = aiSuggestion,
                isLoading = isAiLoading,
                errorMessage = aiErrorMessage,
                onAnalyzeClick = ::runAiAnalysis,
                onApplyClick = { response -> applyAiSuggestion(response) },
                onGenerateTaskClick = { response ->
                    applyAiSuggestion(response)
                    generateTaskFromDetail(response)
                },
            )

            OutlinedButton(
                onClick = { generateTaskFromDetail() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DeepNavy,
                ),
            ) {
                Text(text = if (existingTask == null) "生成创作任务" else "查看已生成任务")
            }

            OutlinedTextField(
                value = userNote,
                onValueChange = {
                    userNote = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "选题灵感") },
                minLines = 4,
            )

            Text(
                text = "分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CategorySelector(
                categories = Categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
            )

            Text(
                text = "状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            StatusSelector(
                statusOptions = StatusOptions,
                selectedStatus = selectedStatus,
                onStatusSelected = { selectedStatus = it },
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = {
                    val trimmedNote = userNote.trim()
                    if (trimmedNote.isBlank()) {
                        errorMessage = "选题灵感不能为空。"
                        return@Button
                    }

                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        if (!detailImageUri.isNullOrBlank() && detailLocalImagePath.isNullOrBlank()) {
                            val localPath = withContext(Dispatchers.IO) {
                                ImageStorageHelper.copyImageToPrivateStorage(context, Uri.parse(detailImageUri))
                            }
                            if (localPath != null) {
                                detailLocalImagePath = localPath
                            } else {
                                errorMessage = "图片复制失败，将尝试使用原始图片来源"
                            }
                        }
                        val updatedCard = card.copy(
                            title = trimmedNote.take(20),
                            userNote = trimmedNote,
                            category = selectedCategory,
                            status = selectedStatus,
                            imageUri = detailImageUri,
                            localImagePath = detailLocalImagePath,
                            croppedImagePath = detailCroppedImagePath,
                            coverAspectRatio = selectedCoverAspectRatio,
                            aiTags = appliedAiSuggestion?.tags?.joinToString(",") ?: card.aiTags,
                            aiReusableStructure = appliedAiSuggestion?.reusableStructure ?: card.aiReusableStructure,
                            aiReferenceValue = appliedAiSuggestion?.referenceValue ?: card.aiReferenceValue,
                            aiReason = appliedAiSuggestion?.reason ?: card.aiReason,
                            aiConfidence = appliedAiSuggestion?.confidence ?: card.aiConfidence,
                            nextAction = appliedAiSuggestion?.nextAction ?: card.nextAction,
                            shareType = when {
                                !card.sourceUrl.isNullOrBlank() && (!detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank()) -> "mixed"
                                !detailImageUri.isNullOrBlank() || !detailLocalImagePath.isNullOrBlank() -> "image"
                                else -> card.shareType
                            },
                            updatedAt = System.currentTimeMillis(),
                        )

                        try {
                            withContext(Dispatchers.IO) {
                                topicCardDao.update(updatedCard)
                            }
                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                            onSaved()
                        } catch (error: Exception) {
                            errorMessage = "保存失败：${error.message ?: "未知错误"}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isSaving) "保存中..." else "保存")
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Coral,
                ),
            ) {
                Text(text = "删除此卡片")
            }
        }
    }

    DeleteTopicCardDialog(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            scope.launch {
                val result = runCatching {
                    deleteTopicCard(
                        topicCardDao = topicCardDao,
                        cardId = card.id,
                        localImagePath = card.localImagePath,
                        croppedImagePath = card.croppedImagePath,
                    )
                }
                if (result.isSuccess) {
                    onSaved()
                } else {
                    errorMessage = "删除失败"
                }
            }
        },
    )
}

@Composable
private fun TopicSourceCard(
    sourcePlatform: String,
    sourceUrl: String?,
    sourceText: String?,
    imageUri: String?,
    localImagePath: String?,
    croppedImagePath: String?,
    coverAspectRatio: String,
) {
    val imageModel = croppedImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: localImagePath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: imageUri?.takeIf { it.isNotBlank() }
    val imageBadge = when {
        !croppedImagePath.isNullOrBlank() -> "已裁剪封面"
        !localImagePath.isNullOrBlank() || !imageUri.isNullOrBlank() -> "用户图片"
        else -> "链接预览"
    }
    val sourceDisplay = when {
        !sourceUrl.isNullOrBlank() -> sourceUrl
        !sourceText.isNullOrBlank() -> sourceText.take(SOURCE_PREVIEW_LENGTH)
        !imageUri.isNullOrBlank() -> "截图来源"
        else -> "暂无来源"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardCream,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "来源：$sourcePlatform",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = sourceDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (imageModel != null) {
                PreviewCoverImage(
                    imageModel = imageModel,
                    badge = imageBadge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(coverPreviewHeight(coverAspectRatio, wide = true)),
                    cornerRadius = 20.dp,
                )
                Text(
                    text = "图片 URI：$imageUri",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSelector(
    statusOptions: List<StatusOption>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        statusOptions.forEach { option ->
            StatusChip(
                text = option.label,
                selected = selectedStatus == option.value,
                onClick = { onStatusSelected(option.value) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingTopicCardScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "卡片详情") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "找不到这张卡片。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun readClipboardShare(context: Context): ShareContent? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        .orEmpty()
    return ShareContentParser.parseClipboardText(text)
}

private fun ShareContent.withPickedImage(
    imageUri: String,
    localImagePath: String?,
): ShareContent {
    val hasSourceUrl = !sourceUrl.isNullOrBlank()
    return copy(
        imageUri = imageUri,
        localImagePath = localImagePath ?: this.localImagePath,
        sourcePlatform = if (hasSourceUrl) sourcePlatform else "截图来源",
        shareType = if (hasSourceUrl) "mixed" else "image",
    )
}

private suspend fun ShareContent.ensureLocalImageCopy(
    context: Context,
    onCopyFailed: (String) -> Unit,
): ShareContent {
    if (imageUri.isNullOrBlank() || !localImagePath.isNullOrBlank()) {
        return this
    }

    val copiedPath = withContext(Dispatchers.IO) {
        ImageStorageHelper.copyImageToPrivateStorage(context, Uri.parse(imageUri))
    }
    return if (copiedPath != null) {
        copy(localImagePath = copiedPath)
    } else {
        onCopyFailed("图片复制失败，将尝试使用原始图片来源")
        this
    }
}

private fun tryTakePersistableImagePermission(
    context: Context,
    imageUri: String,
) {
    // Some shared image URIs are temporary and cannot be persisted; ignore failures and show a fallback.
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            Uri.parse(imageUri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}

private fun ShareContent.withoutPickedImage(): ShareContent {
    val hasSourceUrl = !sourceUrl.isNullOrBlank()
    val hasRawText = !rawText.isNullOrBlank()
    ImageStorageHelper.deleteLocalImageIfExists(localImagePath)
    return copy(
        imageUri = null,
        localImagePath = null,
        sourcePlatform = if (!hasSourceUrl && sourcePlatform == "截图来源") {
            "手动记录"
        } else {
            sourcePlatform
        },
        shareType = when {
            hasSourceUrl -> "link"
            hasRawText -> "text"
            else -> "unknown"
        },
    )
}

private fun ShareContent.normalizedShareType(): String {
    return when {
        !sourceUrl.isNullOrBlank() && (!imageUri.isNullOrBlank() || !localImagePath.isNullOrBlank()) -> "mixed"
        !imageUri.isNullOrBlank() || !localImagePath.isNullOrBlank() -> "image"
        !sourceUrl.isNullOrBlank() -> "link"
        !rawText.isNullOrBlank() -> "text"
        else -> shareType
    }
}

private suspend fun saveTopicCard(
    topicCardDao: TopicCardDao,
    topicCard: TopicCardEntity,
) {
    withContext(Dispatchers.IO) {
        topicCardDao.insert(topicCard)
    }
}

private fun TopicCardEntity.toStoredAiResponse(): AiResponse? {
    val tags = aiTags
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    if (tags.isEmpty() && aiReusableStructure.isNullOrBlank() && aiReferenceValue.isNullOrBlank() && nextAction.isNullOrBlank()) {
        return null
    }
    return AiResponse(
        contentType = category,
        tags = tags.ifEmpty { listOf("创作灵感", "待确认") }.take(3),
        reusableStructure = aiReusableStructure ?: "从收藏内容中提取一个可复用创作角度",
        referenceValue = aiReferenceValue ?: "需要用户补充具体创作意图",
        nextAction = nextAction ?: "先补一句这条内容启发你做什么视频",
        shouldCreateTask = status == "planned",
        confidence = aiConfidence ?: 0.74f,
        reason = aiReason ?: "来自已保存 AI 建议",
        taskDraft = null,
    )
}

private fun buildReviewAiRecommendation(topicCard: TopicCardEntity): AiResponse {
    topicCard.toStoredAiResponse()?.let { return it }

    val signal = listOfNotNull(
        topicCard.userNote,
        topicCard.title,
        topicCard.previewTitle,
        topicCard.previewDescription,
        topicCard.sourceText,
        topicCard.category,
    ).joinToString(" ").lowercase()

    val contentType = when {
        signal.contains("封面") || signal.contains("截图") || signal.contains("视觉") -> "封面参考"
        signal.contains("标题") || signal.contains("爆款") || signal.contains("选题") -> "标题参考"
        signal.contains("教程") || signal.contains("方法") || signal.contains("步骤") || signal.contains("结构") -> "脚本结构"
        signal.contains("reaction") || signal.contains("案例") || signal.contains("素材") -> "素材案例"
        else -> topicCard.category.takeIf { it.isNotBlank() } ?: "选题灵感"
    }
    val shouldCreateTask = contentType != "封面参考" || topicCard.userNote.length >= 8
    val reusableStructure = when (contentType) {
        "标题参考" -> "强问题 + 明确对象 + 结果反差"
        "封面参考" -> "大字标题 + 主体图像 + 低饱和背景"
        "脚本结构" -> "问题引入 → 步骤演示 → 注意事项 → 总结"
        "素材案例" -> "案例现象 → 可引用观点 → 我的补充"
        else -> "收藏内容 → 创作角度 → 下一步行动"
    }
    return AiResponse(
        contentType = contentType,
        tags = when (contentType) {
            "标题参考" -> listOf("标题结构", "选题角度", "爆款案例")
            "封面参考" -> listOf("封面结构", "视觉参考", "点击率")
            "脚本结构" -> listOf("教程结构", "步骤拆解", "方法论")
            "素材案例" -> listOf("素材参考", "案例积累", "观点补充")
            else -> listOf("创作灵感", "素材参考", "待完善")
        },
        reusableStructure = reusableStructure,
        referenceValue = when (contentType) {
            "封面参考" -> "适合作为视频封面排版参考"
            "素材案例" -> "适合作为视频论据或案例"
            else -> "适合提炼成视频任务草稿"
        },
        nextAction = when (contentType) {
            "标题参考" -> "先改写 3 个适合自己账号的标题"
            "封面参考" -> "先标注这个封面里可复用的布局"
            "脚本结构" -> "先列出可复用的 4 个步骤"
            "素材案例" -> "先记录这条素材能支撑哪个观点"
            else -> "先补一句这条内容启发你做什么视频"
        },
        shouldCreateTask = shouldCreateTask,
        confidence = 0.78f,
        reason = if (shouldCreateTask) "适合转为任务草稿" else "更适合先做素材",
        taskDraft = if (shouldCreateTask) {
            CreationTaskDraft(
                taskTitle = topicCard.userNote.take(25).ifBlank { topicCard.title },
                contentDirection = contentType,
                outline = listOf("开场：说明问题", "案例：引用收藏内容", "观点：加入自己的判断", "行动：给出下一步"),
                materialList = buildMaterialList(topicCard).lines(),
                nextAction = "先写出视频前 30 秒脚本",
            )
        } else {
            null
        },
    )
}

private fun createCreationTaskFromCard(
    topicCard: TopicCardEntity,
    aiResponse: AiResponse? = null,
): CreationTaskEntity {
    val now = System.currentTimeMillis()
    val titleDraft = aiResponse?.taskTitle
        ?: topicCard.userNote
        .takeIf { it.isNotBlank() }
        ?.take(36)
        ?: topicCard.title
    return CreationTaskEntity(
        id = UUID.randomUUID().toString(),
        title = titleDraft,
        sourceCardIds = topicCard.id,
        contentDirection = aiResponse?.contentDirection ?: topicCard.category,
        outline = (aiResponse?.outline ?: listOf(
            "1. 开场痛点",
            "2. 参考案例",
            "3. 我的观点/演示",
            "4. 结论与行动建议",
        )).joinToString(separator = "\n"),
        materialList = aiResponse?.materialList?.joinToString(separator = "\n")
            ?: buildMaterialList(topicCard),
        nextAction = aiResponse?.taskDraft?.nextAction ?: aiResponse?.nextAction ?: topicCard.nextAction ?: "先补充脚本大纲",
        generatedByAi = aiResponse != null,
        status = DEFAULT_TASK_STATUS,
        createdAt = now,
        updatedAt = now,
    )
}

private fun buildMaterialList(topicCard: TopicCardEntity): String {
    val materials = buildList {
        topicCard.sourceUrl?.takeIf { it.isNotBlank() }?.let { add("来源链接：$it") }
        topicCard.croppedImagePath?.takeIf { it.isNotBlank() }?.let { add("裁剪封面：$it") }
        topicCard.imageUri?.takeIf { it.isNotBlank() }?.let { add("图片 URI：$it") }
        topicCard.localImagePath?.takeIf { it.isNotBlank() }?.let { add("本地图片：$it") }
        topicCard.sourceText?.takeIf { it.isNotBlank() }?.let { add("来源文字：${it.take(SOURCE_PREVIEW_LENGTH)}") }
    }
    return materials.ifEmpty { listOf("暂无素材，先补充来源或截图") }
        .joinToString(separator = "\n")
}

private fun CreationTaskEntity.sourceCardIdList(): List<String> {
    return sourceCardIds
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun CreationTaskEntity.hasSourceCard(cardId: String): Boolean {
    return sourceCardIdList().contains(cardId)
}

private suspend fun deleteTopicCard(
    topicCardDao: TopicCardDao,
    cardId: String,
    localImagePath: String?,
    croppedImagePath: String?,
) {
    withContext(Dispatchers.IO) {
        topicCardDao.deleteById(cardId)
        ImageStorageHelper.deleteLocalImageIfExists(localImagePath)
        ImageStorageHelper.deleteLocalImageIfExists(croppedImagePath)
    }
}

private suspend fun fetchAndStoreLinkPreview(
    topicCardDao: TopicCardDao,
    topicCard: TopicCardEntity,
) {
    val sourceUrl = topicCard.sourceUrl ?: return
    val result = LinkPreviewFetcher.fetch(sourceUrl)
    withContext(Dispatchers.IO) {
        topicCardDao.updateLinkPreview(
            cardId = topicCard.id,
            previewTitle = result.previewTitle,
            previewDescription = result.previewDescription,
            previewImageUrl = result.previewImageUrl,
            previewFetchedAt = result.previewFetchedAt,
            previewStatus = result.previewStatus,
            updatedAt = System.currentTimeMillis(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    placeholder: String,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatRelativeDate(timestamp: Long): String {
    val target = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }

    return when {
        target.isSameDay(today) -> "今天"
        target.isSameDay(yesterday) -> "昨天"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun Calendar.isSameDay(other: Calendar): Boolean {
    return get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun extractDomain(sourceUrl: String): String {
    val host = runCatching { URI(sourceUrl).host }.getOrNull()
    return host
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: sourceUrl
}

private fun statusLabel(status: String): String {
    return StatusOptions.firstOrNull { it.value == status }?.label ?: status
}

private fun shareTypeLabel(shareType: String): String {
    return when (shareType) {
        "link" -> "链接"
        "image" -> "截图/图片"
        "mixed" -> "链接+图片"
        "text" -> "文字"
        else -> "内容"
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DontJustSaveTheme {
        HomeEmptyGuide()
    }
}
