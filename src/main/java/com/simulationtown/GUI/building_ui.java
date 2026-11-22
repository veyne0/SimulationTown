package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import com.buildlib.api.BuildLibApi;
import com.buildlib.api.NpcBuildApi;
import com.simulationtown.NPC.NPC;
import com.simulationtown.building.BuildingData;
import com.simulationtown.residence.residence_block_entity;
import com.simulationtown.industry.industry_block_entity;
import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.special.special_block_entity;
import com.simulationtown.towncenter.centerBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 建筑本体的界面。四个分类共用这一份，只有标题不一样。 */
public final class building_ui {

    private building_ui() {
    }

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder, String categoryName) {
        // 服务端这一份有数据；客户端这份是空的（我们没写 getUpdateTag），
        // 所以所有文字都用 S2C 绑定，由服务端算好推过来。
        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        final BuildingData data = be instanceof BuildingData d ? d : null;

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(260).height(170).paddingAll(8).gapAll(5));
        root.addClass("panel_bg");

        Label title = new Label();
        title.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(categoryName + "建筑")).build());

        Label state = new Label();
        state.bind(DataBindingBuilder.componentS2C(() -> {
            if (data == null) return Component.literal("状态：--");
            return Component.literal(data.isBuildFinished() ? "状态：已完成" : "状态：建造中");
        }).build());

        Label completion = new Label();
        completion.bind(DataBindingBuilder.componentS2C(() -> {
            if (data == null) return Component.literal("完成度：--");
            double v = Math.max(0.0, Math.min(1.0, data.getCompletionLevel()));
            return Component.literal("完成度：" + Math.round(v * 100) + "%");
        }).build());

        Label builder = new Label();
        builder.bind(DataBindingBuilder.componentS2C(() -> {
            if (data == null) return Component.literal("建造者：--");
            String n = data.getBuilderName();
            return Component.literal("建造者：" + (n == null || n.isEmpty() ? "无" : n));
        }).build());

        Label employ = new Label();
        employ.bind(DataBindingBuilder.componentS2C(() -> {
            if (data == null) return Component.literal("雇佣：--");
            String n = data.getEmployNpc();
            return Component.literal("雇佣：" + (n == null || n.isEmpty() ? "无" : n));
        }).build());

        // 范围不能用普通字段读：building_box 只在服务端有，客户端这份 BE 是空的。
        // 和 catLabel 一样，塞进一个隐藏 Label，由服务端 supplier 拼成 "minX,minY,minZ,maxX,maxY,maxZ" 推过来。
        Label boxLabel = new Label();
        boxLabel.bind(DataBindingBuilder.componentS2C(() -> {
            if (data == null) return Component.literal("");
            BoundingBox b = data.getBuildingBounds();
            if (b == null) return Component.literal("");
            return Component.literal(b.minX() + "," + b.minY() + "," + b.minZ() + ","
                    + b.maxX() + "," + b.maxY() + "," + b.maxZ());
        }).build());
        boxLabel.setVisible(false);

        // 显示范围：纯客户端（画线框），所以用 setOnClick
        Button btnShow = new Button();
        btnShow.setText("显示范围");
        btnShow.layout(l -> l.width(84).height(18));
        btnShow.setOnClick(e -> {
            String s = boxLabel.getText().getString();
            if (s.isEmpty()) return;
            String[] p = s.split(",");
            if (p.length != 6) return;
            BoundingBox box = new BoundingBox(
                    Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
            BuildLibApi.showBuildingBounds(box, 10);
        });

        // 拆除：改世界 + 改数据，必须在服务端，所以用 setOnServerClick
        Button btnDemolish = new Button();
        btnDemolish.setText("拆除");
        btnDemolish.layout(l -> l.width(60).height(18));
        btnDemolish.setOnServerClick(e -> {
            if (data == null || !(be.getLevel() instanceof ServerLevel level)) return;
            BoundingBox b = data.getBuildingBounds();
            if (b == null) return;              // 没绑定过的建筑没有范围，不拆
            BuildLibApi.demolishBuilding(level, b);
            // 方块被拆掉了，数据也得跟着归零，否则界面上还显示"已完成"
            data.setBuildingId("");
            data.setBuildingBounds(null);
            data.setBuildFinished(false);
            data.setCompletion(0.0);
            writeEmploy(be, null);              // 雇佣/建造者也清掉，拆了才能重新招人
        });

        // 雇佣建造：打开二级选人界面（候选 = 玩家 ALL_NPC 名单里在场且空闲的 NPC）。
        // LDLib2 控件树两端各建一份、只同步数据绑定，名单没法直接渲染成动态列表，
        // 所以候选列表放在 npc_hire_ui 里用"固定槽位 + S2C 绑定"的方式展示。
        Button btnHire = new Button();
        btnHire.setText("雇佣建造");
        btnHire.layout(l -> l.width(72).height(18));
        btnHire.setOnServerClick(e -> {
            if (data == null || !(be.getLevel() instanceof ServerLevel level)
                    || !(holder.player instanceof ServerPlayer sp)) return;

            String employed = data.getEmployNpc();
            if (employed != null && !employed.isEmpty()) {
                sp.sendSystemMessage(Component.literal(
                        "这里已有 " + employed + " 在建").withStyle(ChatFormatting.YELLOW));
                return;
            }
            String id = data.getBuildingId();
            BoundingBox b = data.getBuildingBounds();
            if (id == null || id.isEmpty() || b == null) {
                sp.sendSystemMessage(Component.literal(
                        "还没绑定蓝图，先预览建造一次").withStyle(ChatFormatting.RED));
                return;
            }

            List<String> idle = idleNpcs(sp, level);
            if (idle.isEmpty()) {
                sp.sendSystemMessage(Component.literal(
                        "没有空闲的 NPC").withStyle(ChatFormatting.RED));
                return;
            }
            HIRE_SESSIONS.put(sp.getUUID(), new HireSession(be.getBlockPos(), idle, -1));
            com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType.openUI(sp, npc_hire_ui.UI_ID);
        });

        UIElement buttons = new UIElement();
        buttons.layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(6).height(18));
        buttons.addChildren(btnShow, btnDemolish, btnHire);

        root.addChildren(title, state, completion, builder, employ, boxLabel, buttons);

        return ModularUI.of(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)),
                holder.player);
    }

    /**
     * 选人会话（按玩家记）：buildingPos = 要雇人的建筑；candidates = 空闲 NPC 名单；
     * selected = 当前选中的候选下标（-1 = 还没选）。
     */
    public record HireSession(BlockPos buildingPos, List<String> candidates, int selected) {}

    public static final Map<UUID, HireSession> HIRE_SESSIONS = new HashMap<>();

    /** 玩家名单里"在场且没有进行中建造任务"的 NPC 名字（按名单顺序）。 */
    public static List<String> idleNpcs(ServerPlayer sp, ServerLevel level) {
        List<String> out = new ArrayList<>();
        List<String> names = centerBlockEntity.get_all_NPC_name(sp);
        if (names == null) return out;
        for (String n : names) {
            UUID uuid = centerBlockEntity.get_all_NPC_uuid(sp, n);
            if (uuid == null) continue;
            if (level.getEntity(uuid) instanceof NPC npc && NpcBuildApi.progress(npc) < 0) {
                out.add(n);
            }
        }
        return out;
    }

    /** 把名字对应的 NPC 指派给这座建筑（owner = 当前玩家，缺料提示发给他）。失败返回 false。 */
    public static boolean hire(ServerPlayer sp, ServerLevel level, BlockEntity be,
                               BuildingData data, String name) {
        UUID uuid = centerBlockEntity.get_all_NPC_uuid(sp, name);
        if (uuid == null || !(level.getEntity(uuid) instanceof NPC npc)
                || NpcBuildApi.progress(npc) >= 0) {
            sp.sendSystemMessage(Component.literal(
                    "「" + name + "」正忙，换一个吧").withStyle(ChatFormatting.RED));
            return false;
        }
        if (NpcBuildApi.startBuild(npc, data, level, sp.getUUID()) == null) {
            sp.sendSystemMessage(Component.literal(
                    "指派失败，看下日志").withStyle(ChatFormatting.RED));
            return false;
        }
        HIRE_SESSIONS.remove(sp.getUUID());   // 成功才清会话
        writeEmploy(be, npc.getName().getString());
        sp.sendSystemMessage(Component.literal(
                name + " 开始建造 " + shortName(data.getBuildingId()))
                .withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** 建筑显示名: "residence/测试住宅" → "测试住宅" */
    public static String shortName(String buildingId) {
        if (buildingId == null) return "";
        int slash = buildingId.indexOf('/');
        return slash < 0 ? buildingId : buildingId.substring(slash + 1);
    }

    /**
     * 写雇佣/建造者名字（setEmployNpc 不在 BuildingData 接口上，按 4 个具体 BE 分发）。
     * name 传 null 清空。匹配不上任何分类 BE 就什么都不写。
     */
    private static void writeEmploy(BlockEntity be, String name) {
        if (be instanceof residence_block_entity r) {
            r.setEmployNpc(name);
            r.setBuilderName(name);
        } else if (be instanceof industry_block_entity i) {
            i.setEmployNpc(name);
            i.setBuilderName(name);
        } else if (be instanceof agriculture_block_entity a) {
            a.setEmployNpc(name);
            a.setBuilderName(name);
        } else if (be instanceof special_block_entity s) {
            s.setEmployNpc(name);
            s.setBuilderName(name);
        }
    }
}
