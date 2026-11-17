package com.simulationtown.building;

import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class building_info {
    public String category;
    public String file_name;
    public building_info(String category,String file_name){
        this.category = category;
        this.file_name = file_name;
    }
    public static List<building_info> scanAllBuildings() {
        List<building_info> buildingList = new ArrayList<>();
        Path rootDir = FMLPaths.GAMEDIR.get().resolve("simulation_town_buildings");
        String[] categories = {"residence", "agriculture", "industry", "special"};

        for (String category : categories) {
            Path categoryDir = rootDir.resolve(category);
            if (!Files.exists(categoryDir)) continue;

            try (Stream<Path> paths = Files.walk(categoryDir, 1)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".nbt"))
                        .forEach(p -> {
                            String fileName = p.getFileName().toString().replace(".nbt", "");
                            buildingList.add(new building_info(category, fileName));
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return buildingList;
    }
}
