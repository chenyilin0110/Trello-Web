package com.smb.trelloExport.utility;

import java.util.HashMap;

public class Constant {

    public static final HashMap<String, String> NAME_MAP;
    static{
        NAME_MAP = new HashMap<String, String>();
        NAME_MAP.put("xerious", "喜仙");
        NAME_MAP.put("喜仙", "xerious");
        NAME_MAP.put("young", "孟揚");
        NAME_MAP.put("孟揚", "young");
        NAME_MAP.put("yilin", "羿霖");
        NAME_MAP.put("羿霖", "yilin");
        NAME_MAP.put("Jason", "Jason");
        NAME_MAP.put("SJ", "曉真");
        NAME_MAP.put("曉真", "SJ");
        NAME_MAP.put("lunchi", "倫奇");
        NAME_MAP.put("倫奇", "lunchi");
        NAME_MAP.put("jimmy", "俊銘");
        NAME_MAP.put("俊銘", "jimmy");
    }


    public static final HashMap<String, String> CHINESE_NAME_MAP;
    static{
        CHINESE_NAME_MAP = new HashMap<String, String>();
        CHINESE_NAME_MAP.put("YILIN CHEN", "羿霖");
        CHINESE_NAME_MAP.put("羿霖", "YILIN CHEN");
        CHINESE_NAME_MAP.put("Jason Lin", "Jason");
        CHINESE_NAME_MAP.put("Jason", "Jason Lin");
        CHINESE_NAME_MAP.put("xeriou", "喜仙");
        CHINESE_NAME_MAP.put("喜仙", "xeriou");
        CHINESE_NAME_MAP.put("Young", "孟揚");
        CHINESE_NAME_MAP.put("孟揚", "Young");
        CHINESE_NAME_MAP.put("lunchi", "倫奇");
        CHINESE_NAME_MAP.put("倫奇", "lunchi");
        CHINESE_NAME_MAP.put("曉真", "曉真");
    }
}
