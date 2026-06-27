# 别只收藏 Interactive Demo

这是「别只收藏」的可交互产品经理作品集展示页。页面在现有 `dont-just-save-interactive-demo` 目录内增量维护，包含产品视频、可交互手机 Demo、用户调研、需求分析、竞品矩阵、产品闭环、设计产出、技术实现和商业化假设。

Demo 使用 mock AI 和 `localStorage` 保存模拟数据，不包含真实后端，不调用真实 AI API。

## 本地直接打开

直接双击 `index.html` 即可打开。

## 本地静态服务器预览

也可以在本目录运行任意静态服务器，例如：

```bash
python -m http.server 8080
```

然后访问：

```text
http://localhost:8080
```

## Vercel 部署

通过 Vercel 导入 GitHub 仓库时，请将 Root Directory 设置为：

```text
dont-just-save-interactive-demo
```

该目录根部已经包含 `index.html`，所有资源均使用相对路径引用，不依赖 localhost、远程 CDN 或外部图片资源。

## GitHub + Vercel 自动部署

1. 将仓库推送到 GitHub。
2. 在 Vercel 新建项目并导入该仓库。
3. Root Directory 填写 `dont-just-save-interactive-demo`。
4. Framework Preset 选择 Other 或 Static。
5. Build Command 留空。
6. Output Directory 留空或使用默认值。
7. 之后推送到 GitHub 默认分支即可触发自动部署。

## Vercel CLI 部署

如果使用 Vercel CLI，可以进入本目录后运行：

```bash
vercel --prod
```

## 部署前检查

- `index.html` 位于 `dont-just-save-interactive-demo` 根目录。
- `styles.css` 和 `app.js` 使用相对路径引用。
- `assets/demo-video.mp4` 使用相对路径引用。
- `assets/video-poster.png` 使用相对路径引用。
- 页面可以作为纯静态站点部署。
- 手机交互 Demo 仍使用浏览器 `localStorage` 保存模拟数据。
