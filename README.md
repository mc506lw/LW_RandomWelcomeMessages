### 插件简介

为服务器提供动态欢迎解决方案，集成多平台API与本地化内容，支持节日自动响应与交互反馈。

### 核心功能

#### 混合消息源

实时调用诗词API/一言API
本地消息库自动降级机制
权重控制系统（自定义触发概率）

#### 节日响应系统

预设传统节日触发模板
支持自定义日期格式（MM-DD）
专属诗词内容展示

#### 交互优化

可配置音效反馈（默认ENTITY_PLAYER_LEVELUP）
动作栏/聊天框双显示模式
线程安全设计

### 技术参数

适配版本：1.21.x（可以试试看）
存储方式：本地化配置
网络请求：3000ms超时熔断
资源占用：轻量化设计（＜2MB内存）

### 配置示例

```Yaml
# LW_RandomWelcomeMessage
# 这是配置文件，请在此配置插件的各项功能。
# 可以加入QQ群：645921477，一起交流学习！


# 消息系统配置
messages:
  # ==============================
  # 消息配置
  # ------------------------------
  # 配置格式
  # 权重:消息内容
  # 权重小于等于0时不显示消息
  # 权重可为任意数，但建议不要超过100
  # 可用变量（需启用对应消息源）：
  # {player}    - 触发事件的玩家名称
  # {hitokoto}  - 一言服务返回内容（未启用时回退本地消息）
  # {shici}     - 诗词服务返回内容（未启用时回退本地消息）
  # {shici_[type]}  - [type]为诗词类型（例：shici_shuqing，需在诗词服务配置中设定,未启用时回退本地消息，无分类时显示为全部诗词）
  # {random}    - 随机选择内容（根据权重分配）
  # ==============================
  mixed-format:
    - "40:“{shici}” 欢迎{player}进入服务器！"
    - "30:“{hitokoto}” 欢迎{player}进入服务器！" # 启用一言服务
    - "30:{shici}" # 仅启用诗词服务
    - "90:欢迎{player}进入服务器！" # 仅启用本地消息

  # 随机选择权重
  random-weight:
    shici: 50
    hitokoto: 50

  # =========================
  # ========================
  # 一言服务配置
  # ========================
  hitokoto:
    api-url: "https://v1.hitokoto.cn/"  # API端点地址
    timeout: 3000  # 请求超时时间（单位：毫秒）

  # ==============================
  # 诗词服务配置
  # ------------------------------
  # 分类接口地址（参考文档：https://v1.jinrishici.com/）
  # ==============================
  shici:
    all: "https://v1.jinrishici.com/all"
    timeout: 3000  # 请求超时时间（单位：毫秒）
    # 分类接口地址
    categories:
      shuqing: "https://v1.jinrishici.com/shuqing"

# =============================

# ==============================
# 节日模式配置
# ------------------------------
# 日期格式：MM-DD:节日名称:诗词类型
# 启用后优先使用节日专属消息模板
# ==============================
festival:
  enable: true  # 启用节日模式

  # 节日消息模板（变量说明）：
  # {jieri_shici} - 节日专属诗词内容
  # {festival_name} - 节日显示名称
  mixed-format:
    - "“{jieri_shici}” {festival_name}快乐！"

  # 节日日期映射表
  dates:
    - "04-05:清明节:jieri/qingmingjie"  # 清明节配置
    - "01-22:春节:jieri/chunjie"       # 春节配置

# ==============================
# 音效配置
# ------------------------------
# 支持 Minecraft 原版音效类型：
# https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html
# ==============================
sound:
  enable: true                   # 启用音效
  type: ENTITY_PLAYER_LEVELUP    # 音效类型
  volume: 1.0                    # 音量（0.0-1.0）
  pitch: 1.0                     # 音调（0.5-2.0）

# ==============================
# 显示设置
# ==============================
settings:
  use-actionbar: true  # 使用动作栏显示消息（禁用时在聊天框显示）

# ==============================
# 命令反馈消息配置
# ==============================
command-messages:
  no-permission: "&c⚠ 权限不足！"
  test-success: "&a✅ 测试成功！消息内容：&r{message}"
  test-failed: "&c❌ API请求失败，请检查控制台日志"
  reload-message: "&a[成功] &e配置已热重载！"
```

### 管理命令

/rwm reload - 热重载配置
/rwm test - 消息模板测试
/rwm debug - 显示API状态

### 权限节点

rwm.receive - 接收欢迎消息
rwm.admin - 配置管理权限

### 获取支持

GitHub仓库：https://github.com/mc506lw/LW_RandomWelcomeMessages
技术交流：QQ群 645921477

### 运行要求

Java 16+ 运行环境
不要更改本地时间!

### 感谢列表

[一言网（hitokoto.cn）](https://hitokoto.cn/)
[gushi.ci](http://gushi.ci/)