package com.example.extract;

import android.util.Log;

/**
 * Created by temp on 14-7-4.
 * 表示压缩包中一个子条目(文件或文件夹)的信息
 */
public class ItemInfo {
    final String entryName;//在压缩包的路径
    String sep;
    boolean isDir;  //是否为目录
    boolean isEncrypted;//是否加密
//    String displayName;
    public ItemInfo(String en,boolean isD, String s){
        entryName = en;
        isDir = isD;
        sep = s;
//        int index = en.lastIndexOf(sep);
//        displayName = en.substring(1 + index);
    }
}
