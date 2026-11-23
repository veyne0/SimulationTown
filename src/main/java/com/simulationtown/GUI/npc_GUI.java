package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.buildlib.api.NpcBuildApi;
import com.simulationtown.NPC.NPC;
import com.simulationtown.SimulationTown;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.UUID;

/**
 * NPC 右键界面。所有界面代码都在这里，实体类只留一行调用 {@link #onInteract}。
 *
 * <p>LDLib2 没有"实体 UI"这种工厂，只有方块 / 手持物 / 玩家三种。
 * 所以这里走玩家的 UI 工厂（{@link PlayerUIMenuType}）：
 * 右键时把"我在跟哪个 NPC 说话"写进玩家附件（{@link SimulationTown#NPC_TALKING}），
 * 附件是同步的，客户端也读得到，界面两边就都能找到同一个 NPC。</p>
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class npc_GUI {

    /** 这个界面的注册 id，客户端/服务端靠它找到同一份界面工厂。 */
    public static final ResourceLocation UI_ID =
            ResourceLocation.fromNamespaceAndPath("simulation_town", "npc");

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 玩家级 UI：工厂只拿到 player，NPC 由附件查回来
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    // ------------------------------------------------------------------ 入口

    /** 实体那边只需要调这一行：{@code return npc_GUI.onInteract(this, player, hand);} */
    public static InteractionResult onInteract(NPC npc, Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSpectator() || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        // 商店职业的 NPC: 直接进商店界面 (进不去再回落到 NPC 信息界面)
        if ("SHOPER".equals(npc.job) && shop_gui.open(sp, npc)) {
            sp.setData(SimulationTown.NPC_TALKING, npc.getUUID());
            return InteractionResult.SUCCESS;
        }
        // 先记下"我在跟谁说话"，再开界面（附件会同步到客户端）
        sp.setData(SimulationTown.NPC_TALKING, npc.getUUID());
        PlayerUIMenuType.openUI(sp, UI_ID);
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------ 查人

    /** 从玩家身上找回当前交谈的 NPC（服务端、客户端都能用）。 */
    public static NPC find(Player player) {
        UUID uuid = player.getExistingDataOrNull(SimulationTown.NPC_TALKING);
        if (uuid == null) {
            return null;
        }
        // Level 只有 getEntity(int)，按 UUID 找就在附近扫一遍 NPC（数量很少，代价可忽略）
        for (NPC candidate : player.level().getEntitiesOfClass(NPC.class, player.getBoundingBox().inflate(128.0D))) {
            if (candidate.getUUID().equals(uuid)) {
                return candidate;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 界面

    private static ModularUI build(Player player) {
        final NPC npc = find(player);

        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(248).height(156).paddingAll(8).gapAll(4));

        // 标题：NPC 的名字
        Label title = new Label();
        title.layout(l -> l.height(14));
        title.bind(DataBindingBuilder.componentS2C(
                () -> npc == null ? Component.literal("（找不到这个 NPC）") : npc.getName()).build());
        root.addChild(title);

        // 职业
        Label job = new Label();
        job.layout(l -> l.height(11));
        job.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal("职业：" + jobText(npc))).build());
        root.addChild(job);

        // 背包占用
        Label bag = new Label();
        bag.layout(l -> l.height(11));
        bag.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(bagText(npc))).build());
        root.addChild(bag);

        // 皮肤编号（方便调试随机皮肤）
        Label skin = new Label();
        skin.layout(l -> l.height(11));
        skin.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal("皮肤：" + (npc == null ? "-" : "#" + npc.getSkinIndex()))).build());
        root.addChild(skin);

        // 建造任务（buildlib 的 NpcBuildApi；没有进行中任务时 progress 返回 -1）
        Label build = new Label();
        build.layout(l -> l.height(11));
        build.bind(DataBindingBuilder.componentS2C(() -> {
            if (npc == null) return Component.literal("建造：-");
            String bid = NpcBuildApi.buildingId(npc);
            if (bid == null) return Component.literal("建造：空闲");
            double p = NpcBuildApi.progress(npc);
            return Component.literal("建造：" + building_ui.shortName(bid) + "（" + Math.round(p * 100) + "%）");
        }).build());
        root.addChild(build);

        // 施工/停工状态：缺料时显示缺的是什么
        Label buildState = new Label();
        buildState.layout(l -> l.height(11));
        buildState.bind(DataBindingBuilder.componentS2C(() -> {
            if (npc == null || NpcBuildApi.buildingId(npc) == null) return Component.literal("");
            if (NpcBuildApi.isStuck(npc)) {
                // 可互换类显示 "木板"; 精确物品显示物品名
                String label = NpcBuildApi.missingLabel(npc);
                if (label == null) {
                    Item miss = NpcBuildApi.missingItem(npc);
                    label = miss == null ? "?" : new ItemStack(miss).getHoverName().getString();
                }
                return Component.literal("⚠ 缺料停工：" + label);
            }
            return Component.literal("施工中");
        }).build());
        root.addChild(buildState);

        // 按钮行：打开背包 / 关闭
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).height(20).gapAll(6).marginAll(4));
        root.addChild(row);

        Button openBag = new Button();
        openBag.setText("打开背包");
        openBag.layout(l -> l.width(84).height(18));
        openBag.setOnServerClick(e -> {
            if (npc != null && player instanceof ServerPlayer sp) {
                npc.openInventoryFor(sp);   // 换成箱子界面
            }
        });
        row.addChild(openBag);

        // 建造职业 (job 里带 builder) 才有: 查看他到完工还缺什么材料
        // job 必须读 getJobSynced (watcher 同步): 这个判断在客户端构建界面时也执行,
        // 裸字段客户端拿不到 → 两边结构不一致, 按钮就显示不出来
        boolean isBuilder = npc != null && npc.getJobSynced().toLowerCase().contains("builder");
        if (isBuilder) {
            Button materials = new Button();
            materials.setText("建筑材料");
            materials.layout(l -> l.width(64).height(18));
            materials.setOnServerClick(e -> {
                if (player instanceof ServerPlayer sp) {
                    if (!build_material_gui.open(sp, npc)) {
                        sp.sendSystemMessage(Component.literal(
                                "这个 NPC 没有进行中的建造").withStyle(net.minecraft.ChatFormatting.RED));
                    }
                }
            });
            row.addChild(materials);
        }

        Button close = new Button();
        close.setText("关闭");
        close.layout(l -> l.width(56).height(18));
        close.setOnClick(e -> player.closeContainer());
        row.addChild(close);

        return ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), player);
    }

    private static String jobText(NPC npc) {
        if (npc == null) {
            return "-";
        }
        return npc.job == null || npc.job.isEmpty() ? "无" : npc.job;
    }

    private static String bagText(NPC npc) {
        if (npc == null) {
            return "背包：-";
        }
        int used = 0;
        int total = 0;
        for (int i = 0; i < npc.getNpcInventory().getContainerSize(); i++) {
            ItemStack stack = npc.getNpcInventory().getItem(i);
            if (!stack.isEmpty()) {
                used++;
                total += stack.getCount();
            }
        }
        return "背包：" + used + " / " + npc.getNpcInventory().getContainerSize() + " 格（" + total + " 件）";
    }

    // ------------------------------------------------------------------ 工厂

    /** 玩家级 UI 的 holder：拿到的只有 player，NPC 靠附件查。 */
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
            NPC npc = find(player);
            return npc != null && npc.isAlive() && npc.distanceToSqr(player) < 64.0D * 64.0D;
        }
    }
}
