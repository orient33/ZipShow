package com.example.extract;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class ActivityExtract extends Activity {

    String mPath;   //要浏览的压缩包文件的绝对路径    eg. /sdcard/a.zip
    String mParent = ""; //当前浏览的目录在压缩包中的相对路径 eg. dir  表示当前浏览的是/sdcard/a.zip文件中的 dir目录
    AbstractExtract mAbstractExtract;
    ListView mListView;
    ArrayList<ItemInfo> mDirs = new ArrayList<ItemInfo>();
    ProgressDialog mDialog;
    String sep = File.separator; //for zip. if rar, should be \\

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract);
        mDialog = new ProgressDialog(this);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(true);
        mListView = (ListView) findViewById(R.id.list_extract);
        mListView.setAdapter(mAdapter);
        mPath = getIntent().getStringExtra("dst");
        if (TextUtils.isEmpty(mPath)) {
            Uri uri = getIntent().getData();
            if (uri != null) {
                mPath = uri.toString(); //uri eg.  fle:///mnt/sdcard/wfe.zip
                logd("uri: "+uri+",, schema: "+uri.getScheme());    // schema file
                if(mPath.startsWith("file://"))
                    mPath = mPath.substring("file://".length());
            }else
                mPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ww.rar";//"wfe.zip";
        }
        if(mPath.toLowerCase().endsWith(".rar"))
            sep = "\\";
        mAbstractExtract = AbstractExtract.create(mPath);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int p, long id) {
                ItemInfo ii = (ItemInfo) v.getTag();
                logd("click() " + ii.entryName + " ;isDir?" + ii.isDir);
                if (ii.isDir) {
                    new BrowserTask().execute(ii.entryName);
                }
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int arg2, long arg3) {
                final ItemInfo ii = (ItemInfo) v.getTag();
                logd("onLongClick() " + ii.entryName);
                AlertDialog.Builder b = new AlertDialog.Builder(ActivityExtract.this);
                b.setTitle("Extract ?");
                b.setMessage("File:" + ii.entryName);
                b.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int id) {
                                new ExtractTask().execute(ii);
                            }
                        }
                );
                b.create().show();
                return false;
            }
        });
        new BrowserTask().execute(mParent);
    }

    final BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mDirs.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null)
                view = View.inflate(ActivityExtract.this, R.layout.extract_item, null);
            ItemInfo ii = mDirs.get(i);
            view.setTag(ii);
            TextView tv = (TextView) view.findViewById(R.id.item_text);
            String displayName = ii.entryName.substring(mParent.length());
            if (displayName.startsWith(ii.sep))    //remove / or \
                displayName = displayName.substring(1);
            int index = displayName.indexOf(ii.sep);
            if (-1 != index)
                displayName = displayName.substring(0, index);
            tv.setText(displayName);
            return view;
        }
    };
    @Override
    public void onBackPressed() {
        if (mParent != null && mParent.lastIndexOf(sep) != -1) {
            int index = mParent.indexOf(sep), lastindex = mParent
                    .lastIndexOf(sep);
            if (index == lastindex) // only 1 separator
                new BrowserTask().execute("");
            else {
                String sub = mParent.substring(0, mParent.lastIndexOf(sep));
                lastindex = sub.lastIndexOf(sep);
                sub = sub.substring(0, lastindex + 1);
                new BrowserTask().execute(sub);
            }
        } else
            super.onBackPressed();
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAbstractExtract != null)
            mAbstractExtract.finish();
    }

    class BrowserTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            mDialog.setMessage("opening...");
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... arg) {
            mAbstractExtract.initAllItems();
            mDirs = mAbstractExtract.getItemsForDir(arg[0]);
            return arg[0];
        }

        @Override
        protected void onPostExecute(String r) {
            if (mDialog != null && mDialog.isShowing())
                mDialog.dismiss();
            setTitle(r);
            mAdapter.notifyDataSetChanged();
            mParent = r;
        }
    }


    class ExtractTask extends AsyncTask<ItemInfo, String, Integer> {
        @Override
        protected void onPreExecute() {
            mDialog.setMessage("opening...");
            mDialog.show();
        }

        @Override
        protected Integer doInBackground(ItemInfo... arg) {
            return mAbstractExtract.extractDirs(arg, "");
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Integer r) {
            if (mDialog != null && mDialog.isShowing())
                mDialog.dismiss();
            if (r == 0)
                Toast.makeText(ActivityExtract.this, "unzip Compiled! ",
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ActivityExtract.this, "unzip Failed! ",
                        Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.extract, menu);
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

    private void logd(String s) {
        Log.d("orient", s);
    }

    private void loge(String e) {
        Log.e("orient", e);
    }
}
