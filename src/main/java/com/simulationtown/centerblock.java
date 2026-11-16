package com.simulationtown;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
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
        return new centerBlockEntity(pos, state); // 返回你独立的 BlockEntity 子类
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit){
        if (level.getBlockEntity(pos) instanceof centerBlockEntity be){
            if(be.getUUID() == null){
                be.bangding(player);
                player.displayClientMessage(Component.literal("已绑定小镇中心"), true);
            }else{
                player.displayClientMessage(Component.literal("你已经有小镇了，不能再绑定了"), true);
            }

            if (!level.isClientSide) {
                if (be.getUUID().equals(player.getUUID())){
                    BlockUIMenuType.openUI((ServerPlayer) player, pos);
                }else{
                    player.displayClientMessage(Component.literal("这个小镇不是你的"), true);
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

        // ---- 右侧内容区 ----
        Label title = new Label();
        title.setText("小镇中心");
        title.layout(l -> l.height(14));

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
        content.addChildren(title, levelLabel, body);

        // ---- 左侧边栏 ----
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(88).heightPercent(100).gapAll(4));

        Button btnOverview = new Button();
        btnOverview.setText("概览");
        btnOverview.layout(l -> l.widthPercent(100).height(20));
        btnOverview.setOnClick(e -> {
            title.setText("小镇中心");
            body.setText("居民：0 / 4\n建筑：0 / 3");
        });

        Button btnResidents = new Button();
        btnResidents.setText("居民");
        btnResidents.layout(l -> l.widthPercent(100).height(20));
        btnResidents.setOnClick(e -> {
            title.setText("居民");
            body.setText("还没有居民。\n先放一个住房建筑。");
        });

        Button btnBuildings = new Button();
        btnBuildings.setText("建筑");
        btnBuildings.layout(l -> l.widthPercent(100).height(20));
        btnBuildings.setOnClick(e -> {
            title.setText("建筑");
            body.setText("已建：0\n上限：3");
        });

        Button btnUpgrade = new Button();
        btnUpgrade.setText("升级");
        btnUpgrade.layout(l -> l.widthPercent(100).height(20));
        // setOnServerClick 的回调在服务端执行，所以能安全改方块实体里的数据
        btnUpgrade.setOnServerClick(e -> {
            if (center != null) {
                center.shengji();
            }
        });

        sidebar.addChildren(btnOverview, btnResidents, btnBuildings, btnUpgrade);

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
