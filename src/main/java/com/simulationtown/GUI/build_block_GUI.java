package com.simulationtown.GUI;

import com.buildlib.api.BuildLibApi;
import com.buildlib.structure.BlockData;
import com.buildlib.structure.BuildingInfo;
import com.buildlib.structure.BuildingScanner;
import com.buildlib.structure.StructureNbtParser;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.simulationtown.building.build_import;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 建筑方块（蓝图列表 / 导入 / 预览）的界面。 */
public final class build_block_GUI {

    private build_block_GUI() {
    }

    // 和 building_info.() 里的四个目录一一对应
    private static final String[] CATEGORIES = {"residence", "agriculture", "industry", "special"};
    private static final String[] CATEGORY_NAMES = {"住宅", "农业", "工业", "特殊"};

    // 每个分类对应的「管理方块」：buildlib 用它校验这个 nbt 里到底有没有该类建筑的管理方块。
    // 没有 → 客户端不开预览，服务端在点「建造」时也会拒绝。
    private static final Map<String, String> MANAGER_BLOCK = Map.of(
            "residence", "simulation_town:residence_block",
            "agriculture", "simulation_town:agriculture_block",
            "industry", "simulation_town:industry_block",
            "special", "simulation_town:special_block");

    // 建筑 nbt 的根目录（游戏目录下）。
    // buildlib 自带的默认根目录是 "simulation_town_buliding"（少一个 i），我们用自己这个名字，所以每次显式传进去。
    private static final String ROOT_DIR = "simulation_town_buliding";

    // 每页几张卡片。控件树两端必须一样，所以槽位数是写死的，只是每张卡显示的文字会变
    private static final int PAGE_SIZE = 4;

    /** 列表里的一行：名字 + 尺寸 + 方块数 + 价格（json 的 cost，0 = 免费；资金 double 支持小数）。 */
    private record Entry(String category, String name, String path, String size, int blocks, double cost) {
    }

    /** 从 nbt 里解析出来的信息（按文件修改时间缓存，文件不变就不重复解析）。 */
    private record Meta(String size, int blocks) {
    }

    private static final Map<String, Meta> META_CACHE = new HashMap<>();
    private static List<Entry> entries = List.of();
    private static long cacheAt = 0L;

    /** 让下一次扫描重新读盘（导入完成后调用） */
    public static void refresh() {
        cacheAt = 0L;
    }

    // ---------------- 扫描 + 解析 ----------------

    private static List<Entry> all() {
        long now = System.currentTimeMillis();
        if (cacheAt == 0L || now - cacheAt > 2000L) {
            rebuild();
            cacheAt = now;
        }
        return entries;
    }

    /** 真正读盘：扫目录 + 给每个文件补尺寸/方块数/价格。2 秒最多跑一次。 */
    private static void rebuild() {
        List<Entry> out = new ArrayList<>();
        for (BuildingInfo bi : BuildingScanner.scanAll(ROOT_DIR, CATEGORIES)) {
            Path p = BuildingScanner.resolveNbtPath(ROOT_DIR, bi);
            if (p == null) continue;
            Meta meta = metaOf(p);
            // 价格在伴生 json（"cost" 字段），没有 = 0 免费。json 小，rebuild 本身有 2 秒缓存
            double cost = com.buildlib.api.NpcBuildApi.costFor(bi.category() + "/" + bi.name());
            out.add(new Entry(bi.category(), bi.name(), p.toString(), meta.size(), meta.blocks(), cost));
        }
        entries = out;
    }

    /** 尺寸和方块数：buildlib 只有「读 nbt + 解析方块列表」的 API，尺寸由方块坐标自己算。 */
    private static Meta metaOf(Path path) {
        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            String key = path + "|" + modified;      // 文件一改，key 就变，自动重新解析
            Meta cached = META_CACHE.get(key);
            if (cached != null) return cached;

            CompoundTag tag = StructureNbtParser.readNbtAuto(BuildingScanner.readNbtBytes(path));
            List<BlockData> blocks = StructureNbtParser.parseBlocks(tag);
            if (blocks.isEmpty()) {
                // 有些 nbt 外面多包了一层，buildlib 自己有解包方法
                blocks = StructureNbtParser.parseBlocks(StructureNbtParser.unwrapNestedRoot(tag));
            }
            if (blocks.isEmpty()) return new Meta("?", 0);

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            int count = 0;
            for (BlockData b : blocks) {
                BlockState st = b.state();
                // nbt 里通常连空气一起存了，全算进去数字就没意义了（一个 5×1×6 的房子会显示 30）
                if (st.isAir() || st.is(Blocks.STRUCTURE_VOID)) continue;
                count++;
                var pos = b.pos();
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            if (count == 0) return new Meta("?", 0);

            Meta meta = new Meta(
                    (maxX - minX + 1) + " × " + (maxY - minY + 1) + " × " + (maxZ - minZ + 1),
                    count);
            META_CACHE.put(key, meta);
            return meta;
        } catch (Throwable t) {
            return new Meta("?", 0);   // 坏文件不阻断列表
        }
    }

    // ---------------- 按分类取第几行 ----------------

    private static Entry at(String cat, int idx) {
        if (idx < 0) return null;
        int seen = 0;
        for (Entry e : all()) {
            if (!e.category().equals(cat)) continue;
            if (seen == idx) return e;
            seen++;
        }
        return null;
    }

    private static int countOf(String cat) {
        int n = 0;
        for (Entry e : all()) if (e.category().equals(cat)) n++;
        return n;
    }

    private static int pages(String cat) {
        int n = countOf(cat);
        return Math.max(1, (n + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static String categoryName(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(cat)) return CATEGORY_NAMES[i];
        }
        return cat;
    }

    private static String nameText(String cat, int page, int i) {
        Entry e = at(cat, page * PAGE_SIZE + i);
        return e == null ? "" : e.name();
    }

    private static String metaText(String cat, int page, int i) {
        Entry e = at(cat, page * PAGE_SIZE + i);
        return e == null ? "" : e.size() + " · " + e.blocks() + " 方块";
    }

    private static String priceText(String cat, int page, int i) {
        Entry e = at(cat, page * PAGE_SIZE + i);
        if (e == null) return "";
        return e.cost() > 0 ? "价格：" + (e.cost() == Math.floor(e.cost())
                ? String.valueOf((long) e.cost()) : String.format("%.2f", e.cost())) : "价格：免费";
    }

    // ---------------- 界面 ----------------

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {

        // 当前选中的分类 + 页码。按钮点击都发到服务端执行，所以状态以服务端为准；
        // 客户端要靠隐藏 Label 的绑定才知道现在选的是哪个分类（拼 nbt 路径用）。
        final String[] selected = {CATEGORIES[0]};
        final int[] page = {0};

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.ROW).width(320).height(196).paddingAll(6).gapAll(8));
        root.addClass("panel_bg");

        // ---------- 左：四个分类 + 刷新 + 导入（窄栏，把宽度让给卡片）----------
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(56).heightPercent(100).gapAll(3));

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            Button btn = new Button();
            btn.setText(CATEGORY_NAMES[i]);
            btn.layout(l -> l.widthPercent(100).height(15));
            btn.setOnServerClick(e -> {
                selected[0] = cat;
                page[0] = 0;        // 换分类回到第一页
            });
            sidebar.addChild(btn);
        }

        Button btnRefresh = new Button();
        btnRefresh.setText("刷新");
        btnRefresh.layout(l -> l.widthPercent(100).height(15));
        btnRefresh.setOnServerClick(e -> cacheAt = 0L); // 让下一次扫描重新读盘
        sidebar.addChild(btnRefresh);

        // 导入：选一个 .nbt 文件，判断分类后复制进游戏目录的分类文件夹。
        // 用 setOnClick（客户端）而不是 setOnServerClick —— Swing 文件选择框只在客户端有。
        Button btnImport = new Button();
        btnImport.setText("导入");
        btnImport.layout(l -> l.widthPercent(100).height(15));
        btnImport.setOnClick(e -> new build_import().daoru());
        sidebar.addChild(btnImport);

        // ---------- 右：卡片列表（点卡片进预览）----------
        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100).gapAll(2));

        Label title = new Label();
        title.layout(l -> l.height(13));
        title.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "建筑 - " + categoryName(selected[0]) + "（" + countOf(selected[0]) + "）")).build());
        content.addChild(title);

        Label hint = new Label();
        hint.layout(l -> l.height(10));
        hint.bind(DataBindingBuilder.componentS2C(() -> Component.literal("点卡片进入放置预览")).build());
        content.addChild(hint);

        // 把「当前分类」推给客户端：绑定塞进一个隐藏 Label（SimpleBinding 的监听器初始不触发，所以走绑定）
        Label catLabel = new Label();
        catLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(selected[0])).build());
        catLabel.setVisible(false);
        content.addChild(catLabel);

        // 卡片网格：2×2。行用 flexGrow 平分剩余高度 → 卡片自动铺满右侧，下面不再留空
        UIElement cards = new UIElement();
        cards.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexGrow(1).gapAll(3));
        content.addChild(cards);

        for (int row = 0; row < PAGE_SIZE / 2; row++) {
            UIElement rowEl = new UIElement();
            rowEl.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexGrow(1).gapAll(3));
            cards.addChild(rowEl);

            for (int col = 0; col < 2; col++) {
                final int slot = row * 2 + col;

                UIElement card = new UIElement();
                card.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                        .paddingAll(4).gapAll(3));
                card.addClass("panel_bg");

                Label cardName = new Label();
                cardName.layout(l -> l.height(12));
                cardName.bind(DataBindingBuilder.componentS2C(
                        () -> Component.literal(nameText(selected[0], page[0], slot))).build());

                Label cardMeta = new Label();
                cardMeta.layout(l -> l.height(10));
                cardMeta.bind(DataBindingBuilder.componentS2C(
                        () -> Component.literal(metaText(selected[0], page[0], slot))).build());

                Label cardPrice = new Label();
                cardPrice.layout(l -> l.height(10));
                cardPrice.bind(DataBindingBuilder.componentS2C(
                        () -> Component.literal(priceText(selected[0], page[0], slot))).build());

                // 这一页没这么多建筑时把整张卡藏起来。
                // SimpleBinding.registerListener 在第一次同步时不触发，所以用 onRemoteSyncReceived：
                // 它就是在「收到对面推来的值」时调用，包括第一次。
                Label visLabel = new Label();
                visLabel.setVisible(false);
                visLabel.bind(DataBindingBuilder.componentS2C(
                                () -> Component.literal(at(selected[0], page[0] * PAGE_SIZE + slot) != null ? "1" : "0"))
                        .onRemoteSyncReceived(c -> card.setVisible("1".equals(c.getString())))
                        .build());

                card.addChildren(visLabel, cardName, cardMeta, cardPrice);

                // 点这一张 → 打开 buildlib 的幽灵方块预览（客户端事件，openPreview 是纯客户端 API）
                card.addEventListener(UIEvents.CLICK, e -> {
                    String n = cardName.getText().getString();
                    String cat = catLabel.getText().getString();
                    if (n.isEmpty() || cat.isEmpty()) return;
                    String required = MANAGER_BLOCK.get(cat);
                    if (required == null) return;
                    Path path = BuildingScanner.resolveNbtPath(ROOT_DIR, new BuildingInfo(cat, n));
                    if (path == null) return;
                    // 第 2 参数 "分类/文件名"：建造完成事件里原样传回，用来写回方块实体
                    // 第 3 参数：管理方块，nbt 里没有它 → 客户端不开预览，服务端也拒绝建造
                    // 第 4 参数 managerOnly：只放管理方块（本体等玩家雇佣 NPC 逐块盖）
                    BuildLibApi.openPreview(path.toString(), cat + "/" + n, required, true);
                });

                rowEl.addChild(card);
            }
        }

        // ---------- 翻页 ----------
        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(14).gapAll(4));

        Button btnPrev = new Button();
        btnPrev.setText("<");
        btnPrev.layout(l -> l.width(20).height(14));
        btnPrev.setOnServerClick(e -> page[0] = Math.max(0, page[0] - 1));

        Label pageLabel = new Label();
        pageLabel.layout(l -> l.flexGrow(1).height(14));
        pageLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "第 " + (page[0] + 1) + " / " + pages(selected[0]) + " 页")).build());

        Button btnNext = new Button();
        btnNext.setText(">");
        btnNext.layout(l -> l.width(20).height(14));
        btnNext.setOnServerClick(e -> page[0] = Math.min(pages(selected[0]) - 1, page[0] + 1));

        footer.addChildren(btnPrev, pageLabel, btnNext);
        content.addChild(footer);

        root.addChildren(sidebar, content);
        return ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), holder.player);
    }
}
