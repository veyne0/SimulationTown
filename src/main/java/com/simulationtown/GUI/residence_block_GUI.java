package com.simulationtown.GUI;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

/** 住宅方块的界面入口，内容与其它三个分类共用 building_ui。 */
public final class residence_block_GUI {

    private residence_block_GUI() {
    }

    public static ModularUI create(BlockUIMenuType.BlockUIHolder holder) {
        return building_ui.create(holder, "住宅");
    }
}
