package com.gechao.weather;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.gechao.weather.bean.City;
import com.gechao.weather.bean.Province;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private TextView tvTitle;
	private ListView lvList;
	private TextView tvWea;
	private Button btnPro;
	private Button btnCity;
	private ProgressBar pb;

	private int Status;
	private List<Province> proList;
	private List<City> cityList;
	private HashMap<String, String> params;
	private StringBuffer sb;

	private String proCode = "";
	private String cityCode = "";

	public static final String PRO_PATH = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getRegionProvince";
	public static final String CITY_PATH = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getSupportCityString";
	public static final String WEA_DEATIL = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather";
	Handler mhandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				new MyThread(0).start();
				break;
			case 1:// 开始查询城市
				new MyThread(1).start();// 立即开始查询城市数据
				btnCity.setFocusable(false);
				pb.setVisibility(View.VISIBLE);
				break;
			case 2:// 拿到数据
				btnCity.setFocusable(true);
				pb.setVisibility(View.GONE);

				break;
			case 3:// 拿到数据
				pb.setVisibility(View.GONE);
				tvWea.setText(sb.toString());

				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		InitUI();
		ItitData();

	}

	private void InitUI() {
		setContentView(R.layout.activity_main);
		tvWea = (TextView) findViewById(R.id.tv_wea);
		btnPro = (Button) findViewById(R.id.btn_pro);
		btnCity = (Button) findViewById(R.id.btn_city);
		pb = (ProgressBar) findViewById(R.id.pb);
	}

	private void ItitData() {
		params = new HashMap<String, String>();
		// mhandler.sendEmptyMessage(0);
		new MyThread(0).start();
	}

	// 处理两个按钮的点击事件
	public void chooseProvince(View v) {
		ShowProDialog();

	}

	public void chooseCity(View v) {
		System.out.println(proCode);
		if (TextUtils.isEmpty(proCode)) {
			Toast.makeText(MainActivity.this, "请先选择省份", Toast.LENGTH_LONG).show();
			return;
		}
		ShowCityDialog();
	}

	/**
	 * 选择省份的Dialog
	 */
	private void ShowProDialog() {
		if (proList.size() == 0) {
			Toast.makeText(MainActivity.this, "正在初始化数据···", Toast.LENGTH_LONG).show();
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final AlertDialog proDialog = builder.create();

		View view = View.inflate(this, R.layout.dialog, null);
		// dialog.setView(view);// 将自定义的布局文件设置给dialog
		proDialog.setView(view, 0, 0, 0, 0);// 设置边距为0,保证在2.x的版本上运行没问题

		tvTitle = (TextView) view.findViewById(R.id.tv_title);
		lvList = (ListView) view.findViewById(R.id.lv_list);
		tvTitle.setText("选择省份");
		lvList.setAdapter(new ProAdapter());

		proDialog.show();

		lvList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// System.out.println(proList.get(position).getName());
				// Toast.makeText(MainActivity.this,
				// proList.get(position).getName(), Toast.LENGTH_LONG).show();
				proDialog.dismiss();
				btnPro.setText(proList.get(position).getName());
				proCode = proList.get(position).getId();
				mhandler.sendEmptyMessage(1);

			}
		});

	}

	/**
	 * 选择城市的Dialog
	 */
	private void ShowCityDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final AlertDialog proDialog = builder.create();

		View view = View.inflate(this, R.layout.dialog, null);
		// dialog.setView(view);// 将自定义的布局文件设置给dialog
		proDialog.setView(view, 0, 0, 0, 0);// 设置边距为0,保证在2.x的版本上运行没问题

		tvTitle = (TextView) view.findViewById(R.id.tv_title);
		lvList = (ListView) view.findViewById(R.id.lv_list);
		tvTitle.setText("选择城市");
		lvList.setAdapter(new CityAdapter());

		proDialog.show();

		lvList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// System.out.println(proList.get(position).getName());

				proDialog.dismiss();
				btnCity.setText(cityList.get(position).getName());
				cityCode = cityList.get(position).getId();
				new MyThread(2).start();// 立即开始查询天气数据
				pb.setVisibility(View.VISIBLE);
			}
		});

	}

	class MyThread extends Thread {

		public MyThread(int staatus) {
			super();
			Status = staatus;
		}

		@Override
		public void run() {
			switch (Status) {
			case 0:
				try {
					System.out.println("正在加载数据");
					proList = new ArrayList<Province>();
					System.out.println("开始获取数据");
					URL urlPro = new URL(PRO_PATH);
					Document document = Jsoup.connect(urlPro.toString()).get();
					Elements pros = document.getElementsByTag("string");
					for (Element element : pros) {
						Province province = new Province();
						// System.out.println(element.text());
						String[] pro = element.text().split(",");
						province.setName(pro[0]);
						province.setId(pro[1]);
						proList.add(province);

					}

					// for (Province pro : proList) {
					// System.out.println(pro.toString());
					// }

				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					System.out.println("请求省份列表错误！");
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("连接出错");
					e.printStackTrace();
				}

				break;

			case 1:// 根据省份代码获取城市信息

				try {
					cityList = new ArrayList<City>();
					params.clear();
					URL urlCity = new URL(CITY_PATH);
					params.put("theRegionCode", proCode);
					Response response = Jsoup.connect(urlCity.toString()).data(params).execute();
					// System.out.println(response.body());
					Document doc = Jsoup.parse(response.body());
					Elements citys = doc.getElementsByTag("string");
					for (Element element : citys) {
						City city = new City();
						String[] citydata = element.text().split(",");
						city.setName(citydata[0]);
						city.setId(citydata[1]);
						cityList.add(city);
					}
					// for (City city : cityList) {
					// System.out.println(city.toString());
					// }
					mhandler.sendEmptyMessage(2);// 通知拿到城市数据
				} catch (MalformedURLException e) {
					e.printStackTrace();
					System.out.println("URL地址有误");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("连接出错");
					e.printStackTrace();
				}
				break;
			case 2:// 获取具体地区的天气情况

				try {
					params.clear();

					URL urlCity = new URL(WEA_DEATIL);
					params.put("theCityCode", cityCode);
					params.put("theUserID", "");
					Response response = Jsoup.connect(urlCity.toString()).data(params).execute();
					Document doc = Jsoup.parse(response.body());
					Elements info = doc.getElementsByTag("string");
					sb = new StringBuffer();

					for (Element element : info) {
						sb.append(element.text());
					}
					System.out.println(sb.toString());
					mhandler.sendEmptyMessage(3);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					System.out.println("URL地址有误");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("连接出错");
					e.printStackTrace();
				}
				break;
			default:
				break;
			}

		}

	}

	// listView的adapter
	class ProAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return proList.size();
		}

		@Override
		public Object getItem(int position) {
			return proList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(MainActivity.this, R.layout.list_item, null);
				holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			// 开始赋值

			holder.tvName.setText(proList.get(position).getName());
			return convertView;
		}

	}

	class CityAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return cityList.size();
		}

		@Override
		public Object getItem(int position) {
			return cityList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = View.inflate(MainActivity.this, R.layout.list_item, null);
				holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			// 开始赋值

			holder.tvName.setText(cityList.get(position).getName());
			return convertView;
		}

	}

	class ViewHolder {
		TextView tvName;
	}

}
