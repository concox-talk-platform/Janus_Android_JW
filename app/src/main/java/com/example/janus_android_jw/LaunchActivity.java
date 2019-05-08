package com.example.janus_android_jw;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.janus_android_jw.bean.GroupBean;
import com.example.janus_android_jw.bean.UserBean;
import com.example.janus_android_jw.tool.AppTools;
import com.example.janus_android_jw.tool.GrpcConnectionManager;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudModel;

public class LaunchActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        textToSpeech = new TextToSpeech(LaunchActivity.this, this);

        textView = (TextView)findViewById(R.id.text);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        if (AppTools.isNetworkConnected(LaunchActivity.this)) {
            GrpcConnectionManager.initGrpcConnectionManager();
            //todo
            autoLogin();
        } else {
            myToast(R.string.network_unavailable,"");
        }

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //默认设定语言为中文，原生的android貌似不支持中文。
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(LaunchActivity.this, "no support", Toast.LENGTH_SHORT).show();
            } else {
                //不支持中文就将语言设置为英文
                textToSpeech.setLanguage(Locale.US);
            }
        }
    }

    private void checkForUpdate() {

    }

    private void autoLogin() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String name = tm.getDeviceId();
        Log.e("--imei-",name);
        String password = "123456";
        TalkCloudApp.LoginReq loginReq = TalkCloudApp.LoginReq.newBuilder().setName(name).setPasswd(password).build();
        TalkCloudApp.LoginRsp loginRsp = null;
        Future<TalkCloudApp.LoginRsp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.LoginRsp>() {
            @Override
            public TalkCloudApp.LoginRsp call() {
                return GrpcConnectionManager.getInstance().getBlockingStub().login(loginReq);
            }
        });

        try {
            loginRsp = future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(loginRsp == null){
            myToast(R.string.request_data_null,"");
            return;
        }

        if(loginRsp.getRes().getCode() != 200){
            myToast(R.string.app_name,loginRsp.getRes().getMsg());
            return;
        }

        myToast(R.string.login_success,"");

        UserBean userBean = new UserBean();
        userBean.setUserId(loginRsp.getUserInfo().getId());
        userBean.setUserName(loginRsp.getUserInfo().getUserName());
        userBean.setDefaultGroupId((loginRsp.getUserInfo().getLockGroupId() == 0)?207:loginRsp.getUserInfo().getLockGroupId());
        ArrayList<GroupBean> groupBeanArrayList = new ArrayList<>();
        for (TalkCloudApp.GroupInfo groupRecord: loginRsp.getGroupListList()) {
            GroupBean groupBean = new GroupBean();
            groupBean.setGroupName(groupRecord.getGroupName());
            groupBean.setGroupId(groupRecord.getGid());
            groupBeanArrayList.add(groupBean);
        }
        userBean.setGroupBeanArrayList(groupBeanArrayList);
        UserBean.setUserBean(userBean);

        Thread myThread1=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(1200);
                    Message message = new Message();
                    message.what = 0;
                    handler.sendMessage(message);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread1.start();

    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    startActivity(new Intent(LaunchActivity.this,MainActivity.class));
                    if(textToSpeech != null){
                        textToSpeech.stop();
                        textToSpeech.shutdown();
                        textToSpeech = null;
                    }
                    LaunchActivity.this.finish();
                    break;
            }
        }
    };


    private void myToast(int id,String msg){
        Thread myThread=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(200);
                    String text;
                    if("".equals(msg)){
                        text = getResources().getString(id);
                    }else{
                        text = msg;
                    }
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        myThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}
