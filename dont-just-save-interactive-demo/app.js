const STORAGE_KEY = "dontJustSaveInteractiveDemo";

const categories = ["选题灵感", "标题参考", "封面参考", "脚本结构", "素材案例", "表达方式", "待判断"];
const statuses = [
  { label: "全部", value: "全部" },
  { label: "未生成任务", value: "inbox" },
  { label: "已转任务", value: "planned" },
  { label: "以后再看", value: "later" },
  { label: "放弃", value: "dropped" },
];

const defaultState = {
  screen: "home",
  previousScreen: "home",
  selectedCardId: null,
  selectedTaskId: null,
  categoryFilter: "全部",
  statusFilter: "全部",
  reviewIndex: 0,
  openMenuId: null,
  batchSuggestions: [],
  capture: {
    sourceUrl: "",
    sourcePlatform: "手动记录",
    sourceDomain: "",
    sourcePreview: "手动记录",
    sourceTitle: "",
    sourceDescription: "",
    sourceType: "unknown",
    userNote: "",
    contentType: "选题灵感",
    coverType: "gradient",
    coverGradient: "topic",
    aiResult: null,
    aiApplied: false,
  },
  cards: [
    {
      id: "seed-reaction",
      title: "reaction 视频",
      sourceTitle: "一个创作者拆解爆款 reaction 视频结构",
      sourceDescription: "用开场钩子、片段反应和观点总结组成可拆解的视频案例。",
      sourcePlatform: "哔哩哔哩",
      sourceDomain: "b23.tv",
      sourceUrl: "https://b23.tv/demo",
      resolvedUrl: "https://www.bilibili.com/video/demo",
      sourceAuthor: "示例 UP 主",
      sourceType: "video",
      contentType: "素材案例",
      tags: ["reaction", "案例", "视频结构"],
      userNote: "可以做一期 reaction 视频拆解",
      reusableStructure: "开场钩子 → 内容片段 → 观点反应 → 总结",
      referenceValue: "开头结构可拆解",
      nextAction: "先拆解这个视频的开头结构",
      reason: "包含可拆解的视频案例",
      confidence: 0.88,
      aiClassificationStatus: "not_started",
      status: "inbox",
      coverType: "link",
      coverGradient: "material",
      createdAt: Date.now() - 1000 * 60 * 60 * 8,
      taskCreated: false,
    },
  ],
  tasks: [],
};

let state = loadState();

const app = document.getElementById("app");
const toast = document.getElementById("toast");
const outputModal = document.getElementById("outputModal");
const modalBody = document.getElementById("modalBody");
const backToTop = document.getElementById("backToTop");

function loadState() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return structuredClone(defaultState);
  try {
    return { ...structuredClone(defaultState), ...JSON.parse(raw) };
  } catch {
    return structuredClone(defaultState);
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function setScreen(screen, options = {}) {
  state.previousScreen = state.screen;
  state.screen = screen;
  Object.assign(state, options);
  state.openMenuId = null;
  saveAndRender();
}

function saveAndRender() {
  saveState();
  render();
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 1800);
}

function setupPortfolioInteractions() {
  document.querySelectorAll("[data-output-card]").forEach((card) => {
    card.addEventListener("click", () => openOutputModal(card));
  });
  document.querySelectorAll("[data-close-modal]").forEach((element) => {
    element.addEventListener("click", closeOutputModal);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeOutputModal();
  });
  backToTop?.addEventListener("click", () => {
    document.getElementById("top")?.scrollIntoView({ behavior: "smooth" });
  });
  window.addEventListener("scroll", () => {
    backToTop?.classList.toggle("show", window.scrollY > 520);
  });
}

function openOutputModal(card) {
  if (!outputModal || !modalBody) return;
  const visual = card.querySelector(".output-visual")?.cloneNode(true);
  const title = card.querySelector("h3, h4")?.textContent || "产品设计产出";
  const description = card.querySelector("p")?.textContent || "";
  modalBody.innerHTML = "";
  const heading = document.createElement("h3");
  heading.id = "modalTitle";
  heading.textContent = title;
  const text = document.createElement("p");
  text.textContent = description;
  modalBody.append(heading, text);
  if (visual) modalBody.append(visual);
  outputModal.classList.add("open");
  outputModal.setAttribute("aria-hidden", "false");
}

function closeOutputModal() {
  if (!outputModal) return;
  outputModal.classList.remove("open");
  outputModal.setAttribute("aria-hidden", "true");
}

function id(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function chipClass(type) {
  const map = {
    选题灵感: "type-topic",
    标题参考: "type-title",
    封面参考: "type-cover",
    脚本结构: "type-script",
    素材案例: "type-material",
    表达方式: "type-expression",
  };
  return map[type] || "type-topic";
}

function statusLabel(value) {
  return {
    inbox: "未生成任务",
    planned: "已转任务",
    later: "以后再看",
    dropped: "放弃",
    done: "已完成",
  }[value] || value;
}

function sourceTypeLabel(value) {
  return {
    video: "视频",
    article: "图文/文章",
    image: "图片",
    note: "笔记",
    unknown: "未知类型",
  }[value] || "未知类型";
}

function formatDate(timestamp) {
  const date = new Date(timestamp);
  return `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function cardTitleFromNote(note) {
  const clean = note.trim();
  return clean ? clean.slice(0, 18) : "新的视频灵感";
}

function displaySourceTitle(card) {
  return card.sourceTitle || card.title || card.userNote || card.sourceDomain || "未命名收藏";
}

function createMockAiResult(source = state.capture) {
  const signal = [
    source.userNote,
    source.sourceTitle,
    source.sourceDescription,
    source.sourcePreview,
    source.sourcePlatform,
    source.sourceDomain,
    source.sourceType,
    source.contentType,
  ].filter(Boolean).join(" ").toLowerCase();
  const materialList = [
    source.sourceUrl ? `原链接：${source.sourceUrl}` : "补充原内容链接或截图",
    `参考灵感：${source.userNote || source.title || "待补充"}`,
  ];

  if (hasAny(signal, ["reaction", "反应", "评论", "歌手", "视频", "综艺", "看完"])) {
    return {
      contentType: "素材案例",
      tags: ["reaction", "视频结构", "案例拆解"],
      reusableStructure: "开场钩子 → 内容片段 → 观点反应 → 总结",
      referenceValue: "适合拆解 reaction 视频节奏设计",
      nextAction: "先拆出这个视频的前 30 秒结构",
      shouldCreateTask: true,
      confidence: 0.88,
      reason: "包含可拆解的视频案例",
      taskDraft: {
        taskTitle: "拆解一个 reaction 视频为什么吸引人",
        contentDirection: "案例拆解",
        outline: ["为什么 reaction 视频容易吸引点击", "拆解原视频结构", "总结可复用技巧", "给普通创作者行动建议"],
        materialList: [...materialList, "标题结构", "封面截图", "评论区反馈"],
        nextAction: "先写出 3 个标题版本",
      },
    };
  }

  if (hasAny(signal, ["标题", "爆款", "选题", "热点", "话题", "流量"])) {
    return {
      contentType: signal.includes("选题") ? "选题灵感" : "标题参考",
      tags: ["标题结构", "选题角度", "爆款案例"],
      reusableStructure: "强问题 + 明确对象 + 结果反差",
      referenceValue: "适合提炼成自己的视频选题",
      nextAction: "先改写 3 个适合自己账号的标题",
      shouldCreateTask: true,
      confidence: 0.84,
      reason: "标题和选题信号明确",
      taskDraft: {
        taskTitle: source.userNote?.slice(0, 25) || "把爆款标题改成视频选题",
        contentDirection: "选题策划",
        outline: ["原标题为什么有效", "适配自己的受众", "改写 3 个标题", "选择一个进入脚本"],
        materialList,
        nextAction: "先写出 3 个标题版本",
      },
    };
  }

  if (hasAny(signal, ["封面", "截图", "配色", "字体", "排版", "视觉", "海报"])) {
    return {
      contentType: "封面参考",
      tags: ["封面结构", "视觉参考", "点击率"],
      reusableStructure: "大字标题 + 主体图像 + 低饱和背景",
      referenceValue: "适合作为视频封面排版参考",
      nextAction: "先标注这个封面里可复用的布局",
      shouldCreateTask: Boolean(source.userNote && source.userNote.length >= 8),
      confidence: 0.8,
      reason: "视觉和封面线索突出",
      taskDraft: {
        taskTitle: source.userNote?.slice(0, 25) || "整理一组可复用封面样式",
        contentDirection: "封面参考",
        outline: ["观察封面层级", "标注标题和主体", "复用到自己的选题", "生成封面草图"],
        materialList,
        nextAction: "先标注 3 个可复用布局点",
      },
    };
  }

  if (hasAny(signal, ["教程", "步骤", "方法", "怎么做", "教你", "指南", "流程", "脚本", "结构"])) {
    return {
      contentType: "脚本结构",
      tags: ["教程结构", "步骤拆解", "方法论"],
      reusableStructure: "问题引入 → 步骤演示 → 注意事项 → 总结",
      referenceValue: "适合复用为教程类视频脚本结构",
      nextAction: "先列出可复用的 4 个步骤",
      shouldCreateTask: true,
      confidence: 0.86,
      reason: "包含方法或流程线索",
      taskDraft: {
        taskTitle: source.userNote?.slice(0, 25) || "拆出一个教程视频脚本",
        contentDirection: "教程方法",
        outline: ["开场提出问题", "演示关键步骤", "指出常见错误", "总结行动清单"],
        materialList,
        nextAction: "先写出教程前 30 秒脚本",
      },
    };
  }

  return {
    contentType: source.contentType || "选题灵感",
    tags: ["创作灵感", "素材参考", "待完善"],
    reusableStructure: "从收藏内容中提取一个可复用创作角度",
    referenceValue: "需要用户补充具体创作意图",
    nextAction: "先补一句这条内容启发你做什么视频",
    shouldCreateTask: false,
    confidence: 0.72,
    reason: "信息不足需确认",
    taskDraft: null,
  };
}

function classifyCardForCreativeUse(card) {
  const sourceTitle = displaySourceTitle(card);
  const signal = [
    card.userNote,
    card.sourceTitle,
    card.sourceDescription,
    card.sourceAuthor,
    card.sourcePlatform,
    card.sourceDomain,
    card.sourceType,
    card.sourceUrl,
    card.resolvedUrl,
    card.title,
  ].filter(Boolean).join(" ").toLowerCase();
  let suggestion;

  if (hasAny(signal, ["reaction", "反应", "评论", "歌手", "视频", "综艺", "看完"])) {
    suggestion = {
      suggestedCategory: "素材案例",
      suggestedTags: ["reaction", "视频结构", "案例拆解"],
      suggestedNextAction: "先拆出这个视频的前 30 秒结构",
      reusableStructure: "开场钩子 → 片段反应 → 观点总结",
      referenceValue: "可拆解视频结构",
      reason: "包含可拆解的视频案例",
      confidence: 0.88,
    };
  } else if (hasAny(signal, ["标题", "爆款", "热点", "选题", "话题", "流量"])) {
    suggestion = {
      suggestedCategory: signal.includes("选题") ? "选题灵感" : "标题参考",
      suggestedTags: ["标题结构", "选题角度", "爆款案例"],
      suggestedNextAction: "先改写 3 个适合自己账号的标题",
      reusableStructure: "强问题 + 明确对象 + 结果反差",
      referenceValue: "可转成标题结构",
      reason: "更适合作为选题或标题结构参考",
      confidence: 0.84,
    };
  } else if (hasAny(signal, ["封面", "截图", "配色", "字体", "排版", "视觉", "海报"])) {
    suggestion = {
      suggestedCategory: "封面参考",
      suggestedTags: ["封面结构", "视觉参考", "点击率"],
      suggestedNextAction: "先标注这个封面的可复用布局",
      reusableStructure: "主体图 + 大字标题 + 背景层次",
      referenceValue: "可复用视觉布局",
      reason: "明确包含视觉表达参考",
      confidence: 0.82,
    };
  } else if (hasAny(signal, ["教程", "步骤", "方法", "怎么做", "指南", "流程"])) {
    suggestion = {
      suggestedCategory: "脚本结构",
      suggestedTags: ["教程结构", "步骤拆解", "方法论"],
      suggestedNextAction: "先列出可复用的 4 个步骤",
      reusableStructure: "问题引入 → 步骤演示 → 总结",
      referenceValue: "可复用教程脚本",
      reason: "适合复用为教程脚本结构",
      confidence: 0.86,
    };
  } else if (card.sourceType === "video" && sourceTitle) {
    suggestion = {
      suggestedCategory: "素材案例",
      suggestedTags: ["视频案例", "内容拆解", "参考素材"],
      suggestedNextAction: "先判断它适合参考选题、结构还是表达方式",
      reusableStructure: "视频案例 → 可参考点 → 自用改写",
      referenceValue: "可作为视频案例输入",
      reason: "视频内容适合作为创作案例输入",
      confidence: 0.78,
    };
  } else if (card.sourceType === "article") {
    suggestion = {
      suggestedCategory: "选题灵感",
      suggestedTags: ["观点素材", "文章参考", "选题输入"],
      suggestedNextAction: "先提取文章中可支撑一个视频观点的段落",
      reusableStructure: "观点 → 论据 → 视频角度",
      referenceValue: "可提取观点素材",
      reason: "图文/文章适合作为观点或素材输入",
      confidence: 0.76,
    };
  } else if (card.sourceType === "image") {
    suggestion = {
      suggestedCategory: "封面参考",
      suggestedTags: ["图片素材", "视觉参考", "封面参考"],
      suggestedNextAction: "先标注这张图可复用的视觉元素",
      reusableStructure: "视觉元素 → 封面布局 → 自用草图",
      referenceValue: "可参考视觉元素",
      reason: "图片更适合作为视觉或素材参考",
      confidence: 0.77,
    };
  } else {
    suggestion = {
      suggestedCategory: "待判断",
      suggestedTags: ["待整理", "创作灵感"],
      suggestedNextAction: "先补一句这个内容启发你做什么视频",
      reusableStructure: "补充意图后再判断",
      referenceValue: "需要补充收藏理由",
      reason: "当前信息不足，需要用户补充意图",
      confidence: 0.58,
    };
  }

  return {
    cardId: card.id,
    sourceTitle,
    sourcePlatform: card.sourcePlatform,
    currentCategory: card.contentType,
    selectedCategory: suggestion.suggestedCategory,
    ...suggestion,
  };
}

function hasAny(text, keywords) {
  return keywords.some((keyword) => text.includes(keyword));
}

function createCardFromCapture(markTaskCreated = false) {
  const ai = state.capture.aiApplied ? state.capture.aiResult : null;
  const card = {
    id: id("card"),
    title: cardTitleFromNote(state.capture.userNote),
    sourceTitle: state.capture.sourceTitle || state.capture.sourcePreview || cardTitleFromNote(state.capture.userNote),
    sourceDescription: state.capture.sourceDescription || state.capture.sourcePreview,
    sourcePlatform: state.capture.sourcePlatform,
    sourceDomain: state.capture.sourceDomain,
    sourceUrl: state.capture.sourceUrl,
    resolvedUrl: state.capture.sourceUrl,
    sourceAuthor: "",
    sourceType: state.capture.sourceType || (state.capture.coverType === "image" ? "image" : "unknown"),
    contentType: ai?.contentType || state.capture.contentType,
    tags: ai?.tags || [],
    userNote: state.capture.userNote.trim() || "可以做一期内容拆解视频",
    reusableStructure: ai?.reusableStructure || "等待 AI 提取可复用结构",
    referenceValue: ai?.referenceValue || "等待进一步判断",
    nextAction: ai?.nextAction || "先补充脚本大纲",
    reason: ai?.reason || "尚未应用 AI 建议",
    confidence: ai?.confidence || 0,
    aiApplied: Boolean(ai),
    aiClassificationStatus: ai ? "applied" : "not_started",
    taskDraft: ai?.taskDraft || null,
    status: markTaskCreated ? "planned" : "inbox",
    coverType: state.capture.coverType,
    coverGradient: state.capture.coverGradient,
    createdAt: Date.now(),
    taskCreated: markTaskCreated,
  };
  state.cards.unshift(card);
  return card;
}

function createTaskFromCard(card) {
  if (!card) return null;
  const existing = state.tasks.find((task) => task.sourceCardIds.includes(card.id));
  if (existing) return existing;
  const task = {
    id: id("task"),
    title: card.taskDraft?.taskTitle || card.userNote.slice(0, 25) || card.title,
    contentDirection: card.taskDraft?.contentDirection || card.contentType,
    status: "待完善",
    outline: card.taskDraft?.outline || ["开场痛点", "参考案例", "我的观点或演示", "结论与行动建议"],
    materialList: card.taskDraft?.materialList || [
      card.sourceUrl ? `来源链接：${card.sourceUrl}` : "补充来源链接或截图",
      `参考灵感：${displaySourceTitle(card)}`,
    ],
    nextAction: card.taskDraft?.nextAction || card.nextAction || "先补充脚本大纲",
    sourceCardIds: [card.id],
    generatedByAi: Boolean(card.aiApplied || card.taskDraft),
    createdAt: Date.now(),
  };
  state.tasks.unshift(task);
  card.taskCreated = true;
  card.status = "planned";
  return task;
}

function resetCapture() {
  state.capture = structuredClone(defaultState.capture);
}

function render() {
  const screenMap = {
    home: renderHome,
    capture: renderCapture,
    inbox: renderInbox,
    detail: renderDetail,
    taskPool: renderTaskPool,
    taskDetail: renderTaskDetail,
    review: renderReview,
  };
  app.innerHTML = screenMap[state.screen]?.() || renderHome();
  bindEvents();
}

function renderHome() {
  const weekStart = Date.now() - 7 * 24 * 60 * 60 * 1000;
  const weeklyNew = state.cards.filter((card) => card.createdAt >= weekStart).length;
  const planned = state.cards.filter((card) => card.status === "planned").length;
  const review = state.cards.filter((card) => ["inbox", "later"].includes(card.status)).length;
  const recent = state.cards.slice(0, 2);
  return `
    <section class="screen">
      <h2 class="page-title">别只收藏</h2>
      <p class="page-subtitle">把刷到的内容，变成可创作的视频任务</p>
      <div class="hero-card">
        <h3>面向内容创作者的 AI 选题与素材管理工具</h3>
        <div class="chip-row">
          <span class="chip">AI 选题</span>
          <span class="chip">素材管理</span>
          <span class="chip">创作任务</span>
        </div>
      </div>
      <div class="stats-grid">
        ${statCard("本周新增", weeklyNew)}
        ${statCard("已转任务", planned)}
        ${statCard("待复盘", review)}
      </div>
      <button class="primary-button" data-action="go-capture">记录创作灵感</button>
      <div class="entry-grid">
        <button class="entry-card" data-action="go-inbox"><strong>灵感收集箱</strong><span>整理选题素材</span></button>
        <button class="entry-card" data-action="go-task-pool"><strong>创作任务池</strong><span>推进视频任务</span></button>
        <button class="entry-card full" data-action="go-review"><strong>开始复盘</strong><span>把近期灵感筛成下一条视频</span></button>
      </div>
      <h3 class="section-title">最近创作灵感</h3>
      ${recent.length ? recent.map(renderAssetCard).join("") : `<div class="empty-state"><p>保存第一条视频灵感，看看它能变成什么创作任务。</p></div>`}
    </section>
  `;
}

function renderCapture() {
  const capture = state.capture;
  const ai = capture.aiResult;
  return `
    <section class="screen">
      ${topbar("记录", "go-home")}
      <h2 class="page-title">记录一个视频灵感</h2>
      <p class="page-subtitle">把链接、截图或文字先收进来，再转成创作任务。</p>
      <div class="card source-card">
        <div class="chip-row">
          <span class="chip active">${capture.sourcePlatform}</span>
          ${capture.sourceDomain ? `<span class="chip">${capture.sourceDomain}</span>` : ""}
        </div>
        ${cover(capture.coverType, capture.contentType, "small")}
        <p class="caption">${escapeHtml(capture.sourcePreview)}</p>
        <div class="source-actions">
          <button class="secondary-button" data-action="paste-link">粘贴链接</button>
          <button class="secondary-button" data-action="add-image">添加截图/图片</button>
        </div>
      </div>
      <label class="field-label" for="note">这个内容启发你做什么视频？</label>
      <textarea id="note" data-input="capture-note" placeholder="例如：可以做一期 AI 工具测评 / reaction 视频 / 教程拆解">${escapeHtml(capture.userNote)}</textarea>
      <h3 class="section-title">内容类型</h3>
      ${categoryChips(capture.contentType, "set-capture-category")}
      <div class="card ai-card">
        <h3>AI 识别创作用途</h3>
        <p>识别这条内容适合作为选题、标题、封面、脚本还是素材，并生成下一步行动。</p>
        <button class="secondary-button" data-action="run-ai">AI 识别</button>
        ${ai ? aiResult(ai, capture.aiApplied) : ""}
      </div>
    </section>
    <div class="screen-bottom-bar">
      <button class="primary-button" data-action="save-card">保存为灵感</button>
      <button class="secondary-button" data-action="save-task">生成创作任务</button>
    </div>
  `;
}

function renderInbox() {
  const cards = filteredCards();
  return `
    <section class="screen">
      ${topbar("收集箱", "go-home")}
      <h2 class="page-title">灵感收集箱</h2>
      <p class="page-subtitle">把刷到的选题、封面、标题和素材整理成创作资产</p>
      <div class="stats-grid">
        ${statCard("全部卡片", state.cards.length)}
        ${statCard("已转任务", state.cards.filter((card) => card.status === "planned").length)}
        ${statCard("以后再看", state.cards.filter((card) => card.status === "later").length)}
      </div>
      <p class="caption">内容类型</p>
      ${filterChips(["全部", ...categories], state.categoryFilter, "set-category-filter")}
      <p class="caption">状态</p>
      ${filterChips(statuses.map((item) => item.label), labelForFilter(state.statusFilter), "set-status-filter")}
      <div class="card ai-card batch-entry">
        <h3>AI 分类收藏夹</h3>
        <p>根据链接标题、简介、来源平台和你的备注，批量推荐创作用途分类。</p>
        <button class="primary-button" data-action="batch-classify">AI 分类收藏夹</button>
      </div>
      ${state.batchSuggestions.length ? renderBatchClassifyPanel() : ""}
      <div class="asset-list">
        ${cards.length ? cards.map(renderAssetCard).join("") : `<div class="empty-state"><h3>当前筛选下没有卡片</h3><p>换个类型或先记录一个灵感。</p></div>`}
      </div>
    </section>
  `;
}

function renderBatchClassifyPanel() {
  return `
    <div class="batch-panel">
      <div class="batch-panel-head">
        <div>
          <h3>AI 分类建议</h3>
          <p>AI 只给建议，应用后才会更新卡片分类和标签。</p>
        </div>
        <button class="secondary-button" data-action="apply-high-confidence">批量应用高置信度建议</button>
      </div>
      <div class="suggestion-list">
        ${state.batchSuggestions.map(renderBatchSuggestion).join("")}
      </div>
    </div>
  `;
}

function renderBatchSuggestion(suggestion) {
  return `
    <article class="suggestion-card">
      <h4>${escapeHtml(suggestion.sourceTitle)}</h4>
      <div class="chip-row">
        <span class="chip">${escapeHtml(suggestion.sourcePlatform)}</span>
        <span class="chip">当前：${escapeHtml(suggestion.currentCategory)}</span>
        <span class="chip active">推荐：${escapeHtml(suggestion.suggestedCategory)}</span>
        <span class="chip">置信度 ${Math.round(suggestion.confidence * 100)}%</span>
      </div>
      <p><strong>标签</strong>：${suggestion.suggestedTags.map(escapeHtml).join("、")}</p>
      <p><strong>下一步行动</strong>：${escapeHtml(suggestion.suggestedNextAction)}</p>
      <p><strong>理由</strong>：${escapeHtml(suggestion.reason)}</p>
      <p class="caption">修改分类</p>
      ${categoryChips(suggestion.selectedCategory || suggestion.suggestedCategory, "set-suggestion-category").replaceAll("data-value=", `data-id="${suggestion.cardId}" data-value=`)}
      <div class="inline-actions">
        <button class="primary-button" data-action="apply-classification" data-id="${suggestion.cardId}">应用建议</button>
        <button class="secondary-button" data-action="skip-classification" data-id="${suggestion.cardId}">跳过</button>
      </div>
    </article>
  `;
}

function renderDetail() {
  const card = findCard(state.selectedCardId);
  if (!card) return emptyWithBack("找不到这条灵感", "go-inbox");
  return `
    <section class="screen">
      ${topbar("灵感详情", "go-inbox")}
      ${cover(card.coverType, card.contentType, "large")}
      <div class="chip-row" style="margin-top:14px">
        <span class="chip ${chipClass(card.contentType)}">${card.contentType}</span>
        <span class="chip">${card.sourcePlatform}</span>
      </div>
      <h2 class="page-title" style="margin-top:16px">${escapeHtml(displaySourceTitle(card))}</h2>
      <div class="card source-card">
        <p><strong>用户备注</strong></p>
        <p>${escapeHtml(card.userNote)}</p>
        <div class="chip-row">${card.tags.map((tag) => `<span class="chip">AI ${escapeHtml(tag)}</span>`).join("")}</div>
        <p><strong>可复用结构</strong>：${escapeHtml(card.reusableStructure || "等待 AI 提取")}</p>
        <p><strong>参考价值</strong>：${escapeHtml(card.referenceValue)}</p>
        <p><strong>下一步行动</strong>：${escapeHtml(card.nextAction)}</p>
        <p><strong>判断理由</strong>：${escapeHtml(card.reason || "尚未生成")}</p>
        <p><strong>关联任务状态</strong>：${card.taskCreated ? "已转为创作任务" : "尚未生成任务草稿"}</p>
        <p class="caption">来源链接：${escapeHtml(card.resolvedUrl || card.sourceUrl || "暂无链接")}</p>
      </div>
      <button class="primary-button" data-action="task-from-detail" data-id="${card.id}">生成创作任务</button>
      <button class="secondary-button" data-action="open-source">打开来源</button>
      <button class="text-button" data-action="go-inbox">返回</button>
    </section>
  `;
}

function renderTaskPool() {
  return `
    <section class="screen">
      ${topbar("任务池", "go-home")}
      <h2 class="page-title">创作任务池</h2>
      <p class="page-subtitle">把灵感推进到脚本、素材和剪辑准备。</p>
      ${
        state.tasks.length
          ? state.tasks.map(renderTaskCard).join("")
          : `<div class="empty-state">
              <h3>还没有创作任务</h3>
              <p>从灵感卡片点击“生成创作任务”，这里会出现视频选题、脚本大纲和素材清单。</p>
              <button class="primary-button" data-action="go-inbox">去灵感收集箱</button>
              <button class="secondary-button" data-action="go-capture">记录一个灵感</button>
            </div>`
      }
    </section>
  `;
}

function renderTaskDetail() {
  const task = findTask(state.selectedTaskId);
  if (!task) return emptyWithBack("找不到这个任务", "go-task-pool");
  const sourceCards = task.sourceCardIds.map(findCard).filter(Boolean);
  return `
    <section class="screen">
      ${topbar("任务详情", "go-task-pool")}
      <h2 class="page-title">${escapeHtml(task.title)}</h2>
      ${task.generatedByAi ? `<p class="page-subtitle">AI 生成的创作任务草稿：由收藏内容整理生成，用户可继续修改标题、脚本大纲和素材清单。</p>` : ""}
      <div class="chip-row">
        ${task.generatedByAi ? `<span class="chip active">AI 生成草稿</span>` : ""}
        <span class="chip ${chipClass(task.contentDirection)}">${task.contentDirection}</span>
        <span class="chip">${task.status}</span>
      </div>
      <div class="card source-card">
        <p><strong>脚本大纲</strong></p>
        <ol class="outline-list">${task.outline.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
      </div>
      <div class="card source-card">
        <p><strong>素材清单</strong></p>
        <ul class="material-list">${task.materialList.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>
      </div>
      <div class="card source-card">
        <p><strong>参考灵感卡片</strong></p>
        ${sourceCards.map((card) => `<p>${escapeHtml(card.title)} · ${escapeHtml(card.sourcePlatform)}</p>`).join("")}
        <p><strong>下一步行动</strong>：${escapeHtml(task.nextAction)}</p>
      </div>
      <button class="primary-button" data-action="mark-script">标记为待写脚本</button>
      <button class="text-button" data-action="go-task-pool">返回任务池</button>
    </section>
  `;
}

function renderReview() {
  const reviewCards = state.cards.filter((card) => !["dropped", "planned"].includes(card.status));
  const card = reviewCards[state.reviewIndex] || reviewCards[0];
  if (!card) {
    return `
      <section class="screen">
        ${topbar("复盘", "go-home")}
        <div class="empty-state"><h3>没有待复盘内容</h3><p>先记录几条灵感，再回来筛选值得做的视频任务。</p><button class="primary-button" data-action="go-capture">记录一个灵感</button></div>
      </section>
    `;
  }
  return `
    <section class="screen">
      ${topbar("复盘", "go-home")}
      <p class="caption">${state.reviewIndex + 1} / ${reviewCards.length}</p>
      <h2 class="page-title">这个能变成哪类视频任务？</h2>
      ${renderAssetCard(card, { compactActions: true })}
      ${reviewAiRecommendation(card)}
      <button class="primary-button" data-action="review-task" data-id="${card.id}">生成创作任务</button>
      <button class="secondary-button" data-action="join-existing">加入已有任务</button>
      <button class="secondary-button" data-action="review-later" data-id="${card.id}">以后再看</button>
      <button class="secondary-button" data-action="review-drop" data-id="${card.id}">放弃</button>
      <button class="text-button" data-action="review-skip">跳过</button>
    </section>
  `;
}

function statCard(label, count) {
  return `<div class="stat-card"><strong>${count}</strong><span>${label}</span></div>`;
}

function topbar(title, action) {
  return `<div class="topbar"><button class="back-button" data-action="${action}">返回</button><strong>${title}</strong><span></span></div>`;
}

function categoryChips(selected, action) {
  return `<div class="chip-row">${categories
    .map((category) => `<button class="chip ${chipClass(category)} ${selected === category ? "active" : ""}" data-action="${action}" data-value="${category}">${category}</button>`)
    .join("")}</div>`;
}

function filterChips(options, selected, action) {
  return `<div class="chip-row">${options
    .map((option) => `<button class="chip ${selected === option ? "active" : ""}" data-action="${action}" data-value="${option}">${option}</button>`)
    .join("")}</div>`;
}

function cover(type, contentType, size = "") {
  const classes = ["cover", size, type === "image" ? "image" : type === "link" ? "link" : ""].filter(Boolean).join(" ");
  const badge = type === "image" ? "用户图片" : type === "link" ? "链接预览" : "渐变占位";
  return `<div class="${classes}"><span class="cover-badge">${badge}</span><span>${escapeHtml(contentType)}</span></div>`;
}

function aiResult(ai, applied) {
  return `
    <div class="ai-result">
      <div class="chip-row">
        <span class="chip active">推荐类型：${ai.contentType}</span>
        ${ai.tags.map((tag) => `<span class="chip">#${escapeHtml(tag)}</span>`).join("")}
      </div>
      <p><strong>参考价值</strong>：${escapeHtml(ai.referenceValue)}</p>
      <p><strong>可复用结构</strong>：${escapeHtml(ai.reusableStructure)}</p>
      <p><strong>下一步行动</strong>：${escapeHtml(ai.nextAction)}</p>
      <p><strong>建议转任务</strong>：${ai.shouldCreateTask ? "是" : "否"}</p>
      <p><strong>判断理由</strong>：${escapeHtml(ai.reason)} · 置信度 ${Math.round((ai.confidence || 0) * 100)}%</p>
      <div class="inline-actions">
        <button class="secondary-button" data-action="apply-ai">${applied ? "已应用建议" : "应用建议"}</button>
        <button class="primary-button" data-action="save-task">生成创作任务</button>
      </div>
    </div>
  `;
}

function reviewAiRecommendation(card) {
  const ai = createMockAiResult({
    userNote: card.userNote,
    sourceTitle: card.sourceTitle,
    sourceDescription: card.sourceDescription,
    sourcePreview: displaySourceTitle(card),
    sourcePlatform: card.sourcePlatform,
    sourceDomain: card.sourceDomain,
    sourceUrl: card.sourceUrl,
    sourceType: card.sourceType,
    contentType: card.contentType,
  });
  return `
    <div class="card ai-card compact-ai">
      <h3>AI 推荐：${escapeHtml(ai.contentType)}</h3>
      <p><strong>理由</strong>：${escapeHtml(ai.reason)}</p>
      <p><strong>建议动作</strong>：${ai.shouldCreateTask ? "生成创作任务" : "先补充启发后再判断"}</p>
      <p><strong>下一步</strong>：${escapeHtml(ai.nextAction)}</p>
    </div>
  `;
}

function renderAssetCard(card, options = {}) {
  const title = displaySourceTitle(card);
  return `
    <article class="asset-card" data-card-id="${card.id}">
      ${cover(card.coverType, card.contentType, "small")}
      <h3 data-action="open-card" data-id="${card.id}">${escapeHtml(title)}</h3>
      <div class="chip-row">
        <span class="chip ${chipClass(card.contentType)}">${card.contentType}</span>
        <span class="chip">${card.sourcePlatform}</span>
        ${card.sourceType ? `<span class="chip">${escapeHtml(sourceTypeLabel(card.sourceType))}</span>` : ""}
        ${card.tags.slice(0, 2).map((tag) => `<span class="chip">AI ${escapeHtml(tag)}</span>`).join("")}
      </div>
      <p>${escapeHtml(card.sourceDescription || card.userNote)}</p>
      <p><strong>下一步</strong>：${escapeHtml(card.nextAction)}</p>
      <div class="card-footer">
        <span>${escapeHtml(card.sourceDomain || card.sourcePlatform)} · ${formatDate(card.createdAt)}</span>
        <span>${statusLabel(card.status)}</span>
      </div>
      ${
        options.compactActions
          ? ""
          : `<div class="inline-actions">
              <button class="secondary-button" data-action="task-from-card" data-id="${card.id}">生成任务</button>
              <span class="menu-wrap">
                <button class="ghost-button" data-action="toggle-menu" data-id="${card.id}">更多</button>
                ${state.openMenuId === card.id ? miniMenu(card) : ""}
              </span>
            </div>`
      }
    </article>
  `;
}

function miniMenu(card) {
  return `
    <div class="mini-menu">
      <button data-action="open-source">打开来源</button>
      <button data-action="mark-later" data-id="${card.id}">以后再看</button>
      <button data-action="delete-card" data-id="${card.id}">删除</button>
    </div>
  `;
}

function renderTaskCard(task) {
  return `
    <article class="task-card">
      <h3>${escapeHtml(task.title)}</h3>
      <div class="chip-row">
        ${task.generatedByAi ? `<span class="chip active">AI 生成草稿</span>` : ""}
        <span class="chip ${chipClass(task.contentDirection)}">${task.contentDirection}</span>
        <span class="chip">${task.status}</span>
        <span class="chip">关联素材 ${task.sourceCardIds.length}</span>
      </div>
      <p><strong>下一步</strong>：${escapeHtml(task.nextAction)}</p>
      <ol class="outline-list">${task.outline.slice(0, 3).map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
      <ul class="material-list">${task.materialList.slice(0, 2).map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>
      <button class="secondary-button" data-action="open-task" data-id="${task.id}">查看详情</button>
    </article>
  `;
}

function emptyWithBack(text, action) {
  return `<section class="screen">${topbar("返回", action)}<div class="empty-state"><h3>${text}</h3></div></section>`;
}

function filteredCards() {
  return state.cards.filter((card) => {
    const categoryOk = state.categoryFilter === "全部" || card.contentType === state.categoryFilter;
    const statusValue = valueForStatusLabel(state.statusFilter);
    const statusOk = statusValue === "全部" || card.status === statusValue;
    return categoryOk && statusOk;
  });
}

function labelForFilter(value) {
  return statuses.find((item) => item.value === value)?.label || value;
}

function valueForStatusLabel(label) {
  return statuses.find((item) => item.label === label)?.value || label;
}

function findCard(cardId) {
  return state.cards.find((card) => card.id === cardId);
}

function findTask(taskId) {
  return state.tasks.find((task) => task.id === taskId);
}

function bindEvents() {
  app.querySelectorAll("[data-action]").forEach((element) => {
    element.addEventListener("click", handleAction);
  });
  const note = app.querySelector("[data-input='capture-note']");
  if (note) {
    note.addEventListener("input", (event) => {
      state.capture.userNote = event.target.value;
      saveState();
    });
  }
}

function handleAction(event) {
  const action = event.currentTarget.dataset.action;
  const value = event.currentTarget.dataset.value;
  const itemId = event.currentTarget.dataset.id;
  const actions = {
    "go-home": () => setScreen("home"),
    "go-capture": () => {
      resetCapture();
      setScreen("capture");
    },
    "go-inbox": () => setScreen("inbox"),
    "go-task-pool": () => setScreen("taskPool"),
    "go-review": () => setScreen("review", { reviewIndex: 0 }),
    "paste-link": pasteLink,
    "add-image": addImage,
    "set-capture-category": () => {
      state.capture.contentType = value;
      state.capture.aiApplied = false;
      saveAndRender();
    },
    "run-ai": runAi,
    "apply-ai": applyAi,
    "save-card": saveCard,
    "save-task": saveTask,
    "set-category-filter": () => {
      state.categoryFilter = value;
      saveAndRender();
    },
    "set-status-filter": () => {
      state.statusFilter = valueForStatusLabel(value);
      saveAndRender();
    },
    "batch-classify": runBatchClassify,
    "set-suggestion-category": () => setSuggestionCategory(itemId, value),
    "apply-classification": () => applyClassification(itemId),
    "skip-classification": () => skipClassification(itemId),
    "apply-high-confidence": applyHighConfidence,
    "open-card": () => setScreen("detail", { selectedCardId: itemId }),
    "task-from-card": () => taskFromCard(itemId),
    "task-from-detail": () => taskFromCard(itemId),
    "toggle-menu": () => {
      state.openMenuId = state.openMenuId === itemId ? null : itemId;
      saveAndRender();
    },
    "open-source": () => showToast("演示环境中模拟打开原平台 App。"),
    "mark-later": () => markCard(itemId, "later"),
    "delete-card": () => deleteCard(itemId),
    "open-task": () => setScreen("taskDetail", { selectedTaskId: itemId }),
    "mark-script": markScript,
    "review-task": () => reviewTask(itemId),
    "join-existing": () => showToast("演示版暂不支持加入已有任务。"),
    "review-later": () => reviewMark(itemId, "later"),
    "review-drop": () => reviewMark(itemId, "dropped"),
    "review-skip": reviewSkip,
  };
  actions[action]?.();
}

function pasteLink() {
  state.capture.sourceUrl = "https://b23.tv/demo";
  state.capture.sourcePlatform = "哔哩哔哩";
  state.capture.sourceDomain = "b23.tv";
  state.capture.sourcePreview = "模拟 B 站视频链接：一个创作者拆解爆款 reaction 视频结构。";
  state.capture.sourceTitle = "一个创作者拆解爆款 reaction 视频结构";
  state.capture.sourceDescription = "示例短链会被解析成视频标题、简介和来源平台，再交给 AI 分类。";
  state.capture.sourceType = "video";
  state.capture.coverType = "link";
  state.capture.coverGradient = "material";
  showToast("已粘贴模拟 B 站链接");
  saveAndRender();
}

function addImage() {
  state.capture.coverType = "image";
  state.capture.sourcePlatform = state.capture.sourceUrl ? state.capture.sourcePlatform : "截图来源";
  state.capture.sourcePreview = "已添加一张模拟截图，这张图片将作为卡片封面。";
  state.capture.sourceTitle = state.capture.sourceTitle || "一张可作为封面参考的截图";
  state.capture.sourceDescription = "图片收藏更适合作为视觉、封面或素材参考。";
  state.capture.sourceType = state.capture.sourceUrl ? state.capture.sourceType : "image";
  showToast("已添加模拟截图封面");
  saveAndRender();
}

function runAi() {
  state.capture.aiResult = createMockAiResult();
  state.capture.aiApplied = false;
  showToast("AI mock 已生成建议");
  saveAndRender();
}

function applyAi() {
  if (!state.capture.aiResult) return;
  state.capture.contentType = state.capture.aiResult.contentType;
  state.capture.aiApplied = true;
  showToast("已应用 AI 建议");
  saveAndRender();
}

function runBatchClassify() {
  const targets = state.cards.filter((card) =>
    card.contentType === "待判断" ||
    !card.aiClassificationStatus ||
    card.aiClassificationStatus === "not_started" ||
    card.aiClassificationStatus === "failed"
  ).filter((card) => card.aiClassificationStatus !== "applied");

  if (!targets.length) {
    state.batchSuggestions = [];
    showToast("没有需要分类的卡片");
    saveAndRender();
    return;
  }

  // TODO metrics: ai_batch_classify_clicked and ai_classification_suggested.
  state.batchSuggestions = targets.map(classifyCardForCreativeUse);
  showToast(`已生成 ${state.batchSuggestions.length} 条 AI 分类建议`);
  saveAndRender();
}

function setSuggestionCategory(cardId, category) {
  const suggestion = state.batchSuggestions.find((item) => item.cardId === cardId);
  if (!suggestion) return;
  suggestion.selectedCategory = category;
  saveAndRender();
}

function applyClassification(cardId) {
  const suggestion = state.batchSuggestions.find((item) => item.cardId === cardId);
  const card = findCard(cardId);
  if (!suggestion || !card) return;
  Object.assign(card, {
    contentType: suggestion.selectedCategory || suggestion.suggestedCategory,
    tags: suggestion.suggestedTags,
    reusableStructure: suggestion.reusableStructure,
    referenceValue: suggestion.referenceValue,
    nextAction: suggestion.suggestedNextAction,
    reason: suggestion.reason,
    confidence: suggestion.confidence,
    aiSuggestedCategory: suggestion.suggestedCategory,
    aiSuggestedTags: suggestion.suggestedTags,
    aiSuggestedNextAction: suggestion.suggestedNextAction,
    aiSuggestedReason: suggestion.reason,
    aiClassificationStatus: "applied",
  });
  state.batchSuggestions = state.batchSuggestions.filter((item) => item.cardId !== cardId);
  // TODO metrics: ai_classification_applied.
  showToast("已应用 AI 分类建议");
  saveAndRender();
}

function skipClassification(cardId) {
  state.batchSuggestions = state.batchSuggestions.filter((item) => item.cardId !== cardId);
  // TODO metrics: ai_classification_skipped.
  showToast("已跳过这条建议");
  saveAndRender();
}

function applyHighConfidence() {
  const highConfidence = state.batchSuggestions.filter((suggestion) => suggestion.confidence >= 0.75);
  if (!highConfidence.length) {
    showToast("暂无高置信度建议");
    return;
  }
  // TODO metrics: batch_high_confidence_applied.
  highConfidence.forEach((suggestion) => applyClassification(suggestion.cardId));
}

function saveCard() {
  const card = createCardFromCapture(false);
  resetCapture();
  showToast("已保存到灵感收集箱");
  setScreen("inbox", { selectedCardId: card.id });
}

function saveTask() {
  const card = createCardFromCapture(true);
  createTaskFromCard(card);
  resetCapture();
  showToast("已生成创作任务");
  setScreen("taskPool");
}

function taskFromCard(cardId) {
  const card = findCard(cardId);
  const task = createTaskFromCard(card);
  if (!task) return;
  showToast("已生成创作任务");
  setScreen("taskPool", { selectedTaskId: task.id });
}

function markCard(cardId, status) {
  const card = findCard(cardId);
  if (!card) return;
  card.status = status;
  showToast("状态已更新");
  saveAndRender();
}

function deleteCard(cardId) {
  state.cards = state.cards.filter((card) => card.id !== cardId);
  state.tasks = state.tasks.filter((task) => !task.sourceCardIds.includes(cardId));
  showToast("已删除灵感卡片");
  saveAndRender();
}

function markScript() {
  const task = findTask(state.selectedTaskId);
  if (!task) return;
  task.status = "待写脚本";
  showToast("已标记为待写脚本");
  saveAndRender();
}

function reviewTask(cardId) {
  const card = findCard(cardId);
  if (!card) return;
  const ai = createMockAiResult({
    userNote: card.userNote,
    sourceTitle: card.sourceTitle,
    sourceDescription: card.sourceDescription,
    sourcePreview: displaySourceTitle(card),
    sourcePlatform: card.sourcePlatform,
    sourceDomain: card.sourceDomain,
    sourceUrl: card.sourceUrl,
    sourceType: card.sourceType,
    contentType: card.contentType,
  });
  Object.assign(card, {
    contentType: ai.contentType,
    tags: ai.tags,
    reusableStructure: ai.reusableStructure,
    referenceValue: ai.referenceValue,
    nextAction: ai.nextAction,
    reason: ai.reason,
    confidence: ai.confidence,
    aiApplied: true,
    taskDraft: ai.taskDraft,
  });
  const task = createTaskFromCard(card);
  if (!task) return;
  showToast("已根据 AI 推荐生成任务");
  setScreen("taskPool", { selectedTaskId: task.id });
}

function reviewMark(cardId, status) {
  const card = findCard(cardId);
  if (!card) return;
  card.status = status;
  showToast("状态已更新");
  reviewSkip();
}

function reviewSkip() {
  const reviewCards = state.cards.filter((card) => !["dropped", "planned"].includes(card.status));
  state.reviewIndex = Math.min(state.reviewIndex + 1, Math.max(reviewCards.length - 1, 0));
  saveAndRender();
}

setupPortfolioInteractions();
render();
