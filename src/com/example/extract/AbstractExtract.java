package com.example.extract;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by temp on 14-7-4.
 * zip, rar 的父类
 */
public abstract class AbstractExtract {
    String mFilePath;
    ArrayList<ItemInfo> mAllDirs = new ArrayList<ItemInfo>();
    HashMap<String, ArrayList<ItemInfo>> mCaches = new HashMap<String, ArrayList<ItemInfo>>();

    public static AbstractExtract create(String path) {
        if (path.toLowerCase().endsWith("zip"))
            return new ExtractZip(path);
        else if(path.toLowerCase().endsWith("rar"))
            return new ExtractRar(path);
        else {
            System.out.println("no support file :" + path);
            return null;
        }
    }

    public abstract ArrayList<ItemInfo> initAllItems();

    public abstract ArrayList<ItemInfo> getItemsForDir(String dir);

    public int extractDirs(ItemInfo lists[], String out) {
        int r = -1;
        for (ItemInfo ii : lists) {
            if(ii.isDir){
                ArrayList<ItemInfo> iis = getItemsForDir(ii.entryName);
                for(ItemInfo one : iis){
                    if(one.isDir) {
                        r = extractDirs(new ItemInfo[]{one}, out);
                        if(r != 0) return r;
                    }else {
                        r = extractOneEntry(one,out);
                        if(r != 0) return r;
                    }
                }
            } else {
                r = extractOneEntry(ii, out);
                if(r != 0) break;
            }
        }
        return r;
    }

    public abstract int extractOneEntry(ItemInfo entryname, String outDir);

    public abstract void finish();
}
