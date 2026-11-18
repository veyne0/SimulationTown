package com.simulationtown.building;

import com.buildlib.api.BuildLibApi;
import com.buildlib.structure.BuildingInfo;
import com.buildlib.structure.BuildingScanner;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.simulationtown.block;
import com.simulationtown.towncenter.centerblock;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.property.Properties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class build_block extends Block implements BlockUIMenuType.BlockUI {
    public build_block(Properties properties){
        super(properties);
    }

    // 和 building_info.() 里的四个目录一一对应
    private static final String[] CATEGORIES = {"residence", "agriculture", "industry", "special"};
    private static final String[] CATEGORY_NAMES = {"住宅", "农业", "工业", "特殊"};

    // 建筑 nbt 的根目录（游戏目录下）。
    // buildlib 自带的默认根目录是 "simulation_town_buliding"（少一个 i），我们用自己这个名字，所以每次显式传进去。
    private static final String ROOT_DIR = "BUILDING_DIR_NAME";

    // 固定行数：控件树两端必须一样，所以行是写死的，只是每行显示的文字会变
    private static final int ROWS = 10;

    // 列表缓存。绑定的 supplier 每个 tick 都会被问一次，不能每次都去 Files.walk 读盘
    private static List<BuildingInfo> cache = List.of();
    private static long cacheAt = 0L;

    private static List<BuildingInfo> cachedScan() {
        long now = System.currentTimeMillis();
        if (now - cacheAt > 2000L) {
            cache = BuildingScanner.scanAll(ROOT_DIR, CATEGORIES);
            cacheAt = now;
        }
        return cache;
    }

    private static String categoryName(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(cat)) return CATEGORY_NAMES[i];
        }
        return cat;
    }

    private static List<String> namesOf(String cat) {
        List<String> out = new ArrayList<>();
        for (BuildingInfo bi : cachedScan()) {
            if (bi.category().equals(cat)) out.add(bi.name());
        }
        return out;
    }

    /** 第 idx 个建筑的名字，没有则返回空串。 */
    private static String nameAt(String cat, int idx) {
        List<String> names = namesOf(cat);
        return idx < names.size() ? names.get(idx) : "";
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockUIMenuType.openUI((ServerPlayer) player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {

        // 当前选中的分类。按钮点击是发到服务端执行的，所以这份状态以服务端为准；
        // 客户端要靠下面 catLabel 的绑定才知道现在选的是哪个分类（拼 nbt 路径用）。
        final String[] selected = {CATEGORIES[0]};

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.ROW).width(360).height(220).paddingAll(6).gapAll(8));
        root.addClass("panel_bg");

        // ---------- 左：四个分类 + 刷新 ----------
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(88).heightPercent(100).gapAll(4));

        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            Button btn = new Button();
            btn.setText(CATEGORY_NAMES[i]);
            btn.layout(l -> l.widthPercent(100).height(20));
            btn.setOnServerClick(e -> selected[0] = cat);
            sidebar.addChild(btn);
        }

        Button btnRefresh = new Button();
        btnRefresh.setText("刷新");
        btnRefresh.layout(l -> l.widthPercent(100).height(20));
        btnRefresh.setOnServerClick(e -> cacheAt = 0L); // 让下一次扫描重新读盘
        sidebar.addChild(btnRefresh);

        // ---------- 右：当前分类下的 nbt 列表（点文字进预览）----------
        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100).gapAll(2));

        Label title = new Label();
        title.bind(DataBindingBuilder.componentS2C(() -> {
            List<String> names = namesOf(selected[0]);
            return Component.literal("建筑 - " + categoryName(selected[0]) + "（" + names.size() + "）");
        }).build());
        content.addChild(title);

        Label hint = new Label();
        hint.bind(DataBindingBuilder.componentS2C(() -> Component.literal("点名字进入放置预览")).build());
        content.addChild(hint);

        // 把「当前分类」也推给客户端：用绑定塞进一个隐藏 Label。
        // 不能用 SimpleBinding + registerListener —— 它只在值变化时才回调，初始同步不触发。
        Label catLabel = new Label();
        catLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(selected[0])).build());
        catLabel.setVisible(false);
        content.addChild(catLabel);

        for (int i = 0; i < ROWS; i++) {
            final int idx = i;

            UIElement row = new UIElement();
            row.layout(l -> l.widthPercent(100).height(12));

            // 文字用 Label + S2C 绑定（和标题一样，验证过能显示）。
            // 之前用 Button + registerListener 改文字，监听器不触发，所以那几行一直是空的。
            Label rowLabel = new Label();
            rowLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(nameAt(selected[0], idx))).build());
            rowLabel.layout(l -> l.widthPercent(100).heightPercent(100));
            row.addChild(rowLabel);

            // 点这一行 → 打开 buildlib 的幽灵方块预览（客户端事件，openPreview 是纯客户端 API）
            row.addEventListener(UIEvents.CLICK, e -> {
                String n = rowLabel.getText().getString();
                String cat = catLabel.getText().getString();
                if (n.isEmpty() || cat.isEmpty()) return;
                Path path = BuildingScanner.resolveNbtPath(ROOT_DIR, new BuildingInfo(cat, n));
                if (path == null) return;
                BuildLibApi.openPreview(path.toString());
            });

            content.addChild(row);
        }

        root.addChildren(sidebar, content);
        return ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), holder.player);
    }
}
