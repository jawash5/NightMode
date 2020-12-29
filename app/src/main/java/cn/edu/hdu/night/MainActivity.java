package cn.edu.hdu.night;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "测试";

    Button switchButton; // 总开关
    Button nightButton; // 夜间模式开关
    Button readButton; // 阅读模式开关
    Button gameButton; // 游戏模式开关
    Dialog dialog; //遮罩层
    SeekBar seekBar; // 进度条
    TextView percentageText; // 百分比文字显示
    ImageView imageView; //dialog中的图片，即通过设置图片颜色，调整滤镜
    Button timingButton; // 定时器按钮
    Button autoButton; //光传感器开关按钮

    private Boolean isON = true; // 是否开启
    private Boolean openAutoMonitor = true; //自动开启关闭监听
    int mode = 0; //模式：0：夜间模式 1：阅读模式 2：游戏模式
    private int[][] modeParameter = {{20,0,0,0,0},
            {20,82,52,0,0}, {20,84,206,95,0}}; // 亮度、红、绿、蓝、进度条原始值

//    private AlarmManagerUtils alarmManagerUtils;


    private SensorManager sensorManager;  //感应器管理器


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission(); //权限检测
        initView(); //初始化赋值
        getData(); //初始化用户设置
        openWindow(); //打开dialog窗口
        initListener(); //初始化监听器

//        //定时器工具类
//        alarmManagerUtils = AlarmManagerUtils.getInstance(MainActivity.this);
//        alarmManagerUtils.createGetUpAlarmManager();


    }

    public void permission() {
        // 打开应用设置
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1);
            }
        }
    }

    private void initView() {
        switchButton = findViewById(R.id.switchButton);
        nightButton = findViewById(R.id.nightButton);
        readButton = findViewById(R.id.readButton);
        gameButton = findViewById(R.id.gameButton);
        seekBar = findViewById(R.id.seekBar);
        percentageText = findViewById(R.id.percentageText);
        timingButton = findViewById(R.id.timingButton);
        autoButton = findViewById(R.id.autoButton);
    }

    //获取存储 sharePrefrence 保存的数据
    public void getData(){
        SharedPreferences preferences = getSharedPreferences("myPreference", Context.MODE_PRIVATE);
        modeParameter[0][0] = preferences.getInt("nightAlpha",20);
        modeParameter[0][4] = preferences.getInt("nightProgress",10);
        modeParameter[1][0] = preferences.getInt("readAlpha",20);
        modeParameter[1][4] = preferences.getInt("readProgress",10);
        modeParameter[2][0] = preferences.getInt("gameAlpha",20);
        modeParameter[2][4] = preferences.getInt("gameProgress",10);
        openAutoMonitor = preferences.getBoolean("openAutoMonitor", true);
    }

    //打开 dailog 窗口,对 dailog 初始化
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openWindow() {
        dialog = new Dialog(this, R.style.dialog_translucent);
        dialog.setContentView(R.layout.dialog);

        WindowManager.LayoutParams lp = Objects.requireNonNull(dialog.getWindow()).getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.flags =WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // 把该window之外的任何event发送到该window之后的其他window.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | //不能获取焦点
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;  //不能触摸，设置不影响下层的触碰

        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; //悬浮窗设置

        dialog.getWindow().setAttributes(lp);
        imageView = dialog.findViewById(R.id.ll_main);
        dialog.show();

        seekBar.setProgress(modeParameter[mode][4]); // 设置seekBar的进度数
        String percent = "护眼模式：" + modeParameter[mode][4] + "%";
        percentageText.setText(percent); // 进度条数值显示
        imageView.setBackgroundColor(Color.argb(modeParameter[mode][0],modeParameter[mode][1],
                modeParameter[mode][2],modeParameter[mode][3]));

        if (openAutoMonitor) {
            registerLightMSensor();
        }
    }

    private void initListener() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                modeParameter[mode][4] = progress;
                modeParameter[mode][0] = progress * 2;

                // 修改dialog的背景色
                imageView.setBackgroundColor(Color.argb(modeParameter[mode][0], modeParameter[mode][1],
                        modeParameter[mode][2], modeParameter[mode][3]));

                String percent = "护眼模式：" + modeParameter[mode][4] + "%"; // 进度条数值显示
                percentageText.setText(percent);
                saveData(); //保存用户的修改数据
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!isON) {
                    Toast.makeText(MainActivity.this,
                            "请先点击开始", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 总开关按钮监听器
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isON) {
                    dialog.dismiss();
                    switchButton.setText("开启");
                } else {
                    dialog.show();
                    switchButton.setText("关闭");
                }
                isON = !isON;
            }
        });

        // 夜间模式按钮监听器
        nightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 0;
                imageView.setBackgroundColor(Color.argb(modeParameter[mode][0], modeParameter[mode][1],
                        modeParameter[mode][2], modeParameter[mode][3]));
                seekBar.setProgress(modeParameter[mode][4]);
            }
        });

        // 阅读模式按钮监听器
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 1;
                imageView.setBackgroundColor(Color.argb(modeParameter[mode][0], modeParameter[mode][1],
                        modeParameter[mode][2], modeParameter[mode][3]));
                seekBar.setProgress(modeParameter[mode][4]);
            }
        });

        // 游戏模式模式按钮监听器
        gameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = 2;
                imageView.setBackgroundColor(Color.argb(modeParameter[mode][0], modeParameter[mode][1],
                        modeParameter[mode][2], modeParameter[mode][3]));
                seekBar.setProgress(modeParameter[mode][4]);
            }
        });

        timingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                alarmManagerUtils.getUpAlarmManagerStartWork();
                Toast.makeText(getApplicationContext(),"设置成功",Toast.LENGTH_SHORT).show();
            }
        });

        // 自动开启/关闭监听器
        autoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAutoMonitor = !openAutoMonitor;
                if (openAutoMonitor) {
                    registerLightMSensor();
                    autoButton.setText("已开启");
                } else {
                    autoButton.setText("已关闭");
                    //注销监听器
                    sensorManager.unregisterListener(listener);
                    listener = null;
                }
                SharedPreferences myPreference = getSharedPreferences("myPreference", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = myPreference.edit();
                editor.putBoolean("openAutoMonitor", openAutoMonitor);
                editor.apply();
            }
        });
    }

    private void registerLightMSensor() {
        //获得感应器服务
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //获得光线感应器
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sensor != null) {
            //注册监听器
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(MainActivity.this, "您的手机不支持该服务", Toast.LENGTH_LONG).show();
        }
    }

    //感应器事件监听器
    private SensorEventListener listener = new SensorEventListener() {
        Queue<Float> data = new LinkedList<>();

        //当传感器监测到的数值发生变化时
        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.d(TAG, "onSensorChanged: " + event.values[0]);
            int topCount = 0; //峰值计数器
            int lowCount = 0; //谷值计数器
            if (data.size() == 4) {
                data.poll();
                data.offer(event.values[0]);         // values数组中第一个值就是当前的光照强度
                for (float v : data) {
                    if (v > 500) {
                        topCount++;
                    } else if (v < 50) {
                        lowCount++;
                    }
                }
            } else {
                data.offer(event.values[0]);
            }

            if (mode == 0) {
                if (lowCount == 4 && !isON) {
                    switchButton.callOnClick();
                    Toast.makeText(MainActivity.this, "已自动开启夜间模式",Toast.LENGTH_SHORT).show();
                } else if(topCount == 4 && isON) {
                    switchButton.callOnClick();
                    Toast.makeText(MainActivity.this, "已自动关闭夜间模式",Toast.LENGTH_SHORT).show();
                }
            }
        }


        //当感应器精度发生变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void saveData(){
        SharedPreferences myPreference = getSharedPreferences("myPreference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = myPreference.edit();
        editor.putInt("nightAlpha", modeParameter[0][0]); // 夜间模式亮度
        editor.putInt("nightProgress", modeParameter[0][4]); // 夜间模式进度条值
        editor.putInt("readAlpha", modeParameter[1][0]); // 阅读模式亮度
        editor.putInt("readProgress", modeParameter[1][4]); // 阅读模式进度条值
        editor.putInt("gameAlpha", modeParameter[1][0]); // 游戏模式亮度
        editor.putInt("gameProgress", modeParameter[2][4]); // 游戏模式进度条值
        editor.apply();
    }

    //Activity被销毁
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openAutoMonitor) {
            //注销监听器
            sensorManager.unregisterListener(listener);
            listener = null;
        }
    }
}