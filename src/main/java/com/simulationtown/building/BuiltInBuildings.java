package com.simulationtown.building;

import com.buildlib.structure.BuildingScanner;
import com.simulationtown.SimulationTown;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

/*AI写的类，用于打包建筑资源*/
@EventBusSubscriber(modid = SimulationTown.MOD_ID)
public final class BuiltInBuildings {

    private static final String RESOURCE_ROOT = "/simulation_town/buildings";
    private static final String[] CATEGORIES = {"residence", "agriculture", "industry", "special"};

    private BuiltInBuildings() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        try {
            deployAll();
        } catch (Throwable t) {
            SimulationTown.LOGGER.warn("[内置建筑] 释放失败: {}", t.toString());
        }
    }

    // ======== 实际释放逻辑 ========

    /** 启动释放入口: 逐分类把 classpath 里的 nbt/json 拷到游戏目录, 返回本次新释放的文件数 */
    public static int deployAll() {
        int total = 0;
        for (String category : CATEGORIES) {
            Path targetDir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get()
                .resolve(BuildingScanner.DEFAULT_ROOT_DIR)
                .resolve(category);
            try {
                total += copyCategory(category, targetDir);
            } catch (Throwable t) {
                SimulationTown.LOGGER.warn("[内置建筑] 分类 {} 释放失败: {}", category, t.toString());
            }
        }
        if (total > 0) {
            SimulationTown.LOGGER.info("[内置建筑] 本次共释放 {} 个内置建筑", total);
        }
        return total;
    }

    /** 把 classpath 里 simulation_town/buildings/&lt;category&gt;/ 的所有文件拷到 targetDir, 返回新释放数 */
    private static int copyCategory(String category, Path targetDir) throws Exception {
        URL url = BuiltInBuildings.class.getResource(RESOURCE_ROOT + "/" + category);
        if (url == null) return 0;
        URI uri = url.toURI();
        // dev 环境 resources 是磁盘目录 (file:), 打包后在 jar 里 (jar:) — 两种都要支持
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                return copyAll(fs.getPath(RESOURCE_ROOT + "/" + category), targetDir);
            }
        }
        return copyAll(Paths.get(uri), targetDir);
    }

    /** 拷贝 srcDir 下所有 .nbt/.json 到 targetDir (已存在的跳过), 返回新释放数 */
    private static int copyAll(Path srcDir, Path targetDir) throws Exception {
        int n = 0;
        try (Stream<Path> list = Files.list(srcDir)) {
            for (Path src : list.toList()) {
                String name = src.getFileName().toString();
                // nbt 是建筑本体, json 是伴生配置 (职业要求等), 一起释放
                if (!name.endsWith(".nbt") && !name.endsWith(".json")) continue;
                Path dest = targetDir.resolve(name);
                if (Files.exists(dest)) continue;   // 不覆盖: 玩家改过的或已释放过的
                Files.createDirectories(targetDir);
                try (InputStream in = Files.newInputStream(src)) {
                    Files.copy(in, dest);
                }
                SimulationTown.LOGGER.info("[内置建筑] 释放: {}", targetDir.getParent().getFileName() + "/" + name);
                n++;
            }
        }
        return n;
    }
}
