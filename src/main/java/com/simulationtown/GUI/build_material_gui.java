package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.buildlib.api.NpcBuildApi;
import com.simulationtown.NPC.NPC;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 建造材料清单界面 (builder 职业 NPC): 列出这个 NPC 到完工还需要什么材料.
 *
 * <p>数据实时取自 {@link NpcBuildApi#remainingMaterials} — 口径和 NPC 实际扣料一致,
 * 管理方块/免扣材料已过滤, 放掉的不算, 所以这里看到的"还差 N 个"就是真实缺口.
 * 每页 6 行, 数量多的排前面. 图标/文字全部 itemStackS2C / componentS2C 服务端现算.</p>
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class build_material_gui {

    public static final ResourceLocation UI_ID =
            ResourceLocation.fromNamespaceAndPath("simulation_town", "build_material");

    private static final int PAGE_SIZE = 6;

    /** 正在查看哪个 NPC 的材料单: playerUuid → npcUuid */
    private static final Map<UUID, UUID> WATCHING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PAGE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    // ------------------------------------------------------------------ 入口

    /** 返回 false = 没有 {@code npc} / 没有进行中的建造任务 (调用方给提示即可) */
    public static boolean open(ServerPlayer sp, NPC npc) {
        if (npc == null || NpcBuildApi.buildingId(npc) == null) return false;
        WATCHING.put(sp.getUUID(), npc.getUUID());
        PAGE.put(sp.getUUID(), 0);
        PlayerUIMenuType.openUI(sp, UI_ID);
        return true;
    }

    // ------------------------------------------------------------------ 查单

    private static NPC target(Player player) {
        UUID uuid = WATCHING.get(player.getUUID());
        if (uuid == null) return null;
        // 和 npc_GUI.find 同思路: 附近扫 (材料界面对着工地看, NPC 就在旁边)
        for (NPC npc : player.level().getEntitiesOfClass(NPC.class, player.getBoundingBox().inflate(128.0D))) {
            if (npc.getUUID().equals(uuid)) return npc;
        }
        return null;
    }

    private static List<NpcBuildApi.MaterialNeed> needs(Player player) {
        NPC npc = target(player);
        return npc == null ? List.of() : NpcBuildApi.remainingMaterials(npc);
    }

    // ------------------------------------------------------------------ 界面

    private static ModularUI build(Player player) {
        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(248).height(196).paddingAll(8).gapAll(3));

        // 标题: 建筑 + 进度
        Label title = new Label();
        title.layout(l -> l.height(13));
        title.bind(DataBindingBuilder.componentS2C(() -> {
            NPC npc = target(player);
            String bid = npc == null ? null : NpcBuildApi.buildingId(npc);
            if (bid == null) return Component.literal("没有进行中的建造").withStyle(ChatFormatting.GRAY);
            double p = NpcBuildApi.progress(npc);
            return Component.literal("建造材料 — " + building_ui.shortName(bid)
                    + "（" + Math.round(p * 100) + "%）");
        }).build());
        root.addChild(title);

        // 说明行
        Label hint = new Label();
        hint.layout(l -> l.height(11));
        hint.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "以下是到完工还缺的材料（装修类已免）").withStyle(ChatFormatting.GRAY)).build());
        root.addChild(hint);

        // 材料 6 行: 图标 + 名字 ×剩余
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int idx = i;
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).height(20).gapAll(4)
                    .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));

            ItemSlot icon = new ItemSlot();
            icon.layout(l -> l.width(18).height(18));
            icon.bind(DataBindingBuilder.itemStackS2C(() -> {
                List<NpcBuildApi.MaterialNeed> list = needs(player);
                int gi = PAGE.getOrDefault(player.getUUID(), 0) * PAGE_SIZE + idx;
                return gi < list.size()
                        ? new ItemStack(list.get(gi).item())
                        : ItemStack.EMPTY;
            }).build());
            row.addChild(icon);

            Label desc = new Label();
            desc.layout(l -> l.flexGrow(1).height(13));
            desc.bind(DataBindingBuilder.componentS2C(() -> {
                List<NpcBuildApi.MaterialNeed> list = needs(player);
                int gi = PAGE.getOrDefault(player.getUUID(), 0) * PAGE_SIZE + idx;
                if (gi >= list.size()) return Component.literal("");
                NpcBuildApi.MaterialNeed m = list.get(gi);
                // 可互换类显示 "木板（任意一种）", 图标只是代表物品
                String name = m.group() != null
                        ? m.group() + "（任意一种）"
                        : new ItemStack(m.item()).getHoverName().getString();
                return Component.literal(name + "  还差 " + m.count());
            }).build());
            row.addChild(desc);

            root.addChild(row);
        }

        // 底行: 翻页 + 关闭
        UIElement foot = new UIElement();
        foot.layout(l -> l.flexDirection(FlexDirection.ROW).height(18).gapAll(4).marginAll(2));
        root.addChild(foot);

        Button prev = new Button();
        prev.setText("<");
        prev.layout(l -> l.width(24).height(16));
        prev.setOnServerClick(e -> {
            if (player instanceof ServerPlayer sp) {
                PAGE.computeIfPresent(sp.getUUID(), (k, v) -> Math.max(0, v - 1));
            }
        });
        foot.addChild(prev);

        Label pageInfo = new Label();
        pageInfo.layout(l -> l.flexGrow(1).height(13));
        pageInfo.bind(DataBindingBuilder.componentS2C(() -> {
            int total = needs(player).size();
            int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
            return Component.literal((PAGE.getOrDefault(player.getUUID(), 0) + 1) + "/" + pages
                    + " 页，共 " + total + " 种材料");
        }).build());
        foot.addChild(pageInfo);

        Button next = new Button();
        next.setText(">");
        next.layout(l -> l.width(24).height(16));
        next.setOnServerClick(e -> {
            if (player instanceof ServerPlayer sp) {
                int maxPage = Math.max(0, (needs(sp).size() + PAGE_SIZE - 1) / PAGE_SIZE - 1);
                PAGE.computeIfPresent(sp.getUUID(), (k, v) -> Math.min(maxPage, v + 1));
            }
        });
        foot.addChild(next);

        Button close = new Button();
        close.setText("关闭");
        close.layout(l -> l.width(48).height(16));
        close.setOnClick(e -> player.closeContainer());
        foot.addChild(close);

        return ModularUI.of(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)), player);
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
            NPC npc = target(player);
            return npc != null && npc.isAlive() && NpcBuildApi.buildingId(npc) != null
                    && npc.distanceToSqr(player) < 64.0D * 64.0D;
        }
    }
}
