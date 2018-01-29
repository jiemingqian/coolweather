package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weather_layout;
    private TextView title_city;
    private TextView title_update_time;
    private TextView degree_text;
    private TextView weather_info_text;
    private LinearLayout forecast_layout;
    private TextView aqi_text;
    private TextView pm25_text;
    private TextView comfort_text;
    private TextView car_wash_text;
    private TextView sport_text;
    private ImageView bing_pic_img;
    public SwipeRefreshLayout swipe_refresh;
    public DrawerLayout drawer_layout;
    private Button nav_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        bing_pic_img = findViewById(R.id.bing_pic_img);
        weather_layout = findViewById(R.id.weather_layout);
        title_city= findViewById(R.id.title_city);
        title_update_time= findViewById(R.id.title_update_time);
        degree_text= findViewById(R.id.degree_text);
        weather_info_text= findViewById(R.id.weather_info_text);
        forecast_layout= findViewById(R.id.forecast_layout);
        aqi_text= findViewById(R.id.aqi_text);
        pm25_text= findViewById(R.id.pm25_text);
        comfort_text= findViewById(R.id.comfort_text);
        car_wash_text= findViewById(R.id.car_wash_text);
        sport_text= findViewById(R.id.sport_text);
        swipe_refresh = findViewById(R.id.swipe_refresh);
        swipe_refresh.setColorSchemeResources(R.color.colorPrimary);
        drawer_layout = findViewById(R.id.drawer_layout);
        nav_button = findViewById(R.id.nav_button);

        nav_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer_layout.openDrawer(GravityCompat.START);
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingpic = prefs.getString("bing_pic",null);
        if (bingpic != null) {
            Glide.with(this).load(bingpic).into(bing_pic_img);
        }else {
            loadBingPic();
        }

        String weatherstring = prefs.getString("weather",null);
        final String weatherId;
        if (weatherstring != null) {
            Weather weather = Utility.handleWeatherResponse(weatherstring);
            weatherId = weather.basic.weatherId;
            ShowWeatherInfo(weather);
        } else {
            weatherId = getIntent().getStringExtra("weather_id");
            weather_layout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        swipe_refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }

    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingpic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingpic);
                editor.apply();
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(WeatherActivity.this).load(bingpic).into(bing_pic_img);
                            }
                        }
                );
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=eab5be7080504ee58c765e8ef7960e4a";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                 final String responseText = response.body().string();
                 final Weather weather = Utility.handleWeatherResponse(responseText);
                 runOnUiThread(
                         new Runnable() {
                             @Override
                             public void run() {
                                 if (weather != null && "ok".equals(weather.status)) {
                                     SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                     editor.putString("weather",responseText);
                                     editor.apply();
                                     ShowWeatherInfo(weather);
                                 }else {
                                     Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                                 }
                                 swipe_refresh.setRefreshing(false);
                             }
                         }
                 );
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                                swipe_refresh.setRefreshing(false);
                            }
                        }
                );
            }
        });
        loadBingPic();
    }

    private void ShowWeatherInfo(Weather weather) {
        if (weather != null && "ok".equals(weather.status)) {
            String cityName = weather.basic.cityName;
            String updateTime = weather.basic.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature + "℃";
            String weatherInfo = weather.now.more.info;
            title_city.setText(cityName);
            title_update_time.setText(updateTime);
            degree_text.setText(degree);
            weather_info_text.setText(weatherInfo);
            forecast_layout.removeAllViews();
            for (Forecast forecast : weather.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecast_layout, false);
                TextView date_text = view.findViewById(R.id.date_text);
                TextView info_text = view.findViewById(R.id.info_text);
                TextView max_text = view.findViewById(R.id.max_text);
                TextView min_text = view.findViewById(R.id.min_text);
                date_text.setText(forecast.date);
                info_text.setText(forecast.more.info);
                max_text.setText(forecast.temperature.max);
                min_text.setText(forecast.temperature.min);
                forecast_layout.addView(view);
            }
            if (weather.aqi != null) {
                aqi_text.setText(weather.aqi.city.aqi);
                pm25_text.setText(weather.aqi.city.pm25);
            }
            String comfort = "舒适度：" + weather.suggestion.comfort.info;
            String carWash = "洗车指数：" + weather.suggestion.carWash.info;
            String sport = "运动建议：" + weather.suggestion.sport.info;
            comfort_text.setText(comfort);
            car_wash_text.setText(carWash);
            sport_text.setText(sport);
            weather_layout.setVisibility(View.VISIBLE);

            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);
        }else {
            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
        }
    }
}
