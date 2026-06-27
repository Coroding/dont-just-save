const path = require("node:path");
const { spawnSync } = require("node:child_process");
const ffmpegPath = require("ffmpeg-static");

const ffmpegDir = path.dirname(ffmpegPath);
const npxCommand = process.platform === "win32" ? "npx.cmd" : "npx";
const args = ["--yes", "hyperframes@0.6.81", "render", ...process.argv.slice(2)];
const pathKey = Object.keys(process.env).find((key) => key.toLowerCase() === "path") || "PATH";
const env = { ...process.env };

env[pathKey] = `${ffmpegDir}${path.delimiter}${process.env[pathKey] || ""}`;

const result = spawnSync(npxCommand, args, {
  stdio: "inherit",
  env,
  shell: process.platform === "win32",
});

process.exit(result.status ?? 1);
