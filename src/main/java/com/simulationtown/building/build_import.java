package com.simulationtown.building;

import com.buildlib.structure.StructureNbtParser;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class build_import {
    public void daoru() {
        JFileChooser nbt_chooser = new JFileChooser();
        nbt_chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "NBT 文件", "nbt"));
        int result = nbt_chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File nbt_file = nbt_chooser.getSelectedFile();
            String file_path = nbt_file.getAbsolutePath();
            List<String> names = StructureNbtParser.parsePaletteNames(file_path);




        }
    }
}