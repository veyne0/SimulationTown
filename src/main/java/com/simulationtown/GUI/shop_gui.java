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
import com.simulationtown.SimulationTown;
import dev.vfyjxf.taffy.style.FlexDirection;
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
 * 商店界面 (SHOPER 职业 NPC): 右键商店 NPC 打开, 列出伴生 json goods 里的商品, 点购买扣钱发货.
 *
 * <p>打开时服务端把货架读进会话 ({@link #SESSIONS}), 商品图标用 itemStackS2C 推给客户端,
 * 价格/按钮走 componentS2C + setOnServerClick — 和 npc_hire_ui 同一套模式.</p>
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class shop_gui {

    public static final ResourceLocation UI_ID =
            ResourceLocation.fromNamespaceAndPath("simulation_town", "npc_shop");

    private static final int PAGE_SIZE = 6;   // 每页商品数

    /** 打开商店时的货架快照: playerUuid → (商店 NPC 的 buildingId, 商品列表, 当前页) */
    private record Shelf(String buildingId, List<NpcBuildApi.ShopGood> goods) {}
    private static final Map<UUID, Shelf> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PAGE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    // ------------------------------------------------------------------ 入口

    /** 右键 SHOPER NPC 时调 (npc_GUI 分流进来); 返回 false = 这个 NPC 不是可用的商店 */
    public static boolean open(ServerPlayer sp, NPC npc) {
        if (npc.shopBuildingId == null || npc.shopBuildingId.isEmpty()) return false;
        List<NpcBuildApi.ShopGood> goods = NpcBuildApi.goodsFor(npc.shopBuildingId);
        if (goods.isEmpty()) {
            sp.sendSystemMessage(Component.literal(
                    "这个商店没有上架商品（检查 " + npc.shopBuildingId + ".json 的 goods）"));
            return true;   // 是商店只是没货, 不回落到背包界面
        }
        SESSIONS.put(sp.getUUID(), new Shelf(npc.shopBuildingId, goods));
        PAGE.put(sp.getUUID(), 0);
        PlayerUIMenuType.openUI(sp, UI_ID);
        return true;
    }

    // ------------------------------------------------------------------ 界面

    private static Shelf shelf(Player player) {
        Shelf s = SESSIONS.get(player.getUUID());
        return s == null ? new Shelf("", List.of()) : s;
    }

    private static ModularUI build(Player player) {
        Shelf shelf = shelf(player);
        int pages = Math.max(1, (shelf.goods().size() + PAGE_SIZE - 1) / PAGE_SIZE);

        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(248).height(196).paddingAll(8).gapAll(3));

        // 标题: 商店名 (建筑短名)
        Label title = new Label();
        title.layout(l -> l.height(13));
        title.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "商店：" + building_ui.shortName(shelf.buildingId()))).build());
        root.addChild(title);

        // 玩家资金 (double, 整数不带小数点)
        Label money = new Label();
        money.layout(l -> l.height(11));
        money.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "你的资金：" + fmt(player.getData(SimulationTown.PLAYER_JINGJI)))).build());
        root.addChild(money);

        // 商品列表: 6 行 (图标 + 名字×数量 + 价格 + 购买)
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int idx = i;
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).height(20).gapAll(4)
                    .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));

            // 图标 (phantom: 只展示拿不走)
            ItemSlot icon = new ItemSlot();
            icon.layout(l -> l.width(18).height(18));
            icon.bind(DataBindingBuilder.itemStackS2C(() -> {
                Shelf s = shelf(player);
                int page = PAGE.getOrDefault(player.getUUID(), 0);
                int gi = page * PAGE_SIZE + idx;
                return gi < s.goods().size() ? s.goods().get(gi).stack() : ItemStack.EMPTY;
            }).build());
            row.addChild(icon);

            // 名字 ×数量  价格
            Label desc = new Label();
            desc.layout(l -> l.flexGrow(1).height(13));
            desc.bind(DataBindingBuilder.componentS2C(() -> {
                Shelf s = shelf(player);
                int page = PAGE.getOrDefault(player.getUUID(), 0);
                int gi = page * PAGE_SIZE + idx;
                if (gi >= s.goods().size()) return Component.literal("");
                NpcBuildApi.ShopGood g = s.goods().get(gi);
                return Component.literal(g.stack().getHoverName().getString()
                        + " ×" + g.stack().getCount() + "  " + fmt(g.price()) + "元");
            }).build());
            row.addChild(desc);

            // 购买按钮
            Button buy = new Button();
            buy.setText("购买");
            buy.layout(l -> l.width(48).height(16));
            buy.setOnServerClick(e -> {
                if (!(player instanceof ServerPlayer sp)) return;
                Shelf s = shelf(sp);
                int gi = PAGE.getOrDefault(sp.getUUID(), 0) * PAGE_SIZE + idx;
                if (gi >= s.goods().size()) return;
                buy(sp, s.goods().get(gi));
            });
            row.addChild(buy);

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
        pageInfo.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                (PAGE.getOrDefault(player.getUUID(), 0) + 1) + "/" + pages + " 页，共 "
                        + shelf.goods().size() + " 种商品")).build());
        foot.addChild(pageInfo);

        Button next = new Button();
        next.setText(">");
        next.layout(l -> l.width(24).height(16));
        next.setOnServerClick(e -> {
            if (player instanceof ServerPlayer sp) {
                int maxPage = Math.max(0, (shelf(sp).goods().size() + PAGE_SIZE - 1) / PAGE_SIZE - 1);
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

    /** 服务端购买: 扣钱 → 整组发货 (背包满了掉地上); 价格 double 支持小数 */
    private static void buy(ServerPlayer sp, NpcBuildApi.ShopGood good) {
        double price = good.price();
        double money = sp.getData(SimulationTown.PLAYER_JINGJI);
        if (money < price) {
            sp.sendSystemMessage(Component.literal(
                    "资金不足：需要 " + fmt(price) + "，你只有 " + fmt(money)).withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        sp.setData(SimulationTown.PLAYER_JINGJI, money - price);
        ItemStack stack = good.stack().copy();
        if (!sp.getInventory().add(stack) && !stack.isEmpty()) {
            sp.drop(stack, false);   // 背包满掉地上
        }
        sp.sendSystemMessage(Component.literal(
                "买下 " + good.stack().getHoverName().getString() + " ×" + good.stack().getCount()
                        + "，付了 " + fmt(price) + "元").withStyle(net.minecraft.ChatFormatting.GREEN));
    }

    /** 金额显示: 整数不带小数点, 小数保留 2 位 ("500" / "0.4") */
    private static String fmt(double v) {
        return v == Math.floor(v) && !Double.isInfinite(v)
                ? String.valueOf((long) v) : String.format("%.2f", v);
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
            return SESSIONS.containsKey(player.getUUID());
        }
    }
}
