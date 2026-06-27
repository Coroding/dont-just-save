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
  capture: {
    sourceUrl: "",
    sourcePlatform: "手动记录",
    sourceDomain: "",
    sourcePreview: "手动记录",
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
      sourcePlatform: "哔哩哔哩",
      sourceDomain: "b23.tv",
      sourceUrl: "https://b23.tv/demo",
      contentType: "素材案例",
      tags: ["reaction", "案例", "视频结构"],
      userNote: "可以做一期 reaction 视频拆解",
      referenceValue: "开头结构可拆解",
      nextAction: "先拆解这个视频的开头结构",
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

function formatDate(timestamp) {
  const date = new Date(timestamp);
  return `${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function cardTitleFromNote(note) {
  const clean = note.trim();
  return clean ? clean.slice(0, 18) : "新的视频灵感";
}

function createMockAiResult() {
  const note = state.capture.userNote;
  let contentType = state.capture.contentType;
  if (note.includes("标题")) contentType = "标题参考";
  if (note.includes("封面") || note.includes("截图")) contentType = "封面参考";
  if (note.includes("脚本") || note.includes("结构")) contentType = "脚本结构";
  if (note.includes("reaction") || note.includes("案例")) contentType = "素材案例";
  return {
    contentType,
    tags: contentType === "标题参考" ? ["标题结构", "点击理由"] : ["可执行", "视频结构", "创作者"],
    referenceValue: contentType === "封面参考" ? "可参考视觉钩子" : "适合转成视频任务",
    nextAction: contentType === "标题参考" ? "先提取标题结构" : "先补充脚本大纲",
    shouldCreateTask: true,
  };
}

function createCardFromCapture(markTaskCreated = false) {
  const ai = state.capture.aiApplied ? state.capture.aiResult : null;
  const card = {
    id: id("card"),
    title: cardTitleFromNote(state.capture.userNote),
    sourcePlatform: state.capture.sourcePlatform,
    sourceDomain: state.capture.sourceDomain,
    sourceUrl: state.capture.sourceUrl,
    contentType: ai?.contentType || state.capture.contentType,
    tags: ai?.tags || [],
    userNote: state.capture.userNote.trim() || "可以做一期内容拆解视频",
    referenceValue: ai?.referenceValue || "等待进一步判断",
    nextAction: ai?.nextAction || "先补充脚本大纲",
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
    title: card.userNote.slice(0, 25) || card.title,
    contentDirection: card.contentType,
    status: "待完善",
    outline: ["开场痛点", "参考案例", "我的观点或演示", "结论与行动建议"],
    materialList: [
      card.sourceUrl ? `来源链接：${card.sourceUrl}` : "补充来源链接或截图",
      `参考灵感：${card.title}`,
    ],
    nextAction: card.nextAction || "先补充脚本大纲",
    sourceCardIds: [card.id],
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
      <div class="asset-list">
        ${cards.length ? cards.map(renderAssetCard).join("") : `<div class="empty-state"><h3>当前筛选下没有卡片</h3><p>换个类型或先记录一个灵感。</p></div>`}
      </div>
    </section>
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
      <h2 class="page-title" style="margin-top:16px">${escapeHtml(card.title)}</h2>
      <div class="card source-card">
        <p><strong>用户备注</strong></p>
        <p>${escapeHtml(card.userNote)}</p>
        <div class="chip-row">${card.tags.map((tag) => `<span class="chip">AI ${escapeHtml(tag)}</span>`).join("")}</div>
        <p><strong>参考价值</strong>：${escapeHtml(card.referenceValue)}</p>
        <p><strong>下一步行动</strong>：${escapeHtml(card.nextAction)}</p>
        <p class="caption">来源链接：${escapeHtml(card.sourceUrl || "暂无链接")}</p>
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
      <div class="chip-row">
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
      <p><strong>下一步行动</strong>：${escapeHtml(ai.nextAction)}</p>
      <p><strong>建议转任务</strong>：${ai.shouldCreateTask ? "是" : "否"}</p>
      <button class="secondary-button" data-action="apply-ai">${applied ? "已应用建议" : "应用建议"}</button>
    </div>
  `;
}

function renderAssetCard(card, options = {}) {
  return `
    <article class="asset-card" data-card-id="${card.id}">
      ${cover(card.coverType, card.contentType, "small")}
      <h3 data-action="open-card" data-id="${card.id}">${escapeHtml(card.title)}</h3>
      <div class="chip-row">
        <span class="chip ${chipClass(card.contentType)}">${card.contentType}</span>
        <span class="chip">${card.sourcePlatform}</span>
        ${card.tags.slice(0, 2).map((tag) => `<span class="chip">AI ${escapeHtml(tag)}</span>`).join("")}
      </div>
      <p>${escapeHtml(card.userNote)}</p>
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
        <span class="chip ${chipClass(task.contentDirection)}">${task.contentDirection}</span>
        <span class="chip">${task.status}</span>
        <span class="chip">关联素材 ${task.sourceCardIds.length}</span>
      </div>
      <p><strong>下一步</strong>：${escapeHtml(task.nextAction)}</p>
      <ol class="outline-list">${task.outline.slice(0, 3).map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ol>
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
  state.capture.coverType = "link";
  state.capture.coverGradient = "material";
  showToast("已粘贴模拟 B 站链接");
  saveAndRender();
}

function addImage() {
  state.capture.coverType = "image";
  state.capture.sourcePlatform = state.capture.sourceUrl ? state.capture.sourcePlatform : "截图来源";
  state.capture.sourcePreview = "已添加一张模拟截图，这张图片将作为卡片封面。";
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
  taskFromCard(cardId);
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
