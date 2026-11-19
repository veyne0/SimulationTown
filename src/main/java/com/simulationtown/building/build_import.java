package com.simulationtown.building;

import com.buildlib.structure.StructureNbtParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class build_import {
    public String daoru() {
        JFileChooser nbt_chooser = new JFileChooser();
        nbt_chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "NBT 文件", "nbt"));
        int result = nbt_chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            if (GraphicsEnvironment.isHeadless()) {
                return "正在使用headless";
            }
            File nbt_file = nbt_chooser.getSelectedFile();
            String file_path = nbt_file.getAbsolutePath();
            String file_name=nbt_file.getName();
            Path file_path1 = Path.of(file_path);
            List<String> names = StructureNbtParser.parsePaletteNames(file_path);
            if (names.contains("simulation_town:residence_block")) {
                Path t = FMLPaths.GAMEDIR.get().resolve("simulation_town_buliding/residence/");
                String t_=t+"/" +file_name;
                Path new_t=Path.of(t_);
                try {
                    Files.copy(file_path1, new_t, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    e.getMessage();
                    return "导入失败" +e;
                }

            } else {
                String cuowu = "错误的建筑格式,该建筑里没有住宅方块";
                System.out.println(cuowu);
                return cuowu;
            }
        } else {
            String quxiao = "已取消选择";
            return quxiao;
        }
        return "a";
    }
}