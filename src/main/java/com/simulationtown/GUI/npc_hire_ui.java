package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simulationtown.building.BuildingData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 建筑雇佣的二级选人界面：building_ui 点「雇佣建造」跳到这里，列出空闲 NPC，
 * 选中 → 「确认雇佣」指派并返回建筑界面；「返回」不指派直接回上一级。
 *
 * <p>候选列表来自 {@link building_ui#HIRE_SESSIONS}（服务端会话，点雇佣建造时写入）。
 * LDLib2 控件树两端各建一份、只同步数据绑定，所以列表用"固定槽位 + S2C 绑定"渲染：
 * 槽位比候选多就整行隐藏（同 build_block_GUI 卡片分页的做法），一页放不下的翻页看。</p>
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class npc_hire_ui {

    /** 每页槽位数 */
    private static final int PAGE_SIZE = 6;

    public static final net.minecraft.resources.ResourceLocation UI_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("simulation_town", "npc_hire");

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    /** 每次现取选人会话：点「选 TA」会替换会话对象，捕获旧引用会读到过期状态。 */
    private static building_ui.HireSession session(Player player) {
        return building_ui.HIRE_SESSIONS.get(player.getUUID());
    }

    private static ModularUI build(Player player) {
        final int[] page = {0};   // 服务端这份才是有效的（绑定 supplier / 按钮点击都跑服务端）

        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(248).height(172).paddingAll(8).gapAll(3));

        // 标题
        Label title = new Label();
        title.layout(l -> l.height(14));
        title.bind(DataBindingBuilder.componentS2C(() -> {
            building_ui.HireSession session = session(player);
            if (session == null) return Component.literal("没有待选的建造任务");
            int total = (session.candidates().size() + PAGE_SIZE - 1) / PAGE_SIZE;
            String verb = session.homeMode() ? "选个 NPC 住进来"
                    : session.shopMode() ? "选个 NPC 当店员" : "选个 NPC 来盖";
            return Component.literal(verb + "（" + (page[0] + 1) + "/" + total + " 页）");
        }).build());
        root.addChild(title);

        // 已选状态
        Label picked = new Label();
        picked.layout(l -> l.height(11));
        picked.bind(DataBindingBuilder.componentS2C(() -> {
            building_ui.HireSession session = session(player);
            if (session == null || session.selected() < 0
                    || session.selected() >= session.candidates().size()) {
                return Component.literal("未选择");
            }
            return Component.literal("已选 " + session.candidates().get(session.selected()));
        }).build());
        root.addChild(picked);

        // 固定 PAGE_SIZE 个候选槽：名字 + 「选 TA」按钮；超出候选数的整行隐藏
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int slot = i;

            Button pick = new Button();
            pick.setText("选 TA");
            pick.layout(l -> l.width(40).height(14));
            pick.setOnServerClick(e -> {
                if (!(player instanceof ServerPlayer sp)) return;
                building_ui.HireSession s = session(sp);
                if (s == null) return;
                int global = page[0] * PAGE_SIZE + slot;
                if (global >= s.candidates().size()) return;
                building_ui.HIRE_SESSIONS.put(sp.getUUID(), new building_ui.HireSession(
                        s.buildingPos(), s.candidates(), global, s.shopMode(), s.homeMode()));
            });

            Label name = new Label();
            name.layout(l -> l.flexGrow(1).height(14));
            name.bind(DataBindingBuilder.componentS2C(() -> {
                building_ui.HireSession s = session(player);
                if (s == null) return Component.literal("");
                int global = page[0] * PAGE_SIZE + slot;
                if (global >= s.candidates().size()) return Component.literal("");
                String n = s.candidates().get(global);
                return Component.literal((s.selected() == global ? "✓ " : "") + n);
            }).build());

            // 整行显隐：服务端算 "1"/"0"，客户端收到后 setVisible（同 build_block_GUI 卡片）
            Label vis = new Label();
            vis.setVisible(false);
            vis.bind(DataBindingBuilder.componentS2C(() -> {
                building_ui.HireSession s = session(player);
                if (s == null) return Component.literal("0");
                int global = page[0] * PAGE_SIZE + slot;
                return Component.literal(global < s.candidates().size() ? "1" : "0");
            }).onRemoteSyncReceived(c -> {
                boolean show = "1".equals(c.getString());
                name.setVisible(show);
                pick.setVisible(show);
            }).build());

            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).height(14).gapAll(4));
            row.addChildren(vis, name, pick);
            root.addChild(row);
        }

        // 页脚：翻页 + 确认 + 返回
        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.ROW).height(18).gapAll(4));
        root.addChild(footer);

        Button btnPrev = new Button();
        btnPrev.setText("<");
        btnPrev.layout(l -> l.width(20).height(18));
        btnPrev.setOnServerClick(e -> {
            if (session(player) != null) page[0] = Math.max(0, page[0] - 1);
        });
        footer.addChild(btnPrev);

        Button btnNext = new Button();
        btnNext.setText(">");
        btnNext.layout(l -> l.width(20).height(18));
        btnNext.setOnServerClick(e -> {
            building_ui.HireSession s = session(player);
            if (s == null) return;
            int total = (s.candidates().size() + PAGE_SIZE - 1) / PAGE_SIZE;
            page[0] = Math.min(total - 1, page[0] + 1);
        });
        footer.addChild(btnNext);

        Button btnConfirm = new Button();
        btnConfirm.setText("确认");   // 盖楼/店员/住宅三种模式通用
        btnConfirm.layout(l -> l.width(66).height(18));
        btnConfirm.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            building_ui.HireSession s = session(sp);
            if (s == null) return;
            if (s.selected() < 0 || s.selected() >= s.candidates().size()) {
                sp.sendSystemMessage(Component.literal(
                        "先选一个 NPC").withStyle(ChatFormatting.YELLOW));
                return;
            }
            String name = s.candidates().get(s.selected());
            BlockPos pos = s.buildingPos();
            if (!(sp.level() instanceof ServerLevel level)
                    || !(level.getBlockEntity(pos) instanceof BlockEntity be)
                    || !(be instanceof BuildingData data)) {
                sp.sendSystemMessage(Component.literal(
                        "建筑不见了，重新点雇佣建造").withStyle(ChatFormatting.RED));
                building_ui.HIRE_SESSIONS.remove(sp.getUUID());
                return;
            }
            // 失败（人不在/被别处抢走）保留会话，可以换人再确认；成功 hire 内部清会话
            // 店员模式: 不开建造任务, 直接上岗 SHOPER; 住宅模式: 选谁搬进来
            boolean ok;
            if (s.homeMode()) {
                ok = building_ui.assignHome(sp, level, be, name);
            } else if (s.shopMode()) {
                ok = building_ui.hireShopkeeper(sp, level, be, data, name);
            } else {
                ok = building_ui.hire(sp, level, be, data, name);
            }
            if (ok) {
                building_ui.HIRE_SESSIONS.remove(sp.getUUID());
                BlockUIMenuType.openUI(sp, pos);       // 回到建筑界面（上一级），当前界面自动被替换
            }
        });
        footer.addChild(btnConfirm);

        Button btnBack = new Button();
        btnBack.setText("返回");
        btnBack.layout(l -> l.width(44).height(18));
        btnBack.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            building_ui.HireSession s = session(sp);
            if (s != null) {
                BlockUIMenuType.openUI(sp, s.buildingPos());   // 不指派，直接回上一级
            } else {
                player.closeContainer();
            }
        });
        footer.addChild(btnBack);

        return ModularUI.of(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)),
                player);
    }

    /** 玩家级 UI holder（同 npc_GUI 的做法，NPC 列表靠静态会话查）。 */
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
            return true;   // 导航全靠按钮（确认/返回都会主动换界面）
        }
    }
}
