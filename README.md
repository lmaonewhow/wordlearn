# WordLearn - 现代化词汇学习应用

## 项目简介

WordLearn 是一款基于 Android 的现代化词汇学习应用，旨在帮助用户高效学习和记忆单词。应用采用 Jetpack Compose 构建现代化 UI，结合科学的记忆方法，为用户提供个性化的词汇学习体验。

## 主要功能

- **词汇书管理**: 支持多种词汇书导入和切换
- **学习模式**: 提供卡片式学习界面，支持单词朗读、释义显示等功能
- **复习系统**: 基于记忆曲线的智能复习提醒
- **挑战模式**: 每日和定期单词挑战，巩固学习效果
- **收藏与错题本**: 可收藏重要单词，自动记录学习中的错误单词
- **学习计划**: 个性化学习计划制定与进度跟踪
- **个人资料**: 用户学习统计和进度展示

## 技术栈

- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **数据存储**: Room Database
- **依赖注入**: 手动依赖注入
- **异步处理**: Kotlin Coroutines
- **导航**: Jetpack Navigation Compose
- **网络**: Retrofit (用于在线功能)

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/wordlearn/
│   │   ├── data/               # 数据层
│   │   │   ├── api/            # API 接口
│   │   │   ├── dao/            # 数据访问对象
│   │   │   ├── model/          # 数据模型
│   │   │   ├── repository/     # 数据仓库
│   │   ├── navigation/         # 导航组件
│   │   ├── ui/                 # 界面层
│   │   │   ├── components/     # UI 组件
│   │   │   ├── screens/        # 应用屏幕
│   │   │   ├── theme/          # 应用主题
│   │   │   ├── viewmodel/      # 视图模型
│   │   ├── util/               # 工具类
│   ├── res/                    # 资源文件
│   └── AndroidManifest.xml     # 应用清单
```

## 安装和使用

### 系统要求

- Android 5.0 (API 21) 或更高版本
- 推荐 Android 8.0 (API 26) 或更高版本以获得最佳体验

### 安装方式

1. 从 GitHub 仓库下载最新发布的 APK
2. 在 Android 设备上启用"未知来源"安装权限
3. 安装 APK 文件

### 基本使用流程

1. **首次启动**: 完成用户资料设置
2. **选择词汇书**: 在首页或词汇书页面选择要学习的词汇书
3. **开始学习**: 点击学习按钮进入学习界面
4. **每日复习**: 根据提醒完成每日单词复习
5. **记录进度**: 在个人中心查看学习统计和进度

## 开发设置

### 环境准备

- Android Studio Arctic Fox (2020.3.1) 或更高版本
- JDK 11 或更高版本
- Gradle 7.0+ 

### 构建步骤

1. 克隆代码仓库：
```bash
git clone https://github.com/yourusername/wordlearn.git
```

2. 打开 Android Studio 并导入项目

3. 等待 Gradle 同步完成

4. 构建并运行应用

## 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。

## 贡献指南

欢迎提交问题报告、功能请求或贡献代码。请遵循以下步骤：

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交您的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个 Pull Request

## 联系方式

如有任何问题或建议，请通过 GitHub Issues 与我们联系。 