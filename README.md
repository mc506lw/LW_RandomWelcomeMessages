## [LW]随机欢迎消息 - 智能多源欢迎|节日特效|高度自定义

[![GitHub release](https://img.shields.io/github/v/release/mc506lw/LW_RandomWelcomeMessages?style=flat-square)](https://github.com/mc506lw/LW_RandomWelcomeMessages)

![Spigot Version](https://img.shields.io/badge/Spigot-1.12.2%2B-brightgreen?style=flat-square)

![Downloads](https://img.shields.io/github/downloads/YourName/LW_RandomWelcomeMessage/total?style=flat-square)

\## ✨ 插件特色

\- **智能消息池**：混合本地/API消息源，支持权重分配

\- 🎉 **节日模式**：自动识别传统节日触发专属诗词

\- 🎶 **沉浸式体验**：支持动作栏显示与自定义音效

\- 📊 **高度可配置**：七大类配置项，自由掌控每个细节

\- 🔄 **热重载支持**：修改配置无需重启服务器

\## 📥 快速开始

1. 将插件放入 `plugins/` 文件夹
2. 重启服务器生成配置文件
3. 按需修改 `config.yml`
4. 使用 `/lw reload` 应用配置

\## 🎯 核心功能

\### 🌈 智能消息系统

\```yaml

\# 示例配置

mixed-format:

 \- "40:“{shici}” 欢迎{player}进入服务器！"  # 40%概率显示诗词欢迎

 \- "30:“{hitokoto}” 欢迎{player}进入服务器！" # 30%概率一言语录

 \- "30:{shici}" # 30%概率纯诗词

 \- "90:欢迎{player}进入服务器！" # 保底本地消息

\```

\### 🎁 节日模式（自动激活）

\```yaml

festival:

 dates:

  \- "04-05:清明节:jieri/qingmingjie" # 清明节专属诗词

  \- "01-22:春节:jieri/chunjie" 

\```

\### 🔊 沉浸式音效

\```yaml

sound:

 type: ENTITY_PLAYER_LEVELUP # 升级音效

 volume: 1.0 # 100%音量

\```

\## 📄 配置详解

\### 📌 消息源配置

| 参数    | 说明          | 默认值           |

|------------|---------------------|-------------------------|

| api-url   | API端点地址       | https://v1.hitokoto.cn/ |

| timeout   | 请求超时(ms)      | 3000          |

\### 🎨 显示设置

\```yaml

settings:

 use-actionbar: true # 在动作栏显示消息

\```

\## ⚙ 命令与权限

| 命令        | 权限节点           | 功能说明     |

|-------------------|----------------------------|----------------|

| /lw reload    | lw.randomwelcomemessage.admin | 热重载配置    |

| /lw test     | lw.randomwelcomemessage.admin | 测试消息系统   |

\## 🖼 效果截图

![欢迎示例](https://via.placeholder.com/800x400?text=欢迎消息展示效果)

*图示：包含动作栏消息与聊天框显示的双重效果*

\## 📦 下载地址

[![下载按钮](https://img.shields.io/badge/立即下载-v1.0-blue?style=for-the-badge&logo=github)](https://github.com/YourName/LW_RandomWelcomeMessage/releases/latest)

\## ❓ 常见问题

Q: API请求失败如何处理？  

A: 检查网络连接 → 确认API地址有效性 → 适当增加timeout值

Q: 如何添加自定义节日？  

A: 在 `festival.dates` 按 `MM-DD:节日名称:分类` 格式添加

\## 💬 支持与交流

QQ群：`645921477`  

[点击加入讨论](https://qm.qq.com/q/rUwWnGvnyN)

\---

**更新日志**  

`v1.0` 初始发布  

\- 实现多源消息系统  

\- 支持节日模式与音效  

\- 完成基础配置框架