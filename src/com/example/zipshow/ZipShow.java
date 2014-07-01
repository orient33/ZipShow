package com.example.zipshow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.zipshow.R;

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
				if (isDirectory(mParent + text)) {
					if (TextUtils.isEmpty(mParent) /* || sep.equals(mParent) */)
						new MyTask().execute(text);
					else
						new MyTask().execute(mParent + text);
				}
			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View v,
					int arg2, long arg3) {
				logd("onLongClick() " + v);
				TextView tv = (TextView) ((LinearLayout) v)
						.findViewById(R.id.item);
				final String text = tv.getText().toString();
				AlertDialog.Builder b = new AlertDialog.Builder(ZipShow.this);
				b.setTitle("unzip ?");
				b.setMessage("File:" + text);
				b.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface di, int id) {
								new UnzipTask().execute(text);
							}
						});
				b.create().show();
				return false;
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

	/**
	 * 判断 path在压缩包中是否为文件
	 * 
	 * @param path
	 *            在压缩包的位置<br>
	 *            若当前mParent为 a/， path为 b,则entryName为 a/b
	 * */
	private boolean isDirectory(String path) {
		boolean r = false;
		try {
			ZipFile file = new ZipFile(mZipPath);
			ZipEntry en = new ZipEntry(path);
			r = en.isDirectory();
			file.close();
		} catch (IOException e) {
			loge("" + e);
		} finally {
		}
//		logd(path + " is Directory? " + r);
		return r;
	}

	class UnzipTask extends AsyncTask<String, String, Integer> {
		@Override
		protected void onPreExecute() {
			mDialog.setMessage("unziping...");
			mDialog.show();
		}

		@Override
		protected Integer doInBackground(String... arg) {
			return unzip(mParent + arg[0]);
		}

		@Override
		protected void onPostExecute(Integer r) {
			if (mDialog != null && mDialog.isShowing())
				mDialog.dismiss();
			if (r == 0)
				Toast.makeText(ZipShow.this, "unzip Compiled! ",
						Toast.LENGTH_LONG).show();
			else
				Toast.makeText(ZipShow.this, "unzip Failed! ",
						Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			mDialog.setMessage(values[0]);
		}

		/** 
		 * @param path entryName的绝对路径/全名
		 * */
		private int unzip(String path) {
			int result = -1;
			if (!isDirectory(path)) { // just a file; path is file name.
				result = unzipOneFile(mParent + path);
			} else { // path is a Dirctory
//				logd("unzip Dir:" + path);
				ArrayList<String> childs = getDirs(path);
				for (String child : childs) {
					if (isDirectory(path + child)) {
						unzip(path + child);
					} else {
						result = unzipOneFile(path + child);
						if (result != 0)
							break;
					}
				}
			}
			return result;
		}

		/**
		 * 解压缩一个文件
		 * 
		 * @param entryName
		 *            文件在压缩包中的entry
		 * @param des
		 *            文件解压后存放的位置
		 * @return 解压成功时返回0 失败时返回其他值.
		 * */
		private int unzipOneFile(String entryName) {
			final String des = mZipPath.substring(0, mZipPath.length() - 4)+sep + entryName;
			ZipFile file = null;
			logd("entryName=" + entryName + ",des=" + des);
			publishProgress(entryName);
			try {
				file = new ZipFile(mZipPath);
				InputStream is = file.getInputStream(new ZipEntry(entryName));
				File desFile = new File(des);
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
				is.close();
				out.close();
				file.close();
			} catch (IOException e) {
				loge("copy failed! " + e.toString());
				return -1;
			}
			return 0;
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
			try {
				if (mAllDirs.isEmpty()) {
					ZipFile zipFile = new ZipFile(mZipPath);
					Enumeration<?> es = zipFile.entries();
					ZipEntry zipEntry = null;

					while (es.hasMoreElements()) {
						zipEntry = (ZipEntry) es.nextElement();
						String s = zipEntry.getName();
						if (!s.endsWith(sep)) // ignore string end with /
							mAllDirs.add(s);
//						logd("find directly :" + s);
					}
					zipFile.close();
				}
				mDirs = getDirs(mAllDirs, parent);
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

	private ArrayList<String> getDirs(String parent) {
		return getDirs(mAllDirs, parent);
	}

	/**
	 * 从zip文件的所有文件列表 dirs中 筛选出目录parent下的直接文件或目录
	 * */
	private ArrayList<String> getDirs(ArrayList<String> dirs, String parent) {
		if (mCaches.containsKey(parent)) {
			return mCaches.get(parent);
		}
		int index = -1;
		ArrayList<String> childs = new ArrayList<String>();
		if (parent == null || parent.length() == 0) {
			for (String str : dirs) {
				index = str.indexOf(sep);
				if (-1 == index /* || index == str.length() - 1 */)
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

	/** 遍历,打印list中的元素 */
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
