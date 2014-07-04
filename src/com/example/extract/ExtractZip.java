package com.example.extract;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by temp on 14-7-4.
 * extract zip file .
 */
public class ExtractZip extends AbstractExtract {
    private static final String TAG = "ExtractZip";

    private static final String sep = File.separator;

    ExtractZip(String p) {
        mFilePath = p;
    }

    @Override
    public ArrayList<ItemInfo> initAllItems() {
        if (!mAllDirs.isEmpty())
            return mAllDirs;
        try {
            ZipFile zipFile = new ZipFile(mFilePath);
            Enumeration<?> es = zipFile.entries();
            ZipEntry zipEntry = null;

            while (es.hasMoreElements()) {
                zipEntry = (ZipEntry) es.nextElement();
                String s = zipEntry.getName();
                ItemInfo ii = new ItemInfo(s, zipEntry.isDirectory(), sep);
//                if (!s.endsWith(sep)) // ignore string end with /
                mAllDirs.add(ii);
                logd("find directly :" + s);
            }
            zipFile.close();
        } catch (Exception e) {

        }
        return mAllDirs;
    }

    public int extractOneEntry(ItemInfo ii, String outDir) {
        String des = "";
        if (TextUtils.isEmpty(outDir))
            des = mFilePath.substring(0, mFilePath.length() - 4) + sep + ii.entryName;
        else
            des = outDir + sep + ii.entryName;
        ZipFile file = null;
        logd("entryName=" + ii.entryName + ",des=" + des);
//        publishProgress(entryName);
        try {
            file = new ZipFile(mFilePath);
            InputStream is = file.getInputStream(new ZipEntry(ii.entryName));
            File desFile = new File(des);
            if (ii.isDir) {
                desFile.mkdirs();
                return 0;
            }
            if (!desFile.exists()) {
                File pFile = desFile.getParentFile();
                if (!pFile.exists())
                    pFile.mkdirs();
                desFile.createNewFile();
            }
            OutputStream out = new FileOutputStream(desFile);
            byte buffer[] = new byte[10240];
            int read;
            while (0 < (read = is.read(buffer, 0, 10240)))
                out.write(buffer, 0, read);

            out.close();
            is.close();
            file.close();
        } catch (IOException e) {
            loge("copy failed! " + e.toString());
            return -1;
        }
        return 0;
    }

    @Override
    public ArrayList<ItemInfo> getItemsForDir(String parent) {
        if (mCaches.containsKey(parent)) {
            return mCaches.get(parent);
        }
        int index = -1;
        ArrayList<ItemInfo> lists = mAllDirs;
        ArrayList<ItemInfo> childs = new ArrayList<ItemInfo>();
        if (parent.length() == 0) {
            for (ItemInfo ii : lists) {
                String str = ii.entryName;
                index = str.indexOf(sep);
                if (-1 == index /* || index == str.length() - 1 */) {
                    childs.add(ii);
                } else {
                    str = str.substring(0, index + 1);
                    ItemInfo itemi = new ItemInfo(str, true, sep);
                    boolean has = false;
                    for (ItemInfo iii : childs) {
                        if (str.equals(iii.entryName)) {
                            has = true;
                            break;
                        }
                    }
                    if (!has)
                        childs.add(itemi);
                }
            }
            mCaches.put(sep, childs);
        } else {
            for (ItemInfo ii : lists) {  // parent endWith / ,  eg. xl/
                String str = ii.entryName;
                if (!str.startsWith(parent))//过滤开头不一样的entry
                    continue;
                str = str.substring(parent.length());//去除一样的开头部分parent
                index = str.indexOf(sep);
                if ((-1 == index || index == str.length()) && str.length() > 0) {
                    childs.add(ii);
                } else {
                    str = str.substring(0, index + 1);
                    ItemInfo itemInfo = new ItemInfo(parent + str, true, sep);
                    boolean has = false;
                    for (ItemInfo iii : childs) {     //判断childs中是否已经含itemInfo
                        if (iii.entryName.equals(parent + str)) {
                            has = true;
                            break;
                        }
                    }
                    if (!has)
                        childs.add(itemInfo);
                }
            }
            mCaches.put(parent, childs);
        }
        logd("put key: " + parent + " ,size =" + childs.size());
        return childs;
    }

    @Override
    public void finish(){

    }
    private void logd(String s) {
        Log.d(TAG, s);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
}
