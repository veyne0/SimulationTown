# Simulation Town（模拟小镇）

Minecraft **1.21.1** / **NeoForge 21.1.x** / **Java 21** 的模组工程骨架。
当前进度：**阶段 0**（环境跑通）。功能还没开始写。

---

## 这个工程里有什么

```
SimulationTown/
├── build.gradle                          构建脚本（ModDevGradle）
├── settings.gradle                       插件仓库 + 工程名
├── gradle.properties                     ★ 版本号、modid、模组信息全在这
├── gradle/wrapper/gradle-wrapper.properties   Gradle 版本（8.10.2）
├── src/main/java/com/simulationtown/
│   └── SimulationTown.java               ★ 模组入口
└── src/main/resources/
    ├── META-INF/neoforge.mods.toml       ★ 模组元数据（NeoForge 靠它加载）
    ├── pack.mcmeta                       资源包版本
    └── assets/simulation_town/lang/      中英文语言文件
```

★ 标记的四个位置，是后面每天都要改的地方。

**两个名字别搞混：**

| | 值 | 用在哪 |
|---|---|---|
| modid | `simulation_town` | 代码、资源路径、存档。**只能用 小写字母/数字/下划线** |
| 显示名 | `Simulation Town` | 模组列表、玩家看到的名字。随便写 |

modid **发布后就改不了了**（存档里所有方块都用它做前缀）。

---

## 第一次运行

本机已经有 JDK 21（`C:\java21`，微软 OpenJDK 21.0.7），`gradle.properties` 里已经指向它。

**1. 装 IntelliJ IDEA**（Community 免费版就够）
**2. 打开工程**：`File → Open`，选中 `SimulationTown` 这个文件夹（不是里面的某个文件）
**3. 等 Gradle 同步**：右下角有进度条，第一次要下载 Gradle 8.10.2 + NeoForge，**大约 500MB，10~30 分钟**，取决于网速
**4. 运行**：右上角运行配置里选 `runClient` → 点绿色三角

**成功的样子**：
- 游戏启动，主菜单出现
- `Mods` 列表里能看到 **Simulation Town**
- 日志里有 `[Simulation Town] done loading`

到这一步，阶段 0 才算完成。

> `gradlew.bat` 没带 `gradle-wrapper.jar`（二进制文件，我这边下不了）。不用管它：
> IDEA 会直接读 `gradle/wrapper/gradle-wrapper.properties` 自己下载 Gradle 8.10.2。
> 如果哪天你想用命令行，在有网的机器上跑一次 `gradle wrapper` 就会生成那个 jar。

---

## 版本号在哪改

`gradle.properties` 里这两行是**唯一需要对的地方**：

```properties
neo_version=21.1.133        # NeoForge 版本，1.21.1 对应 21.1.x
```

如果同步时报 `Could not find net.neoforged:neoforge:21.1.133`，说明这个具体小版本号不存在，
换成 `21.1.115`、`21.1.150`、`21.1.209` 中的一个，重试同步即可。

`build.gradle` 第一行的 `2.0.78` 是 ModDevGradle 插件版本，同样，报错就换
`2.0.66` / `2.0.72` / `2.0.79` 试。

**除了这两个数字，别的都不用动。**

---

## 打不开官方仓库（maven.neoforged.net）

国内访问 `maven.neoforged.net` 会被 DNS 污染，表现为同步卡在
`Connect to maven.neoforged.net:443 ... Connection timed out`。

`build.gradle` 里已经把 `https://neoforged.forgecdn.net/releases` 放在前面当镜像，但它
**缺一个构件**：`net.neoforged:minecraft-dependencies:1.21.1`（镜像里连这个目录都没有）。
所以还得让官方仓库本身能连上——用 hosts 把它指到真实 IP：

1. 用**管理员权限**打开记事本，编辑 `C:\Windows\System32\drivers\etc\hosts`
2. 末尾加一行：

   ```
   195.181.172.2 maven.neoforged.net
   ```

3. 在 PowerShell 里执行 `ipconfig /flushdns`，然后在 IDEA 里重新同步

> 这个 IP 来自 `maven.neoforged.net` 的 CDN（cdn77）解析结果，会变。如果连不上，
> 把 `195.181.172.2` 换成 `195.181.172.6` 再试。
> 验证方法：浏览器打开
> `https://maven.neoforged.net/releases/net/neoforged/minecraft-dependencies/1.21.1/minecraft-dependencies-1.21.1.pom`
> ——能下载出一个 XML 文件就说明通了。

---

## 常见报错

| 报错 | 原因 | 怎么办 |
|---|---|---|
| `Could not find net.neoforged:neoforge` | neo_version 写错 | 换一个 21.1.x 的号 |
| `Unsupported class file major version 65` | 用了 Java 17 编 | 工程 JDK 设成 `C:\java21` |
| `Plugin [id: 'net.neoforged.moddev'] was not found` | 插件版本不存在或没网 | 换版本号 / 检查网络 |
| `Could not find net.neoforged:minecraft-dependencies` | 镜像缺这个包，官方仓库又连不上 | 按上面「打不开官方仓库」加 hosts |
| 游戏起来了但 Mods 列表没有它 | mods.toml 的 modId 和 `@Mod` 里的不一致 | 两个都必须是 `simulation_town` |
| `java: 找不到符号` | NeoForge 还没同步完 | 等同步结束再编译 |

---

## 接下来

阶段 1 = 第一个方块（`town_center`）。按 `../ROADMAP.md` 走，别提前写功能。
