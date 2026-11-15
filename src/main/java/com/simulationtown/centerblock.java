package com.simulationtown;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class centerblock extends Block implements BlockUIMenuType.BlockUI{

    public centerblock(Properties properties) {
        super(properties);
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit){
        if (!level.isClientSide) {
            BlockUIMenuType.openUI((ServerPlayer) player, pos);
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        // ---- 右侧内容区 ----
        Label title = new Label();
        title.setText("小镇中心");
        title.layout(l -> l.height(14));

        Label body = new Label();
        body.setText("等级：1 级\n居民：0 / 4\n建筑：0 / 3");
        body.layout(l -> l.flexGrow(1));

        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .flexGrow(1).heightPercent(100).paddingAll(8).gapAll(6));
        content.addChildren(title, body);

        // ---- 左侧边栏 ----
        UIElement sidebar = new UIElement();
        sidebar.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .width(88).heightPercent(100).gapAll(4));

        Button btnOverview = new Button();
        btnOverview.setText("概览");
        btnOverview.layout(l -> l.widthPercent(100).height(20));
        btnOverview.setOnClick(e -> {
            title.setText("小镇中心");
            body.setText("等级：1 级\n居民：0 / 4\n建筑：0 / 3");
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

        sidebar.addChildren(btnOverview, btnResidents, btnBuildings);

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
