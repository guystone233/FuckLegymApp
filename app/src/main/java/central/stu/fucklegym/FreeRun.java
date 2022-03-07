package central.stu.fucklegym;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import fucklegym.top.entropy.User;
class InitUserThread extends Thread{
    private User user;
    private Activity activity;
    private Handler handler;

    public InitUserThread(User user, Activity activity,Handler handler){
        this.user = user;
        this.handler = handler;
        this.activity = activity;
    }
    public void run(){
        try {
            user.login();
            Message msg = handler.obtainMessage();
            msg.what = FreeRun.WHAT_UPDATE_RESTMILEAGE;
            msg.obj = Double.valueOf(-user.getTotalDailyMileage()+user.getDaliyMileage()).toString();
            handler.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(FreeRun.UPLOAD_FAIL);
        }
    }
}
class UploadThread extends Thread{
    private User user;
    private double tot,effective;
    private Activity activity;
    private Handler handler;
    private int campus;
    public UploadThread(User user,double tot,double effective,Activity activity,Handler handler,int campus){
        this.user = user;
        this.tot = tot;
        this.effective = effective;
        this.activity = activity;
        this.handler = handler;
        this.campus = campus;
    }
    public void run(){
        Random random = new Random(System.currentTimeMillis());
        Date endTime = new Date();
        Date startTime = new Date(endTime.getTime()-(10+ random.nextInt(10))*60*1000-random.nextInt(60)*1000);
        try {
            user.uploadRunningDetail(startTime,endTime,tot,effective,campus);
        } catch (IOException e) {
            e.printStackTrace();
            handler.sendEmptyMessage(FreeRun.UPLOAD_FAIL);
            return;
        }
        handler.sendEmptyMessage(FreeRun.UPLOAD_SUCCESS);
    }
}
public class FreeRun extends AppCompatActivity implements View.OnClickListener {
    private User user;
    public static final int WHAT_UPDATE_RESTMILEAGE = 0;
    public static final int UPLOAD_FAIL = 1;
    public static final int UPLOAD_SUCCESS = 2;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_run);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        user = new User(bundle.getString("username"),bundle.getString("password"));
        TextView restMileage = (TextView)findViewById(R.id.textView_restMielage);
        handler = new Handler(){
            public void handleMessage(Message msg) {
                // 处理消息
                super.handleMessage(msg);
                switch (msg.what) {
                    case WHAT_UPDATE_RESTMILEAGE:
                        restMileage.setText((String)msg.obj);
                        break;
                    case UPLOAD_FAIL:
                        Toast.makeText(FreeRun.this,"上传失败", Toast.LENGTH_LONG).show();
                        break;
                    case UPLOAD_SUCCESS:
                        Toast.makeText(FreeRun.this,"上传成功",Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        InitUserThread thread = new InitUserThread(user,this,handler);
        thread.start();
        Button upload = (Button)findViewById(R.id.button_upload);
        upload.setOnClickListener(this);
        findViewById(R.id.button_force).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(this);
        normalDialog.setTitle("选择校区");
        normalDialog.setMessage("你要选择哪一个校区呢?");
        normalDialog.setPositiveButton("沙河",
                (dialog, which) -> {
                    if(view.getId()==R.id.button_upload){
                        upload(0);
                    }else if(view.getId()==R.id.button_force){
                        forceUpload(0);
                    }
                });
        normalDialog.setNegativeButton("清水河",
                (dialog, which) -> {
                    if(view.getId()==R.id.button_upload){
                        upload(1);
                    }else if(view.getId()==R.id.button_force){
                        forceUpload(1);
                    }
                });
        // 显示
        normalDialog.show();


    }
    private void forceUpload(int campus){
        EditText text = (EditText)findViewById(R.id.editText_mileage);
        TextView view = (TextView)findViewById(R.id.textView_restMielage);
        UploadThread thread = new UploadThread(user,Double.parseDouble(text.getText().toString()),Double.parseDouble(text.getText().toString()),this,handler,campus);
        thread.start();

    }
    private void upload(int campus){
        EditText text = (EditText)findViewById(R.id.editText_mileage);
        TextView view = (TextView)findViewById(R.id.textView_restMielage);
        double value = Double.parseDouble(text.getText().toString());
        double mx = 3.5;
        if(!"初始化中......".equals(view.getText().toString()))mx = Double.parseDouble(view.getText().toString());
        if(value<0||value>mx){
            Toast.makeText(FreeRun.this,"上传失败，请检查数据是否安全", Toast.LENGTH_LONG).show();
        }else{
            UploadThread thread = new UploadThread(user,Double.parseDouble(text.getText().toString()),Double.parseDouble(text.getText().toString()),this,handler,campus);
            thread.start();
        }
    }
}
