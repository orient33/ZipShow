package com.example.zipshow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ZipShow extends Activity {

	final String sep = File.separator;
	String mZipPath, mParent;
	ListView mListView;
	ArrayList<String> mAllDirs = new ArrayList<String>();
	ArrayList<String> mDirs = new ArrayList<String>();
	HashMap<String, ArrayList<String>> mCaches = new HashMap<String, ArrayList<String>>();
	ProgressDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zip_show);
		mListView = (ListView) findViewById(R.id.zip_shou);
		mDialog = new ProgressDialog(this);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.setCancelable(true);
		mZipPath = "/sdcard/wfe.zip";
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int p, long id) {
				logd("click " + v);
				TextView tv = (TextView) ((LinearLayout) v)
						.findViewById(R.id.item);
				String text = tv.getText().toString();
				if (isDirectory(text)) {
					if (TextUtils.isEmpty(mParent) || sep.equals(mParent))
						new MyTask().execute(text);
					else
						new MyTask().execute(mParent + text);
				}
			}
		});
		new MyTask().execute("");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mDialog != null && mDialog.isShowing())
			mDialog.dismiss();
	}

	@Override
	public void onBackPressed() {
		if (sep.equals(mParent)) {
			super.onBackPressed();
		} else if (mParent != null && mParent.lastIndexOf(File.separator) != -1) {
			int index = mParent.indexOf(File.separatorChar), lastindex = mParent
					.lastIndexOf(File.separatorChar);
			if (index == lastindex) // only 1 separator
				new MyTask().execute("");
			else {
				String sub = mParent.substring(0,
						mParent.lastIndexOf(File.separator));
				lastindex = sub.lastIndexOf(File.separator);
				sub = sub.substring(0, lastindex + 1);
				new MyTask().execute(sub);
			}
		} else
			super.onBackPressed();
	}

	private boolean isDirectory(String path) {
		boolean r = false;
		try {
			ZipFile file = new ZipFile(mZipPath);
			ZipEntry en = new ZipEntry(path);
			r = en.isDirectory();
			file.close();
			return r;
		} catch (IOException e) {
			loge("" + e);
		} finally {
		}
		return r;
	}

	class MyTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
//			mDirs.clear(); // 导致 mCaches无效 每次都查询
			mDialog.setMessage("Opening ... ");
			mDialog.show();
		}

		@Override
		protected String doInBackground(String... as) {
			String parent = as[0];
			if (TextUtils.isEmpty(parent))
				parent = sep;
			logd("task, parent = " + parent);
			try {
				if (mAllDirs.isEmpty()) {
					ZipFile zipFile = new ZipFile(mZipPath);
					Enumeration<?> es = zipFile.entries();
					ZipEntry zipEntry = null;

					while (es.hasMoreElements()) {
						zipEntry = (ZipEntry) es.nextElement();
						String s = zipEntry.getName();
						mAllDirs.add(s);
						// logd( "find directly :" + s);
					}
					zipFile.close();
				}
				mDirs = getDirX(mAllDirs, parent);
			} catch (IOException e) {
				loge("" + e);
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
			setTitle(result);
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
				v = View.inflate(ZipShow.this, R.layout.zip_show_item, null);
			}
			TextView tv = (TextView) ((LinearLayout) v).findViewById(R.id.item);
			tv.setText(mDirs.get(i));
			return v;
		}

	};

	/**
	 * 从zip文件的所有文件列表 dirs中 筛选出目录parent下的直接文件或目录
	 * */
	private ArrayList<String> getDirX(ArrayList<String> dirs, String parent) {
		if (mCaches.containsKey(parent)) {
			return mCaches.get(parent);
		}

		int index = -1;
		ArrayList<String> childs = new ArrayList<String>();
		if (parent == null || parent.length() == 0 || sep.equals(parent)) {
			for (String str : dirs) {
				index = str.indexOf(sep);
				if (-1 == index || index == str.length() - 1)
					childs.add(str);
				else {
					str = str.substring(0, index + 1);
					if (!childs.contains(str))
						childs.add(str);
				}
			}
			mCaches.put(sep, childs);
			logd("put key '.' ,size =" + childs.size());
		} else {
			for (String str : dirs) {
				if (!str.startsWith(parent))
					continue;
				str = str.substring(parent.length());
				index = str.indexOf(sep);
				if ((-1 == index || index == str.length()) && str.length() > 0) {
					childs.add(str);
				} else {
					str = str.substring(0, index + 1);
					if (!childs.contains(str) && !TextUtils.isEmpty(str)) {
						childs.add(str);
					}
				}
			}
			mCaches.put(parent, childs);
			logd("put key: " + parent + " ,size =" + childs.size());
		}
		return childs;
	}

	void logList(List<?> list) {
		StringBuilder sb = new StringBuilder();
		for (Object o : list)
			sb.append(o.toString() + ",");
		logd(sb.toString());
	}

	private void logd(String s) {
		Log.d("orient", s);
	}

	private void loge(String e) {
		Log.e("orient", e);
	}
}
