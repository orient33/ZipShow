package com.example.extract;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RarShow extends Activity {
    final String sep = "\\";//File.separator; // Rar file sep is \
    String mRarPath, mOutDir, mParent;
    Archive mRarFile;
    ListView mListView;
    ArrayList<FileInfo> mAllDirs = new ArrayList<FileInfo>();
    ArrayList<FileInfo> mDirs = new ArrayList<FileInfo>();
    HashMap<String, ArrayList<FileInfo>> mCaches = new HashMap<String, ArrayList<FileInfo>>();
    ProgressDialog mDialog;

    private class FileInfo{
        final String mmDirs;
        final String mmDisplayDir;
        final boolean mmIsDir;
        final boolean mmIsEncrypted;
        final FileHeader mmFileHeader;
        FileInfo(FileHeader fh){
            mmFileHeader = fh;
            mmDirs = fh.getFileNameString();
            mmIsDir = fh.isDirectory();
            mmIsEncrypted = fh.isEncrypted();
            int index = mmDirs.lastIndexOf(sep);
            if(-1 == index)
                mmDisplayDir = mmDirs;
            else
                mmDisplayDir = mmDirs.substring(index+1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rar_show);
        mListView = (ListView) findViewById(R.id.rar_show);
        mDialog = new ProgressDialog(this);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(true);
        mRarPath = "/sdcard/ww.rar";
        mOutDir = mRarPath.substring(0, mRarPath.length() - 4) + File.separator;
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int p, long id) {
                logd("click " + v);
                TextView tv = (TextView) v.findViewById(R.id.item);
                FileInfo fi = (FileInfo) tv.getTag();
                if (fi.mmIsDir)
                    new MyTask().execute(fi.mmDirs);
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int arg2, long arg3) {
                logd("onLongClick() " + v);
                TextView tv = (TextView) v.findViewById(R.id.item);
                final FileInfo fi = (FileInfo) tv.getTag();
                AlertDialog.Builder b = new AlertDialog.Builder(RarShow.this);
                b.setTitle("unRar ?");
                b.setMessage("File:" + fi.mmDirs);
                b.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int id) {
                                new UnrarTask().execute(fi);
                            }
                        }
                );
                b.create().show();
                return false;
            }
        });
        new MyTask().execute("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            mRarFile = new Archive(new File(mRarPath));
        }catch (Exception e){
            loge("new Archive() failed..");
            Toast.makeText(this, e+"",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        try {
            mRarFile.close();
        }catch(IOException e){
            loge("onStop() close fail.");
        }
        super.onStop();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }

    @Override
    public void onBackPressed() {
        if (mParent != null && mParent.lastIndexOf(sep) != -1) {
            int index = mParent.indexOf(sep), lastindex = mParent
                    .lastIndexOf(sep);
            if (index == lastindex) // only 1 separator
                new MyTask().execute("");
            else {
                String sub = mParent.substring(0, mParent.lastIndexOf(sep));
                lastindex = sub.lastIndexOf(sep);
                sub = sub.substring(0, lastindex + 1);
                new MyTask().execute(sub);
            }
        } else
            super.onBackPressed();
    }

    class UnrarTask extends AsyncTask<FileInfo, String, Integer> {
        @Override
        protected void onPreExecute() {
            mDialog.setMessage("extracting...");
            mDialog.show();
        }

        @Override
        protected Integer doInBackground(FileInfo... arg) {
            return unrar(arg[0])?0:-1;
        }

        @Override
        protected void onPostExecute(Integer r) {
            if (mDialog != null && mDialog.isShowing())
                mDialog.dismiss();
            if (r == 0)
                Toast.makeText(RarShow.this, "Compiled! ",
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(RarShow.this, "Failed! ",
                        Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDialog.setMessage(values[0]);
        }

        /**
         * 解压文件 或 递归的解压文件夹
         * */
        private boolean unrar(FileInfo fi) {
            final String pre = fi.mmDirs;
            List<FileHeader> list = mRarFile.getFileHeaders();
            for (FileHeader fh : list) {
                if (!fh.getFileNameString().startsWith(pre))
                    continue;
                boolean success = Utils.extractOneFileHeader(mRarFile, fh, mOutDir);
                if (!success)
                    return false;
            }
            return true;
        }
    }// end class UnzipTask

    class MyTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            // mDirs.clear(); // 导致 mCaches无效 每次都查询
            mDialog.setMessage("Opening ... ");
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... as) {
            String parent = as[0];
            logd("task, parent = " + parent);
            FileHeader fh = null;
            try {
                if (mAllDirs.isEmpty()) {   //扫描所有文件列表 添加到列表 mAllDirs
                    mRarFile = new Archive(new File(mRarPath));//RarException
                    if(mRarFile.isEncrypted()){
                        loge("rar File is encrypted. ");
                        return "Error";
                    }
                    while (true){
                        fh = mRarFile.nextFileHeader();
                        if(fh == null) break;
                        String fns = fh.getFileNameString();
                        if(fh.isEncrypted()){
                            loge(fh.getFileNameString()+" is encrypted.");
                            continue;
                        }
                        logd("find fns="+fns /*getFileNameW() is empty.*/ );
                        mAllDirs.add(new FileInfo(fh));
                    }
//                    mRarFile.close();
                }
                mDirs = getDirs(mAllDirs, parent);
            } catch (Exception e) {
                loge("scan all file list : " + e);
            }
            return parent;
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected void onPostExecute(String result) {
            if (mDialog != null && mDialog.isShowing())
                mDialog.dismiss();
            adapter.notifyDataSetChanged();
            setTitle(mRarPath + result);
            mParent = result;
        }
    }

    final BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mDirs.size();
        }

        @Override
        public Object getItem(int a) {
            return a;
        }

        @Override
        public long getItemId(int a) {
            return a;
        }

        @Override
        public View getView(int i, View v, ViewGroup vg) {
            if (v == null) {
                v = View.inflate(RarShow.this, R.layout.zip_show_item, null);
            }
            TextView tv = (TextView) v.findViewById(R.id.item);
            tv.setText(mDirs.get(i).mmDisplayDir);
            tv.setTag(mDirs.get(i));
            return v;
        }
    };

    private ArrayList<FileInfo> getDirs(String parent) {
        return getDirs(mAllDirs, parent);
    }

    /**
     * 从zip文件的所有文件列表 dirs中 筛选出目录parent下的直接文件或目录
     * */
    private ArrayList<FileInfo> getDirs(ArrayList<FileInfo> dirs, String parent) {
        if (mCaches.containsKey(parent)) {
            return mCaches.get(parent);
        }
        ArrayList<FileInfo> childs = new ArrayList<FileInfo>();
        if(TextUtils.isEmpty(parent)){
            for (FileInfo fi : dirs) {
                if (fi.mmIsDir) {
                    if (!fi.mmDirs.contains(sep)) {
                        childs.add(fi);
                    }
                } else {    //File
                    if(!fi.mmDirs.contains(sep)){
                        childs.add(fi);
                    }
                }
            }
            logd("put key: " + parent + " ,size =" + childs.size());
            return childs;
        }
        for (FileInfo fi : dirs) {
            String str = fi.mmDirs;
            logd("parent: " + parent + " , str: " + str);
            if (!str.startsWith(parent) || str.equals(parent))
                continue;
            str = str.substring(parent.length());//去除前面的parent一样的string
            if (str.startsWith(sep))
                str = str.substring(1);
            if (!str.contains(sep))
                childs.add(fi);
//            if (fi.mmIsDir && !str.contains(sep))    //若是目录 且不再含分隔符 add
//                childs.add(fi);
//            else if (!fi.mmIsDir && !str.contains(sep) ) { //若是文件 但不包括分隔符 add
//                childs.add(fi);
//            }
        }
        mCaches.put(parent, childs);
        logd("put key: " + parent + " ,size =" + childs.size());
        return childs;
    }


    /** 遍历,打印list中的元素 */
    void logList(List<?> list) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list)
            sb.append(o.toString() + ",");
        logd(sb.toString());
    }

    private void logd(String s) {
        Log.d("orient", "[RarShow]"+s);
    }

    private static void loge(String e) {
        Log.e("orient", "[RarShow]"+e);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.rar_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
