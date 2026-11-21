package com.simulationtown.NPC;


import java.util.Random;

public class NPC_name {
    /* 名字生成*/
    private static final String[] xing = {"王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴","凑"};
    private static final String[] ming    = {"小明", "大山", "二狗", "铁柱", "春花", "秀英", "长生", "小狗","小猫"};

    public static String randomName() {
        Random r = new Random();
        return xing[r.nextInt(xing.length)] + ming[r.nextInt(ming.length)];
    }

}
