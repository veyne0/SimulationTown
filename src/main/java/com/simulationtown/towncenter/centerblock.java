package com.simulationtown.towncenter;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.simulationtown.SimulationTown;
import com.simulationtown.towncenter.centerBlockEntity;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class centerblock extends Block implements BlockUIMenuType.BlockUI, EntityBlock {

    public centerblock(Properties properties) {

        super(properties);
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new centerBlockEntity(pos, state);
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit){
        if (level.getBlockEntity(pos) instanceof centerBlockEntity be){
            if(be.getUUID() == null){
                be.bangding(player);

            }
            if (be.get_pos()==null){
                BlockPos TownPos = player.getData(SimulationTown.TOWN_CENTER);
                player.setData(SimulationTown.TOWN_CENTER, new BlockPos(pos));
                be.zuobiao();
            }

            if (!level.isClientSide) {
                if (be.getUUID().equals(player.getUUID())&& pos.equals(player.getData(SimulationTown.TOWN_CENTER))){
                    BlockUIMenuType.openUI((ServerPlayer) player, pos);
                }else{
                    player.displayClientMessage(Component.literal("你已经有小镇了"), true);

                }

            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        // 从方块位置取回中心方块实体（服务端、客户端各有一份）
        BlockEntity be = holder.player.level().getBlockEntity(holder.pos);
        final centerBlockEntity center = be instanceof centerBlockEntity c ? c : null;

        // 输入框里最新的文字。服务端这一份由 stringC2S 持续写入，点"确定"时读它
        final String[] typedName = {""};

        // ---- 标题：没名字显示"建立小镇"，有名字显示镇名 ----
        Label title = new Label();
        title.layout(l -> l.height(14));
        if (center != null) {
            // 判断写在 createUI 里，但用 S2C 绑定：服务端算好推给客户端，客户端不自己猜
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

        // 已经有名字就整行藏起来。判断仍然只在服务端算，客户端跟着这个布尔值走
        if (center != null) {
            SimpleBinding<Boolean> namedBinding = DataBindingBuilder.boolS2C(() -> {
                String n = center.town_name;
                return n != null && !n.isEmpty();
            }).build();
            namedBinding.registerListener(isNamed ->
                    nameRow.setVisible(!Boolean.TRUE.equals(isNamed)));
        }

        Label levelLabel = new Label();
        levelLabel.layout(l -> l.height(12));
        if (center != null) {
            // 服务端读 get_level()，数值会自动同步到客户端；升级后这里会跟着变
            levelLabel.bind(DataBindingBuilder.componentS2C(
                    () -> Component.literal("等级：" + center.get_level() + " 级")).build());
        } else {
            levelLabel.setText("等级：--");
        }

        Label body = new Label();
        body.setText("居民：0 / 4\n建筑：0 / 3");
        body.layout(l -> l.flexGrow(1));

        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .flexGrow(1).heightPercent(100).paddingAll(8).gapAll(6));
        content.addChildren(title, hint, nameRow, levelLabel, body);

        // ---- 左侧边栏 ----
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(88).heightPercent(100).gapAll(4));

        Button btnOverview = new Button();
        btnOverview.setText("概览");
        btnOverview.layout(l -> l.widthPercent(100).height(20));
        // 只换正文，标题留给镇名（标题是绑定上去的，改它会断开显示）
        btnOverview.setOnClick(e -> body.setText("居民：0 / 4\n建筑：0 / 3"));

        Button btnResidents = new Button();
        btnResidents.setText("居民");
        btnResidents.layout(l -> l.widthPercent(100).height(20));
        btnResidents.setOnClick(e -> body.setText("还没有居民。\n先放一个住房建筑。"));

        Button btnBuildings = new Button();
        btnBuildings.setText("建筑");
        btnBuildings.layout(l -> l.widthPercent(100).height(20));
        btnBuildings.setOnClick(e -> body.setText("已建：0\n上限：3"));

        Button btnUpgrade = new Button();
        btnUpgrade.setText("升级");
        btnUpgrade.layout(l -> l.widthPercent(100).height(20));
        // setOnServerClick 的回调在服务端执行，所以能安全改方块实体里的数据
        btnUpgrade.setOnServerClick(e -> {
            if (center != null) {
                center.shengji();
            }
        });

        Button btnAbandon = new Button();
        btnAbandon.setText("放弃小镇");
        btnAbandon.layout(l -> l.widthPercent(100).height(20));
        // 服务端回调：调用你写的 qingli 解绑，然后关界面、删方块
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

        return ModularUI.of(
                UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP)),
                holder.player);
    }




}
