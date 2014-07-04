package com.example.extract;

import android.text.TextUtils;
import android.util.Log;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by temp on 14-7-4.
 * extract rar file.
 */
public class ExtractRar extends AbstractExtract {

    private static final String TAG = "ExtractRar";
    private static final String sep = "\\";//File.separator;

    Archive mRarFile;

    public ExtractRar(String path) {
        mFilePath = path;
        try {
            mRarFile = new Archive(new File(path));
        } catch (Exception e) {
            loge("new Archive()" + e);
        }
    }

    @Override
    public ArrayList<ItemInfo> initAllItems() {
        if (!mAllDirs.isEmpty())
            return mAllDirs;
        try {
            if (mRarFile.isEncrypted()) {
                loge("rar File is encrypted. ");
                return mAllDirs;
            }
            FileHeader fh = null;
            while (true) {
                fh = mRarFile.nextFileHeader();
                if (fh == null) break;
                String fns = fh.getFileNameString();
                if (fh.isEncrypted()) {
                    loge(fh.getFileNameString() + " is encrypted.");
                    continue;
                }
                logd("find fns=" + fns /*getFileNameW() is empty.*/);
                ItemInfo ii = new ItemInfo(fns, fh.isDirectory(), sep);
                mAllDirs.add(ii);
            }
//            mRarFile.close();
        } catch (Exception e) {
            loge("initAllitems() " + e);
        }
        return mAllDirs;
    }

    @Override
    public int extractOneEntry(ItemInfo ii, String outDir) {
        String des = "";
        if (TextUtils.isEmpty(outDir))
            des = mFilePath.substring(0, mFilePath.length() - 4);// + sep + ii.entryName;
        else
            des = outDir ;//+ sep + ii.entryName;
        final String pre = ii.entryName;
        List<FileHeader> list;
        try {
            list = mRarFile.getFileHeaders();
        } catch (Exception e) {
            loge("[rar]extractOneEntry() " + e);
            return -10;
        }
        for (FileHeader fh : list) {
            if (!fh.getFileNameString().startsWith(pre))
                continue;
            boolean success = Utils.extractOneFileHeader(mRarFile, fh, des);
            if (!success)
                return -1;
        }
        return 0;
    }

    @Override
    public ArrayList<ItemInfo> getItemsForDir(String parent) {
        if (mCaches.containsKey(parent)) {
            return mCaches.get(parent);
        }
        ArrayList<ItemInfo> lists = mAllDirs;
        ArrayList<ItemInfo> childs = new ArrayList<ItemInfo>();
        if (TextUtils.isEmpty(parent)) {
            for (ItemInfo ii : lists) {
                if (!ii.entryName.contains(sep)) {
                    childs.add(ii);
                }
            }
        } else {
            for (ItemInfo ii : lists) {
                String str = ii.entryName;
                if (!str.startsWith(parent) || str.equals(parent))
                    continue;
                str = str.substring(parent.length());//去除前面的parent一样的string
                if (str.startsWith(sep))
                    str = str.substring(1);
                if (!str.contains(sep))
                    childs.add(ii);
            }
        }

        mCaches.put(parent, childs);
        logd("put key: " + parent + " ,size =" + childs.size());
        return childs;
    }

    @Override
    public void finish(){
        if(mRarFile!=null){
            try {
                mRarFile.close();
            }catch (IOException e){
                loge("finish()"+e);
            }
        }
    }

    private void logd(String s) {
        Log.d(TAG, s);
    }

    private void loge(String s) {
        Log.e(TAG, s);
    }
}
