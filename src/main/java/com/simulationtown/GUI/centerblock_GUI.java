package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
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

        // ---- 正文：内容随 page 变化，仍然是服务端算好推过来 ----
        Label body = new Label();
        body.layout(l -> l.flexGrow(1));
        if (center != null) {
            body.bind(DataBindingBuilder.componentS2C(
                    () -> Component.literal(pageText(page[0], center, holder.player))).build());
        } else {
            body.setText("居民：--\n建筑：--");
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
        UIElement rest = new UIElement();
        rest.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).gapAll(6));
        rest.addChildren(levelLabel, body, btnDoUpgrade);

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

        Button btnResidents = new Button();
        btnResidents.setText("居民");
        btnResidents.layout(l -> l.widthPercent(100).height(20));
        btnResidents.setOnServerClick(e -> page[0] = 1);

        Button btnBuildings = new Button();
        btnBuildings.setText("建筑");
        btnBuildings.layout(l -> l.widthPercent(100).height(20));
        btnBuildings.setOnServerClick(e -> page[0] = 2);

        Button btnUpgrade = new Button();
        btnUpgrade.setText("升级");
        btnUpgrade.layout(l -> l.widthPercent(100).height(20));
        // 只切到升级页，真正的升级在页内的"升级镇中心"按钮上，避免误点
        btnUpgrade.setOnServerClick(e -> page[0] = 3);

        Button btnAbandon = new Button();
        btnAbandon.setText("放弃小镇");
        btnAbandon.layout(l -> l.widthPercent(100).height(20));
        // 服务端回调：调用 qingli 解绑，然后关界面、删方块
        btnAbandon.setOnServerClick(e -> {
            if (center == null) return;
            ServerPlayer sp = (ServerPlayer) holder.player;
            center.qingli(sp);
            sp.closeContainer();                       // 界面正盯着这个方块，先关掉
            sp.level().removeBlock(holder.pos, false); // 再删方块，不掉落物品
            sp.displayClientMessage(Component.literal("小镇已放弃，可以在别处重建"), true);
        });

        sidebar.addChildren(btnOverview, btnResidents, btnBuildings, btnUpgrade, btnAbandon);

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
                            () -> Component.literal(page[0] == 3 ? "1" : "0"))
                    .onRemoteSyncReceived(c -> btnDoUpgrade.setVisible("1".equals(c.getString())))
                    .build());
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

    // ======== 正文文案（服务端算） ========

    private static String pageText(int page, centerBlockEntity c, Player p) {
        return switch (page) {
            case 1 -> "居民：" + c.get_now_resident() + " / " + c.now_max_resident;
            case 2 -> "建筑：0 / 3";
            case 3 -> upgradeText(c, p);
            default -> "居民：" + c.get_now_resident() + " / " + c.now_max_resident
                    + "\n建筑：0 / 3";
        };
    }

    /** 升级页：列出这一级要什么、你有多少。数值与 shengji() 里的判断保持一致。 */
    private static String upgradeText(centerBlockEntity c, Player p) {
        int lv = c.get_level();
        if (lv >= c.max_level) {
            return "等级：" + lv + " / " + c.max_level + "\n已经最高级了。";
        }

        Item need;
        int needCount;
        int needJingji;
        switch (lv) {
            case 1 -> {
                need = Items.IRON_INGOT;
                needCount = 64;
                needJingji = 10;
            }
            case 2 -> {
                need = Items.GOLD_INGOT;
                needCount = 64;
                needJingji = 500;
            }
            case 3 -> {
                need = Items.DIAMOND;
                needCount = 64;
                needJingji = 5000;
            }
            default -> {
                need = Items.NETHER_STAR;
                needCount = 1;
                needJingji = 20000;
            }
        }

        int haveItem = countItem(p, need);
        int haveJingji = p.getData(SimulationTown.PLAYER_JINGJI);

        String itemName = need.getDescription().getString();
        return "升级到 " + (lv + 1) + " 级需要："
                + "\n" + (haveItem >= needCount ? "✔" : "✘") + " " + itemName
                + " " + haveItem + " / " + needCount
                + "\n" + (haveJingji >= needJingji ? "✔" : "✘") + " 经济 "
                + haveJingji + " / " + needJingji;
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
