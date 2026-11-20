package com.simulationtown.towncenter;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class consume {
    /*两个方法纯AI生成*/
    public static boolean hasItem(Player player, Item targetItem, int requiredAmount) {
        int foundAmount = 0;


        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.is(targetItem)) {
                foundAmount += stack.getCount();

                if (foundAmount >= requiredAmount) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean xiaohao(Player player, Item targetItem, int amountToConsume) {
        // 先检查是否足够（防止扣到一半发现不够）
        if (!hasItem(player, targetItem, amountToConsume)) {
            return false;
        }

        int remainingToConsume = amountToConsume;

        // 遍历背包扣除物品
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.is(targetItem)) {
                int stackCount = stack.getCount();

                if (stackCount > remainingToConsume) {

                    stack.shrink(remainingToConsume);
                    remainingToConsume = 0;
                    break;
                } else {

                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    remainingToConsume -= stackCount;
                }
            }
        }

        // 更新背包状态
        player.getInventory().setChanged();
        return true;
    }
}
