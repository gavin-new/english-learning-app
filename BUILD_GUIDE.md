# 英语学习APP - 编译指南

## 项目概述

本项目是一个基于 **Kotlin + Jetpack Compose** 开发的英语学习 Android APP，包含 5218 个词汇（涵盖沪教版小学到 CET-6）。

## 无需本地编译环境的方案

由于你没有安装 Android SDK 和 Gradle，我为你准备了 **GitHub Actions 云端编译方案**，无需在本地安装任何编译环境。

---

## 方案一：GitHub Actions 云端编译（推荐）

### 步骤 1：创建 GitHub 仓库

1. 访问 https://github.com/new
2. 创建一个新的仓库（例如：`english-learning-app`）
3. **不要**初始化 README（因为项目已有 README）

### 步骤 2：上传代码到 GitHub

在项目根目录执行以下命令：

```bash
# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交代码
git commit -m "Initial commit: English Learning App with 5218 vocabulary words"

# 添加远程仓库（将 YOUR_USERNAME 替换为你的 GitHub 用户名）
git remote add origin https://github.com/YOUR_USERNAME/english-learning-app.git

# 推送到 GitHub
git push -u origin main
```

### 步骤 3：触发自动编译

推送代码后，GitHub Actions 会自动开始编译：

1. 访问你的 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 查看 **Build Android APK** 工作流的运行状态
4. 等待约 5-10 分钟，编译完成后会自动上传 APK 文件

### 步骤 4：下载 APK 文件

编译完成后：

1. 在 Actions 页面点击最新的工作流运行记录
2. 滚动到页面底部的 **Artifacts** 区域
3. 下载以下文件：
   - `debug-apk` - 调试版本（推荐测试使用）
   - `release-apk` - 发布版本（未签名）

---

## 方案二：手动触发编译

如果你不想等待推送触发，可以手动触发：

1. 进入 GitHub 仓库的 **Actions** 页面
2. 选择 **Build Android APK** 工作流
3. 点击 **Run workflow** 按钮
4. 选择分支（main）并点击 **Run workflow**

---

## 方案三：本地编译（需要安装环境）

如果你后续想本地编译，需要安装：

1. **Android Studio** - https://developer.android.com/studio
2. **JDK 17** - 安装 Android Studio 时会自动安装
3. 打开项目，点击 **Build > Generate Signed Bundle/APK**

---

## 项目文件说明

```
english-learning-app/
├── .github/workflows/build.yml    # GitHub Actions 编译配置
├── app/
│   ├── src/main/assets/vocabulary/  # 词汇数据文件（5218词）
│   │   ├── grade1.json ~ grade6.json    # 小学词汇
│   │   ├── middle_school.json           # 中考词汇
│   │   ├── high_school.json             # 高考词汇
│   │   ├── cet4.json                    # 四级词汇
│   │   └── cet6.json                    # 六级词汇
│   └── build.gradle                     # 应用构建配置
├── gradle/wrapper/                      # Gradle Wrapper
├── build.gradle                         # 项目构建配置
└── README.md                            # 项目说明
```

---

## 词汇统计

| 等级 | 词汇数量 | 文件 |
|------|----------|------|
| 一年级 | 209 | grade1.json |
| 二年级 | 217 | grade2.json |
| 三年级 | 175 | grade3.json |
| 四年级 | 393 | grade4.json |
| 五年级 | 217 | grade5.json |
| 六年级 | 275 | grade6.json |
| 中考 | 1305 | middle_school.json |
| 高考 | 963 | high_school.json |
| CET-4 | 852 | cet4.json |
| CET-6 | 612 | cet6.json |
| **总计** | **5218** | - |

---

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构**: MVVM
- **数据库**: Room（离线数据存储）
- **导航**: Jetpack Navigation
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 34)

---

## 常见问题

### Q: 编译失败怎么办？
A: 检查 GitHub Actions 日志，常见原因：
- 网络问题（依赖下载失败）- 重新运行即可
- 代码语法错误 - 检查 Kotlin 代码

### Q: APK 安装失败？
A: 
- Debug 版本可以直接安装
- Release 版本未签名，需要在开发者选项中允许安装未知来源应用

### Q: 如何更新词汇？
A: 修改 `app/src/main/assets/vocabulary/` 下的 JSON 文件，重新推送即可自动编译。

---

## 下一步建议

1. **创建 GitHub 仓库** 并推送代码
2. **等待 GitHub Actions 编译完成**
3. **下载 APK** 并安装测试
4. 如果需要发布到应用商店，需要创建签名密钥

---

*最后更新: 2026-05-28*
