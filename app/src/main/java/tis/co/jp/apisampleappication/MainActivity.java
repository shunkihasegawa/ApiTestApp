package tis.co.jp.apisampleappication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends Activity implements LocationListener {

    LocationManager locationManager;
    private AsyncTask<Uri,  Void, String> mTask;
    private int numOfHotels;
    private ArrayList<String> hotelName = new ArrayList<String>();
    private ArrayList<String> hotelUrl = new ArrayList<String>();
    private ArrayList<String> hotelAccess = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onLocationChanged(Location location) {
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());

        // 【重要】緯度経度が1度でも取得できた時点で処理を終了
        locationManager.removeUpdates(this);

        // 取得した緯度経度を使って処理を行う（処理の詳細は省略）

        TextView tv_lat = (TextView)findViewById(R.id.latitude);
        TextView tv_lng = (TextView)findViewById(R.id.longitude);

        tv_lat.setText(latitude);
        tv_lng.setText(longitude);

        getNearHotelsInfo(latitude,longitude);


    }




    private void getNearHotelsInfo(String lat, String lng) {

        // URLを、扱いやすいUri型で組む
        Uri baseUri = Uri.parse("https://app.rakuten.co.jp/services/api/" +
                "Travel/SimpleHotelSearch/20131024?" +
                //下記の"XXXX"に取得した楽天のアプリIDを入力する
                "applicationId=XXXXXXXXXXXXXXXXXXX");

        // パラメータの付与
        //jsonフォーマットで出力
        Uri uri = baseUri.buildUpon().appendQueryParameter("format", "json").build();
        //緯度を現在地に
        uri = uri.buildUpon().appendQueryParameter("latitude", lat).build();
        //経度を現在地に
        uri = uri.buildUpon().appendQueryParameter("longitude", lng).build();
        //検索範囲を3.0km
        uri = uri.buildUpon().appendQueryParameter("searchRadius", "3.0").build();
        //キャリアをモバイルに
        uri = uri.buildUpon().appendQueryParameter("carrier", "1").build();
        //緯度経度の表示形式を世界測地系に
        uri = uri.buildUpon().appendQueryParameter("datumType", "1").build();


        if (mTask == null) {
            mTask = new AsyncTask<Uri, Void, String>() {
                /**
                 * 通信において発生したエラー
                 */

                private Throwable mError = null;


                @Override
                protected String doInBackground(Uri... params) {
                    String result = requestDirections(params[0]);
                    if (isCancelled()) {
                        return result;
                    }
                    return result;
                }

                private String requestDirections(Uri uri) {
                    DefaultHttpClient httpClient = new DefaultHttpClient();

                    // タイムアウトの設定
                    HttpParams httpParams = httpClient.getParams();
                    // 接続確立までのタイムアウト設定 (ミリ秒)
                    HttpConnectionParams.setConnectionTimeout(httpParams,
                            5 * 1000);
                    // 接続後までのタイムアウト設定 (ミリ秒)
                    HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);

                    String result = null;
                    HttpGet request = new HttpGet(uri.toString());
                    try {
                        Log.d("info", "connectionStart");
                        result = httpClient.execute(request, new ResponseHandler<String>() {
                            @Override
                            public String handleResponse(HttpResponse response) throws IOException {
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == HttpStatus.SC_OK) {
                                    String result = EntityUtils.toString(response.getEntity());
                                    Log.d("info", "get");
                                    return result;
                                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                                    throw new RuntimeException("404 NOT FOUND");
                                } else {
                                    throw new RuntimeException("そのほかの通信エラー");
                                }
                            }
                        });
                        Log.d("info", "Connection");
                    } catch (RuntimeException e) {
                        mError = e;
                        Log.e("info", "通信失敗", e);
                    } catch (ClientProtocolException e) {
                        mError = e;
                        Log.e("info", "通信失敗", e);
                    } catch (IOException e) {
                        mError = e;
                        Log.e("info", "通信失敗", e);
                    } finally {
                        // リソースを開放する
                        httpClient.getConnectionManager().shutdown();
                    }

                    return result;
                }

                @Override
                protected void onPostExecute(String result) {

                    if (mError == null) {
                        parse(result);
                        setListView();
                    } else {
                        Log.d("postExecute", "error");
                    }

                }

                @Override
                protected void onCancelled() {
                    onCancelled(null);
                }

                @Override
                protected void onCancelled(String result) {
                    Log.e("onCancel", "error");

                }
            }.execute(uri);

        }
    }



    /*
    取得したJsonをパースするメソッド
    */
    private void parse(String json) {

        try {

            JSONObject jsRoot = new JSONObject(json);
            JSONObject jsInfo = jsRoot.getJSONObject("pagingInfo");
            numOfHotels = Integer.parseInt(jsInfo.getString("recordCount"));

            JSONArray jsHotels = jsRoot.getJSONArray("hotels");

            for (int i = 0; i < numOfHotels && i < 30; i++) {

                JSONObject hotelObject =jsHotels.getJSONObject(i);

                JSONArray hotelInfoArray = hotelObject.getJSONArray("hotel");

                JSONObject hotelInfoObject = hotelInfoArray.getJSONObject(0);
                JSONObject basicInfoObject = hotelInfoObject.getJSONObject("hotelBasicInfo");

                hotelName.add(basicInfoObject.getString("hotelName"));
                System.out.println(hotelName.get(i));
                hotelAccess.add(basicInfoObject.getString("access"));
                System.out.println(hotelAccess.get(i));
                hotelUrl.add(basicInfoObject.getString("hotelInformationUrl"));
                System.out.println(hotelUrl.get(i));

            }


        } catch (JSONException e) {
            Log.e("json", "json parse error");
        } catch (Exception e) {
            Log.e("json", "json exception error");
        }
    }

    private void setListView(){

        setContentView(R.layout.hotel_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,hotelName);
        ListView listView = (ListView)findViewById(R.id.HotelList);
        listView.setAdapter(adapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Adapterからタップした位置のデータを取得する
                System.out.println(position);
                Uri hotelUri = Uri.parse(hotelUrl.get(position));
                Intent intent = new Intent(Intent.ACTION_VIEW, hotelUri);
                startActivity(intent);
            }
        });


    }


}
