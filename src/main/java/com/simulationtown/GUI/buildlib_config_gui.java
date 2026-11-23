package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.simulationtown.SimulationTown;
import com.simulationtown.towncenter.centerBlockEntity;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BuildLib 配置界面 (/buildlib, 仅 OP): 建造速度 / 材料消耗开关 / 给自己加钱.
 *
 * <h2>配置本体</h2>
 * {@link #blocksPerSecond} 和 {@link #consumeMaterials} 是两个 public static volatile 字段,
 * buildlib 的 NpcBuildTask 直接读 (编译期引用主模组, 运行时同进程, 无需同步包).
 * 修改走 {@link Data} (SavedData, 随存档持久化, 重启不丢).
 *
 * <h2>加钱</h2>
 * 输入框 (TextField, 数字) + 确认按钮: 输入内容经 LDLib2 的 stringC2S 绑定同步到服务端
 * (MONEY_INPUT), 点确认时 OP 校验 → {@link centerBlockEntity#add_jinji} 加钱
 * (玩家没放小镇中心时直接加 PLAYER_JINGJI 附件, 效果一样).
 */
@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)
public class buildlib_config_gui {

    public static final ResourceLocation UI_ID =
            ResourceLocation.fromNamespaceAndPath("simulation_town", "buildlib_config");

    // ==================== 配置本体 (buildlib NpcBuildTask 直接读) ====================

    /** NPC 建造速度: 格/秒 (1~100, 默认 5). /buildlib 可改 */
    public static volatile double blocksPerSecond = 5.0;
    /** NPC 建造是否消耗材料 (默认消耗, 可改不消耗=凭空放置). /buildlib 可改 */
    public static volatile boolean consumeMaterials = true;

    public static final double DEFAULT_SPEED = 5.0;
    public static final double MAX_SPEED = 100.0;

    private static final String DATA_NAME = "simulation_town_buildlib";

    /** 配置持久化 (overworld SavedData, 随存档) */
    private static final class Data extends SavedData {
        double speed = DEFAULT_SPEED;
        boolean consume = true;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            if (tag.contains("speed")) d.speed = tag.getDouble("speed");
            d.consume = tag.getBoolean("consume");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putDouble("speed", speed);
            tag.putBoolean("consume", consume);
            return tag;
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Data::new, Data::load, null), DATA_NAME);
    }

    /** 打开界面时把存档配置刷进内存 (服务器启动后第一次改动前的兜底) */
    public static void loadFromDisk(MinecraftServer server) {
        Data d = data(server);
        blocksPerSecond = clampSpeed(d.speed);
        consumeMaterials = d.consume;
    }

    private static double clampSpeed(double v) {
        if (v < 1.0) return 1.0;
        return Math.min(v, MAX_SPEED);
    }

    private static void setSpeed(MinecraftServer server, double v) {
        blocksPerSecond = clampSpeed(v);
        Data d = data(server);
        d.speed = blocksPerSecond;
        d.setDirty();
    }

    private static void setConsume(MinecraftServer server, boolean consume) {
        consumeMaterials = consume;
        Data d = data(server);
        d.consume = consume;
        d.setDirty();
    }

    // ==================== 界面 ====================

    /** 输入中的加钱金额: playerUuid → 文本 (TextField C2S 绑定写入, 服务端读) */
    private static final Map<UUID, String> MONEY_INPUT = new ConcurrentHashMap<>();
    /** 操作反馈 (界面底部一行): playerUuid → 文本 */
    private static final Map<UUID, Component> FEEDBACK = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        PlayerUIMenuType.register(UI_ID, Holder::new);
    }

    /** 打开配置界面 (指令入口, 指令已验 OP; 界面内每次修改再验一遍) */
    public static void open(ServerPlayer sp) {
        if (sp.getServer() != null) loadFromDisk(sp.getServer());
        MONEY_INPUT.remove(sp.getUUID());
        FEEDBACK.remove(sp.getUUID());
        PlayerUIMenuType.openUI(sp, UI_ID);
    }

    private static boolean isOp(Player player) {
        return player.hasPermissions(2);
    }

    private static void feedback(Player player, Component msg) {
        FEEDBACK.put(player.getUUID(), msg);
    }

    private static void deny(Player player) {
        feedback(player, Component.literal("仅 OP 可修改").withStyle(ChatFormatting.RED));
    }

    /** 金额显示: 整数不带小数点 */
    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }

    /** 加钱: 有小镇中心走 centerBlockEntity.add_jinji, 没有直接加 PLAYER_JINGJI 附件 */
    private static void addMoney(ServerPlayer sp, double amount) {
        BlockPos centerPos = sp.getData(SimulationTown.TOWN_CENTER.get());
        if (centerPos != null && sp.level().getBlockEntity(centerPos) instanceof centerBlockEntity be) {
            be.add_jinji(sp, amount);
            return;
        }
        double money = sp.getData(SimulationTown.PLAYER_JINGJI.get());
        sp.setData(SimulationTown.PLAYER_JINGJI.get(), money + amount);
    }

    private static ModularUI build(Player player) {
        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(232).height(140).paddingAll(8).gapAll(3));

        // 标题
        Label title = new Label();
        title.layout(l -> l.height(13));
        title.bind(DataBindingBuilder.componentS2C(() ->
                Component.literal("BuildLib 服务器配置（仅 OP）")).build());
        root.addChild(title);

        // --- 建造速度 ---
        Label speedLabel = new Label();
        speedLabel.layout(l -> l.height(13));
        speedLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "建造速度: " + fmt(blocksPerSecond) + " 格/秒（1~100，默认 5）")).build());
        root.addChild(speedLabel);

        UIElement speedRow = new UIElement();
        speedRow.layout(l -> l.flexDirection(FlexDirection.ROW).height(16).gapAll(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        root.addChild(speedRow);

        for (int delta : new int[]{-10, -1, 1, 10}) {
            Button b = new Button();
            b.setText((delta > 0 ? "+" : "") + delta);
            b.layout(l -> l.width(30).height(16));
            b.setOnServerClick(e -> {
                if (player instanceof ServerPlayer sp && isOp(sp) && sp.getServer() != null) {
                    setSpeed(sp.getServer(), blocksPerSecond + delta);
                } else {
                    deny(player);
                }
            });
            speedRow.addChild(b);
        }

        // --- 材料消耗开关 ---
        UIElement consumeRow = new UIElement();
        consumeRow.layout(l -> l.flexDirection(FlexDirection.ROW).height(16).gapAll(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        root.addChild(consumeRow);

        Label consumeLabel = new Label();
        consumeLabel.layout(l -> l.flexGrow(1).height(13));
        consumeLabel.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "建造消耗材料: " + (consumeMaterials ? "开" : "关（凭空放置）"))).build());
        consumeRow.addChild(consumeLabel);

        Button toggle = new Button();
        toggle.setText("切换");
        toggle.layout(l -> l.width(40).height(16));
        toggle.setOnServerClick(e -> {
            if (player instanceof ServerPlayer sp && isOp(sp) && sp.getServer() != null) {
                setConsume(sp.getServer(), !consumeMaterials);
                feedback(sp, Component.literal(consumeMaterials ? "材料消耗: 开" : "材料消耗: 关")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                deny(player);
            }
        });
        consumeRow.addChild(toggle);

        // --- 给自己加钱: 输入框 + 确认 ---
        UIElement moneyRow = new UIElement();
        moneyRow.layout(l -> l.flexDirection(FlexDirection.ROW).height(16).gapAll(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        root.addChild(moneyRow);

        Label moneyLabel = new Label();
        moneyLabel.layout(l -> l.width(36).height(13));
        moneyLabel.bind(DataBindingBuilder.componentS2C(() ->
                Component.literal("加钱")).build());
        moneyRow.addChild(moneyLabel);

        TextField input = new TextField();
        input.layout(l -> l.flexGrow(1).height(16));
        input.setNumbersOnlyDouble(0, 1_000_000_000);
        input.bind(DataBindingBuilder.stringC2S(v -> {
            if (v != null) MONEY_INPUT.put(player.getUUID(), v);
        }).build());
        moneyRow.addChild(input);

        Button confirm = new Button();
        confirm.setText("确认");
        confirm.layout(l -> l.width(40).height(16));
        confirm.setOnServerClick(e -> {
            if (!(player instanceof ServerPlayer sp)) return;
            if (!isOp(sp)) { deny(sp); return; }
            String raw = MONEY_INPUT.get(sp.getUUID());
            double amount;
            try {
                amount = Double.parseDouble(raw == null ? "" : raw.trim());
            } catch (NumberFormatException ex) {
                amount = -1;
            }
            if (amount <= 0) {
                feedback(sp, Component.literal("请输入大于 0 的金额").withStyle(ChatFormatting.RED));
                return;
            }
            addMoney(sp, amount);
            feedback(sp, Component.literal("已增加 " + fmt(amount) + " 元").withStyle(ChatFormatting.GREEN));
        });
        moneyRow.addChild(confirm);

        // 当前资金 + 操作反馈
        Label balance = new Label();
        balance.layout(l -> l.height(13));
        balance.bind(DataBindingBuilder.componentS2C(() -> Component.literal(
                "当前资金: " + fmt(player.getData(SimulationTown.PLAYER_JINGJI.get())))
                .withStyle(ChatFormatting.GRAY)).build());
        root.addChild(balance);

        Label feedbackLabel = new Label();
        feedbackLabel.layout(l -> l.height(13));
        feedbackLabel.bind(DataBindingBuilder.componentS2C(() -> FEEDBACK.containsKey(player.getUUID())
                ? FEEDBACK.get(player.getUUID())
                : Component.literal("")).build());
        root.addChild(feedbackLabel);

        // 关闭
        Button close = new Button();
        close.setText("关闭");
        close.layout(l -> l.width(48).height(16));
        close.setOnClick(e -> player.closeContainer());
        root.addChild(close);

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
            return player.hasPermissions(2);   // 非 OP 打不开也留不住
        }
    }
}
