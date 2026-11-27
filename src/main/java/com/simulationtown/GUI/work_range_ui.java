package com.simulationtown.GUI;

import com.buildlib.api.BuildLibApi;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.industry.industry_block_entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作范围设置的二级界面: building_ui 点「设置工作范围」跳到这里 (工业/农业管理方块),
 * 填两个点坐标 → 「保存」写进管理方块的 work_pos, NPC 的工作范围就是这两点围出的长方体.
 *
 * <p>输入方式: 手持木棍 左键/右键 方块, 聊天栏会显示坐标 (tool_ 的选点), 照着填进
 * 两个点的 X/Y/Z 输入框 (共 6 个, 数字模式 ±30,000,000 含负坐标)。</p>
 *
 * <p>会话来自 {@link #SESSIONS} (服务端, 点入口按钮时写入管理方块坐标);
 * 输入内容经 LDLib2 的 stringC2S 绑定同步到服务端 ({@link #INPUT}),
 * 模式与 buildlib_config_gui 的加钱输入框一致。</p>
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class work_range_ui {

    public static final ResourceLocation UI_ID =
            ResourceLocation.fromNamespaceAndPath("simulation_town", "work_range");

    /** 会话: playerUuid → 管理方块坐标 (building_ui 入口按钮写入) */
    public static final Map<UUID, BlockPos> SESSIONS = new ConcurrentHashMap<>();
    /** 输入中的两点: playerUuid → [x1,y1,z1,x2,y2,z2] (6 个 TextField C2S 绑定写入, 服务端读) */
    private static final Map<UUID, String[]> INPUT = new ConcurrentHashMap<>();
    /** 操作反馈 (界面底部一行): playerUuid → 文本 */
    private static final Map<UUID, Component> FEEDBACK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    /** building_ui 调: 记下管理方块坐标并打开本界面 */
    public static void open(ServerPlayer sp, BlockPos managerPos) {
        SESSIONS.put(sp.getUUID(), managerPos);
        INPUT.remove(sp.getUUID());
        FEEDBACK.remove(sp.getUUID());
        PlayerUIMenuType.openUI(sp, UI_ID);
    }

    // ==================== 数据读写 (全部服务端) ====================

    /** 当前管理方块里存的两点 (没会话/方块没了/没设置过 → null) */
    private static BlockPos[] currentPoints(Player player) {
        BlockPos pos = SESSIONS.get(player.getUUID());
        if (pos == null) return new BlockPos[2];
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof industry_block_entity i) {
            return new BlockPos[]{i.get_i_work_pos_first(), i.get_i_work_pos_end()};
        }
        if (be instanceof agriculture_block_entity a) {
            return new BlockPos[]{a.get_a_work_pos_first(), a.get_a_work_pos_end()};
        }
        return new BlockPos[2];
    }

    /** 写两点进管理方块 (p1/p2 传 null = 清除); 不是工业/农业方块返回 false */
    private static boolean writePoints(ServerLevel level, BlockPos pos, BlockPos p1, BlockPos p2) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof industry_block_entity i) {
            i.set_i_work_pos_first(p1);
            i.set_i_work_pos_end(p2);
            return true;
        }
        if (be instanceof agriculture_block_entity a) {
            a.set_a_work_pos_first(p1);
            a.set_a_work_pos_end(p2);
            return true;
        }
        return false;
    }

    /** 写一条操作反馈 (界面底部一行, S2C 推给客户端) */
    private static void feedback(Player player, Component msg) {
        FEEDBACK.put(player.getUUID(), msg);
    }

    /** 解析单个坐标轴: 非空白整数才算成功 (TextField 是数字模式, 但仍校验兜底) */
    private static Integer parseAxis(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 解析一个点的三个轴, 任一缺失返回 null */
    private static BlockPos parsePoint(String[] in, int base) {
        Integer x = parseAxis(in[base]), y = parseAxis(in[base + 1]), z = parseAxis(in[base + 2]);
        return (x == null || y == null || z == null) ? null : new BlockPos(x, y, z);
    }

    // ==================== 界面 ====================

    private static ModularUI build(Player player) {
        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(232).height(154).paddingAll(8).gapAll(3));

        // 标题
        Label title = new Label();
        title.layout(l -> l.height(14));
        title.bind(DataBindingBuilder.componentS2C(() ->
                Component.literal("设置工作范围")).build());
        root.addChild(title);

        // 当前已存的范围 (S2C: 客户端 BE 是空的, 由服务端算好推过来)
        Label status = new Label();
        status.layout(l -> l.height(11));
        status.bind(DataBindingBuilder.componentS2C(() -> {
            BlockPos[] pts = currentPoints(player);
            if (pts[0] == null || pts[1] == null) {
                return Component.literal("当前范围：未设置").withStyle(ChatFormatting.GRAY);
            }
            return Component.literal("当前范围：" + pts[0].toShortString()
                    + " ~ " + pts[1].toShortString());
        }).build());
        root.addChild(status);

        // 隐藏标签: 服务端把已存两点拼成 "x1,y1,z1,x2,y2,z2" 推过来, 「显示范围」按钮客户端解析
        Label hidden = new Label();
        hidden.setVisible(false);
        hidden.bind(DataBindingBuilder.componentS2C(() -> {
            BlockPos[] pts = currentPoints(player);
            if (pts[0] == null || pts[1] == null) return Component.literal("");
            return Component.literal(pts[0].getX() + "," + pts[0].getY() + "," + pts[0].getZ() + ","
                    + pts[1].getX() + "," + pts[1].getY() + "," + pts[1].getZ());
        }).build());
        root.addChild(hidden);

        // 坐标轴表头: 与下方输入框对齐 (每个 flexGrow(1) 跟字段同宽)
        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).height(11).gapAll(3));
        Label axisPad = new Label();
        axisPad.layout(l -> l.width(30).height(11));
        header.addChild(axisPad);
        for (String axis : new String[]{"X", "Y", "Z"}) {
            Label axisLabel = new Label();
            axisLabel.layout(l -> l.flexGrow(1).height(11));
            String a = axis;
            axisLabel.bind(DataBindingBuilder.componentS2C(() ->
                    Component.literal(a).withStyle(ChatFormatting.GRAY)).build());
            header.addChild(axisLabel);
        }
        root.addChild(header);

        // 点1 / 点2 输入行 (每点 x/y/z 三个输入框)
        root.addChild(pointRow(player, 0, "点1"));
        root.addChild(pointRow(player, 3, "点2"));

        // 提示: 木棍取坐标
        Label hint = new Label();
        hint.layout(l -> l.height(11));
        hint.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "手持木棍左/右键方块，聊天栏会显示坐标")
                .withStyle(ChatFormatting.GRAY)).build());
        root.addChild(hint);

        // 操作反馈
        Label feedback = new Label();
        feedback.layout(l -> l.height(11));
        feedback.bind(DataBindingBuilder.componentS2C(() -> FEEDBACK.containsKey(player.getUUID())
                ? FEEDBACK.get(player.getUUID())
                : Component.literal("")).build());
        root.addChild(feedback);

        // 页脚: 保存 / 显示 / 清除 / 返回
        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.ROW).height(18).gapAll(4));
        root.addChild(footer);

        Button btnSave = new Button();
        btnSave.setText("保存");
        btnSave.layout(l -> l.width(50).height(18));
        btnSave.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            BlockPos pos = SESSIONS.get(sp.getUUID());
            if (pos == null || !(sp.level() instanceof ServerLevel level)) {
                feedback(sp, Component.literal("没有待设置的建筑，从建筑界面重新进入")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            String[] in = INPUT.get(sp.getUUID());
            BlockPos p1 = in == null ? null : parsePoint(in, 0);
            BlockPos p2 = in == null ? null : parsePoint(in, 3);
            if (p1 == null || p2 == null) {
                feedback(sp, Component.literal("两个点的 X/Y/Z 都要填")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            if (writePoints(level, pos, p1, p2)) {
                FEEDBACK.remove(sp.getUUID());   // 状态行会立刻显示新范围, 不用重复提示
            } else {
                feedback(sp, Component.literal("管理方块不见了，重新打开建筑界面")
                        .withStyle(ChatFormatting.RED));
            }
        });
        footer.addChild(btnSave);

        Button btnShow = new Button();
        btnShow.setText("显示范围");
        btnShow.layout(l -> l.width(62).height(18));
        // 纯客户端动作 (画线框), 所以 setOnClick: 读 S2C 推过来的隐藏标签
        btnShow.setOnClick(e -> {
            String s = hidden.getText().getString();
            if (s.isEmpty()) return;
            String[] p = s.split(",");
            if (p.length != 6) return;
            int[] c = new int[6];
            for (int i = 0; i < 6; i++) {
                try {
                    c[i] = Integer.parseInt(p[i].trim());
                } catch (NumberFormatException ex) {
                    return;
                }
            }
            BoundingBox box = new BoundingBox(
                    Math.min(c[0], c[3]), Math.min(c[1], c[4]), Math.min(c[2], c[5]),
                    Math.max(c[0], c[3]), Math.max(c[1], c[4]), Math.max(c[2], c[5]));
            BuildLibApi.showBuildingBounds(box, 10);
        });
        footer.addChild(btnShow);

        Button btnClear = new Button();
        btnClear.setText("清除");
        btnClear.layout(l -> l.width(50).height(18));
        btnClear.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            BlockPos pos = SESSIONS.get(sp.getUUID());
            if (pos == null || !(sp.level() instanceof ServerLevel level)) return;
            if (writePoints(level, pos, null, null)) {
                FEEDBACK.remove(sp.getUUID());
            } else {
                feedback(sp, Component.literal("管理方块不见了")
                        .withStyle(ChatFormatting.RED));
            }
        });
        footer.addChild(btnClear);

        Button btnBack = new Button();
        btnBack.setText("返回");
        btnBack.layout(l -> l.width(44).height(18));
        btnBack.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            BlockPos pos = SESSIONS.get(sp.getUUID());
            if (pos != null) {
                BlockUIMenuType.openUI(sp, pos);   // 回建筑界面 (上一级)
            } else {
                player.closeContainer();
            }
        });
        footer.addChild(btnBack);

        return ModularUI.of(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)),
                player);
    }

    /** 一行输入: 「点N」标签 + x/y/z 三个数字输入框 (C2S 绑定写进 INPUT[base..base+2]) */
    private static UIElement pointRow(Player player, int base, String name) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).height(16).gapAll(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));

        Label label = new Label();
        label.layout(l -> l.width(30).height(13));
        label.bind(DataBindingBuilder.componentS2C(() ->
                Component.literal(name)).build());
        row.addChild(label);

        for (int i = 0; i < 3; i++) {
            final int idx = base + i;
            TextField field = new TextField();
            field.layout(l -> l.flexGrow(1).height(16));
            // 数字模式, 范围 ±30,000,000 (含负坐标)
            field.setNumbersOnlyInt(-30_000_000, 30_000_000);
            field.bind(DataBindingBuilder.stringC2S(v -> {
                if (player == null) return;
                String[] in = INPUT.computeIfAbsent(player.getUUID(), k -> new String[6]);
                in[idx] = v;
            }).build());
            row.addChild(field);
        }
        return row;
    }

    // ------------------------------------------------------------------ 工厂

    private static final class Holder implements PlayerUIMenuType.PlayerUIHolder {

        private final Player player;

        private Holder(Player player) {
            this.player = player;
        }

        @Override
        public ModularUI createUI(Player player) {
            return build(player);
        }

        @Override
        public boolean isStillValid(Player player) {
            return true;   // 导航全靠按钮 (保存后手动「返回」)
        }
    }
}
