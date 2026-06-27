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

## GitHub Pages 部署

本项目通过 GitHub Actions 部署 `dont-just-save-interactive-demo/` 子目录到 GitHub Pages。

部署步骤：

1. 将代码 push 到 GitHub 的 main 分支。
2. 打开 GitHub 仓库。
3. 进入 Settings -> Pages。
4. 在 Build and deployment 中，将 Source 设置为 GitHub Actions。
5. 进入 Actions 页面，查看 `Deploy interactive demo to GitHub Pages` workflow 是否成功。
6. 部署成功后，在 workflow summary 或 Settings -> Pages 中查看 Pages 链接。

注意：

- 不需要 npm install。
- 不需要 React build。
- 不需要 Vercel。
- 不要部署整个 Android 工程。
- 发布内容来自 `dont-just-save-interactive-demo/`。

## 部署前检查

- `index.html` 位于 `dont-just-save-interactive-demo` 根目录。
- `styles.css` 和 `app.js` 使用相对路径引用。
- `assets/demo-video.mp4` 使用相对路径引用。
- `assets/video-poster.png` 使用相对路径引用。
- 页面可以作为纯静态站点部署。
- 手机交互 Demo 仍使用浏览器 `localStorage` 保存模拟数据。
