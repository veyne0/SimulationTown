package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.simulationtown.SimulationTown;
import com.simulationtown.towncenter.centerBlockEntity;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 镇中心的界面：左侧栏 + 右侧内容，命名、升级、放弃都在这里。 */
public final class centerblock_GUI {

    private centerblock_GUI() {
    }

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        // 从方块位置取回中心方块实体（服务端、客户端各有一份）
        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        final centerBlockEntity center = be instanceof centerBlockEntity c ? c : null;

        // 输入框里最新的文字。服务端这一份由 stringC2S 持续写入，点"确定"时读它
        final String[] typedName = {""};

        // 当前显示的是哪一页：0 概览 / 1 居民 / 2 建筑 / 3 升级
        // 只在服务端改，文字由 S2C 绑定推给客户端
        final int[] page = {0};

        // ---- 标题：没名字显示"建立小镇"，有名字显示镇名 ----
        Label title = new Label();
        title.layout(l -> l.height(14));
        if (center != null) {
            // 判断写在界面里，但用 S2C 绑定：服务端算好推给客户端，客户端不自己猜
            title.bind(DataBindingBuilder.componentS2C(() -> {
                String n = center.town_name;
                return Component.literal(n == null || n.isEmpty() ? "建立小镇" : n);
            }).build());
        } else {
            title.setText("建立小镇");
        }

        // ---- 提示行 ----
        Label hint = new Label();
        hint.layout(l -> l.height(10));
        if (center != null) {
            hint.bind(DataBindingBuilder.componentS2C(() -> {
                String n = center.town_name;
                return Component.literal(n == null || n.isEmpty()
                        ? "给你的小镇取个名字（最多 16 个字），点右边『确定』"
                        : "镇长：" + holder.player.getName().getString());
            }).build());
        } else {
            hint.setText("给你的小镇取个名字，点右边『确定』");
        }

        // ---- 命名行：输入框 + 确定 ----
        TextField nameField = new TextField();
        nameField.setAnyString();
        nameField.layout(l -> l.flexGrow(1).height(18));
        nameField.bind(DataBindingBuilder.stringC2S(s -> typedName[0] = s).build());

        Button btnConfirm = new Button();
        btnConfirm.setText("确定");
        btnConfirm.layout(l -> l.width(44).height(18));
        btnConfirm.setOnServerClick(e -> {
            if (center == null) return;
            ServerPlayer sp = (ServerPlayer) holder.player;
            String name = typedName[0] == null ? "" : typedName[0].trim();
            if (name.isEmpty()) {
                sp.displayClientMessage(Component.literal("请先输入小镇名字"), true);
                return;
            }
            if (name.length() > 16) name = name.substring(0, 16);

            center.jianli(sp, name);
            center.setChanged();   // jianli 里没写 setChanged，补一句才会写进存档
            if (sp.getExistingDataOrNull(SimulationTown.TOWN_CENTER) == null) {
                sp.setData(SimulationTown.TOWN_CENTER, holder.pos);   // 兜底：玩家身上还没有镇坐标
            }
            if (center.getUUID() == null) {
                center.bangding(sp);                                   // 兜底：还没绑定镇长
            }
            // buildlib 人口系统：命名门槛解除。首次命名会补发初始 NPC
            // （放镇中心时没命名 → PopulationManager 当时不发，等这里解除门槛）
            if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                // 首次命名（初始化标记还没打上）才发启动资金，改名重复点不会重复发
                boolean firstTime = !com.buildlib.population.PopulationManager.isInitialized(sl, holder.pos);
                com.buildlib.population.PopulationManager.markTownInitialized(sl, holder.pos);
                if (firstTime) {
                    String msg = center.add_jingji(sp);
                    if (!msg.isEmpty()) {
                        sp.displayClientMessage(Component.literal(msg), true);
                    }
                }
            }
            sp.displayClientMessage(Component.literal("小镇「" + name + "」建立成功"), true);
        });

        UIElement nameRow = new UIElement();
        nameRow.layout(l -> l.flexDirection(FlexDirection.ROW).gapAll(4).height(18));
        nameRow.addChildren(nameField, btnConfirm);

        // 命名行的显隐不在这里定：它要和侧边栏一起切，见文件末尾的 namedLabel

        Label levelLabel = new Label();
        levelLabel.layout(l -> l.height(12));
        if (center != null) {
            // 服务端读 get_level()，数值会自动同步到客户端；升级后这里会跟着变
            levelLabel.bind(DataBindingBuilder.componentS2C(
                    () -> Component.literal("等级：" + center.get_level() + " / " + center.max_level)).build());
        } else {
            levelLabel.setText("等级：--");
        }

        // ---- 升级页专用内容：标题 / 材料行(物品图标+文字) / 经济行 ----
        // 显隐全部由文件末尾的 pageLabel 绑定控制 (只有停在升级页且未满级时才显示)
        Label upgradeTitle = new Label();
        upgradeTitle.layout(l -> l.height(12));
        upgradeTitle.setVisible(false);

        ItemSlot upgradeIcon = new ItemSlot();
        upgradeIcon.layout(l -> l.width(18).height(18));
        if (center != null) {
            // 材料图标: 服务端按当前等级把所需物品推给客户端 (物品数量就是需求的数量)
            upgradeIcon.bind(DataBindingBuilder.itemStackS2C(() -> {
                if (page[0] != 3 || center.get_level() >= center.max_level) return ItemStack.EMPTY;
                UpgradeNeed need = needFor(center.get_level());
                return new ItemStack(need.item(), need.count());
            }).build());
        }

        Label upgradeItem = new Label();
        upgradeItem.layout(l -> l.flexGrow(1).height(16));
        upgradeItem.setVisible(false);

        Label upgradeEco = new Label();
        upgradeEco.layout(l -> l.height(12));
        upgradeEco.setVisible(false);

        if (center != null) {
            upgradeTitle.bind(DataBindingBuilder.componentS2C(() -> {
                if (page[0] != 3 || center.get_level() >= center.max_level) return Component.literal("");
                return Component.literal("升级到 " + (center.get_level() + 1) + " 级需要：");
            }).build());
            upgradeItem.bind(DataBindingBuilder.componentS2C(() -> {
                if (page[0] != 3 || center.get_level() >= center.max_level) return Component.literal("");
                UpgradeNeed need = needFor(center.get_level());
                int have = countItem(holder.player, need.item());
                return Component.literal((have >= need.count() ? "✔" : "✘") + " "
                        + need.item().getDescription().getString()
                        + " " + have + " / " + need.count());
            }).build());
            upgradeEco.bind(DataBindingBuilder.componentS2C(() -> {
                if (page[0] != 3 || center.get_level() >= center.max_level) return Component.literal("");
                UpgradeNeed need = needFor(center.get_level());
                double haveJingji = holder.player.getData(SimulationTown.PLAYER_JINGJI);
                String jingjiText = haveJingji == Math.floor(haveJingji)
                        ? String.valueOf((long) haveJingji) : String.format("%.2f", haveJingji);
                return Component.literal((haveJingji >= need.jingji() ? "✔" : "✘") + " 经济 "
                        + jingjiText + " / " + need.jingji());
            }).build());
        } else {
            upgradeTitle.setText("--");
            upgradeItem.setText("--");
            upgradeEco.setText("--");
        }

        // ---- 升级页专用按钮：只有停在升级页时才显示 ----
        Button btnDoUpgrade = new Button();
        btnDoUpgrade.setText("升级镇中心");
        btnDoUpgrade.layout(l -> l.width(96).height(18));
        btnDoUpgrade.setVisible(false);
        // 只有停在升级页时才显示，显隐由文件末尾的 pageLabel 绑定控制
        // 真正的升级动作必须在服务端执行（要扣物品、扣经济、改数据）
        btnDoUpgrade.setOnServerClick(e -> {
            if (center == null) return;
            ServerPlayer sp = (ServerPlayer) holder.player;
            int before = center.get_level();
            center.shengji(sp);
            int after = center.get_level();
            if (after > before) {
                sp.displayClientMessage(Component.literal("小镇升级到 " + after + " 级"), true);
            } else {
                sp.displayClientMessage(Component.literal("材料或经济不够，升级失败"), true);
            }
        });

        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .flexGrow(1).heightPercent(100).paddingAll(8).gapAll(6));
        // 材料行: 图标 + 文字并排
        UIElement upgradeRow = new UIElement();
        upgradeRow.layout(l -> l.flexDirection(FlexDirection.ROW).height(18).gapAll(4));
        upgradeRow.addChildren(upgradeIcon, upgradeItem);
        upgradeRow.setVisible(false);

        UIElement rest = new UIElement();
        rest.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).gapAll(6));
        rest.addChildren(levelLabel, upgradeTitle, upgradeRow, upgradeEco, btnDoUpgrade);

        // 两个隐藏 Label：文字分别是"有没有名字""是不是升级页"，回调里据此切显隐
        Label namedLabel = new Label();
        namedLabel.setVisible(false);
        Label pageLabel = new Label();
        pageLabel.setVisible(false);

        content.addChildren(title, hint, nameRow, rest, namedLabel, pageLabel);

        // ---- 左侧边栏 ----
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(88).heightPercent(100).gapAll(4));

        Button btnOverview = new Button();
        btnOverview.setText("概览");
        btnOverview.layout(l -> l.widthPercent(100).height(20));
        btnOverview.setOnServerClick(e -> page[0] = 0);

        Button btnUpgrade = new Button();
        btnUpgrade.setText("升级");
        btnUpgrade.layout(l -> l.widthPercent(100).height(20));
        // 只切到升级页，真正的升级在页内的"升级镇中心"按钮上，避免误点
        btnUpgrade.setOnServerClick(e -> page[0] = 3);

        Button btnAbandon = new Button();
        btnAbandon.setText("放弃小镇");
        btnAbandon.layout(l -> l.widthPercent(100).height(20));
        // 服务端回调：qingli 返回结果消息 —— 成功才关界面、删方块；没下界之星就只提示
        // 注意：玩家 removeData 只能清玩家附件；人口去重记录在维度 SavedData 里，
        // 必须调 PopulationManager.clearTown，否则同位置重建会出现"新镇不补 NPC"
        // (GDP 登记已迁往主程序镇中心 BE, 拆镇自动消失, 无需清理)
        btnAbandon.setOnServerClick(e -> {
            if (center == null) return;
            ServerPlayer sp = (ServerPlayer) holder.player;
            String msg = center.qingli(sp);
            if ("已放弃小镇".equals(msg)) {
                if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    com.buildlib.population.PopulationManager.clearTown(sl, holder.pos);
                }
                sp.closeContainer();                       // 界面正盯着这个方块，先关掉
                sp.level().removeBlock(holder.pos, false); // 再删方块，不掉落物品
            }
            sp.displayClientMessage(Component.literal(msg), true);
        });

        sidebar.addChildren(btnOverview, btnUpgrade, btnAbandon);

        // ---- 根容器：左右两栏 ----
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.ROW)
                .width(320).height(200).paddingAll(6).gapAll(8));
        root.addClass("panel_bg");
        root.addChildren(sidebar, content);

        // ---- 没名字：只留命名界面；有名字：藏起命名行，显示侧边栏和正文 ----
        // center.name() 只有服务端有正确结果（客户端方块实体里没有 town_name），
        // 所以判断用 S2C 绑定推给客户端。onRemoteSyncReceived 在第一次同步时也会触发，
        // 而 registerListener 只在值变化时触发 —— 用它控件会一直停在初始状态。
        if (center != null) {
            namedLabel.bind(DataBindingBuilder.componentS2C(
                            () -> Component.literal(center.town_name != null && center.name() ? "1" : "0"))
                    .onRemoteSyncReceived(c -> {
                        boolean ok = "1".equals(c.getString());
                        nameRow.setVisible(!ok);
                        sidebar.setVisible(ok);
                        rest.setVisible(ok);
                    }).build());

            pageLabel.bind(DataBindingBuilder.componentS2C(
                            () -> Component.literal(page[0] == 3 && center.get_level() < center.max_level ? "1" : "0"))
                    .onRemoteSyncReceived(c -> {
                        boolean ok = "1".equals(c.getString());
                        btnDoUpgrade.setVisible(ok);
                        upgradeTitle.setVisible(ok);
                        upgradeRow.setVisible(ok);
                        upgradeEco.setVisible(ok);
                    }).build());
        } else {
            // 拿不到方块实体（理论上不会发生）：只留命名界面，别让玩家点出事
            nameRow.setVisible(true);
            sidebar.setVisible(false);
            rest.setVisible(false);
        }

        return ModularUI.of(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)),
                holder.player);
    }

    // ======== 升级需求（服务端算） ========

    /** 每一级的升级需求: 材料 + 数量 + 经济 */
    private record UpgradeNeed(Item item, int count, int jingji) {
    }

    /** 按当前等级取需求。数值与 centerBlockEntity.shengji() 里的判断保持一致。 */
    private static UpgradeNeed needFor(int lv) {
        return switch (lv) {
            case 1 -> new UpgradeNeed(Items.IRON_INGOT, 64, 10);
            case 2 -> new UpgradeNeed(Items.GOLD_INGOT, 64, 500);
            case 3 -> new UpgradeNeed(Items.DIAMOND, 64, 5000);
            default -> new UpgradeNeed(Items.NETHER_STAR, 1, 20000);
        };
    }

    /** 数玩家背包里某种物品的总数。 */
    private static int countItem(Player p, Item item) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(item)) n += st.getCount();
        }
        return n;
    }
}
