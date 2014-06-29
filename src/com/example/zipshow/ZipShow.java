package com.example.zipshow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
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

	String mZipPath, mParent;
	ListView mListView;
	ArrayList<String> mAllDirs = new ArrayList<String>();
	ArrayList<String> mDirs = new ArrayList<String>();
	HashMap<String, ArrayList<String>> mCaches = new HashMap<String, ArrayList<String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zip_show);
		mListView = (ListView) findViewById(R.id.zip_shou);
		mZipPath = "/sdcard/wfe.zip";
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int p, long id) {
				Log.d("dfdun", "click " + v);
				TextView tv = (TextView) ((LinearLayout) v)
						.findViewById(R.id.item);
				String text = tv.getText().toString();
				if (isDirectory(text))
					new MyTask().execute(text);
			}
		});
		new MyTask().execute("");
	}

	@Override
	public void onBackPressed() {
		if (mParent != null && mParent.lastIndexOf(File.separator) != -1) {
			String sub = mParent.substring(0,
					mParent.lastIndexOf(File.separator));
			new MyTask().execute(sub);
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
			Log.e("dfdun", "" + e);
		} finally {
		}
		return r;
	}

	class MyTask extends AsyncTask<String, String, String> {
		@Override
		protected void onPreExecute() {
			mDirs.clear();
		}

		@Override
		protected String doInBackground(String... as) {
			final String parent = as[0];
			Log.d("dfdun", "task, parent = " + parent);
			try {
				if (mAllDirs.isEmpty()) {
					ZipFile zipFile = new ZipFile(mZipPath);
					Enumeration<?> es = zipFile.entries();
					ZipEntry zipEntry = null;

					while (es.hasMoreElements()) {
						zipEntry = (ZipEntry) es.nextElement();
						String s = zipEntry.getName();
						mAllDirs.add(s);
						Log.d("dfdun", "find directly :" + s);
					}
					zipFile.close();
				}
				mDirs = getDirX(mAllDirs, parent);
				mDirs.remove(parent);
			} catch (IOException e) {
				Log.e("dfdun", "" + e);
			}
			return parent;
		}

		@Override
		protected void onProgressUpdate(String... values) {
		}

		@Override
		protected void onPostExecute(String result) {
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

	private ArrayList<String> getDirX(ArrayList<String> dirs, String parent) {
		if (mCaches.containsKey(parent))
			return mCaches.get(parent);
		final String sep = File.separator;
		ArrayList<String> childs = new ArrayList<String>();
		if (parent == null || parent.length() == 0) {
			for (String str : dirs) {
				if (!str.contains(sep) || str.indexOf(sep) == str.length() - 1)
					childs.add(str);
			}
			mCaches.put("", childs);
		} else {
			for (String str : dirs) {
				if (str.startsWith(parent))
					childs.add(str);
			}
			mCaches.put(parent, childs);
		}
		return childs;
	}
}
