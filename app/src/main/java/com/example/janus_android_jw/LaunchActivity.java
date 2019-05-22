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
import com.example.janus_android_jw.tool.DownloadUtils;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch);
        textToSpeech = new TextToSpeech(LaunchActivity.this, this);

        if (AppTools.isNetworkConnected(LaunchActivity.this)) {
            GrpcConnectionManager.initGrpcConnectionManager();
            //todo
            handleLoginTaskBack();
            //checkForUpdate();
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
        //http://113.105.153.240:82/group1/M00/00/16/cWmZ8FzUDHmAJBRBAP12mgr0KgU904.apk
        Log.e("janus---","checkForUpdate");
        DownloadUtils downloadUtils = new DownloadUtils(LaunchActivity.this);
        downloadUtils.downloadAPK("http://113.105.153.240:82/group1/M00/00/16/cWmZ8FzUDHmAJBRBAP12mgr0KgU904.apk","jimitalk.apk");
    }

    public void handleLoginTaskBack() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String name = tm.getDeviceId();
        Log.e("--imei-",name);
        String password = name.substring(9,15);
        Log.e("--imei-",password);
        TalkCloudApp.LoginReq loginReq = TalkCloudApp.LoginReq.newBuilder().setName(name).setPasswd(password).build();
        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    Log.d("MainActivity","MainActivity this is handleLoginTaskBack 1111111");
                    TalkCloudApp.LoginRsp loginRsp = GrpcConnectionManager.getInstance().getBlockingStub().login(loginReq);
                    Log.d("MainActivity","MainActivity this is handleLoginTaskBack 222 result="+loginRsp.getRes().getCode());
                    Message msg = Message.obtain();
                    msg.obj = loginRsp;
                    msg.what = 1;   //标志消息的标志
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            //TODO Nothing here
        }
    }

    private void autoLogin(TalkCloudApp.LoginRsp loginRsp) {
        if(loginRsp == null){
            myToast(R.string.request_data_null,"");
            Log.e("--janus--","---login---登录失败");
            return;
        }

        if(loginRsp.getRes().getCode() != 200){
            myToast(R.string.app_name,loginRsp.getRes().getMsg());
            Log.e("--janus--","---login---"+loginRsp.getRes().getMsg());
            return;
        }

        myToast(R.string.login_success,"");

        UserBean userBean = new UserBean();
        userBean.setUserId(loginRsp.getUserInfo().getId());
        userBean.setUserName(loginRsp.getUserInfo().getUserName());
        userBean.setNickName(loginRsp.getUserInfo().getNickName());
        Log.e("--janus--","getUserInfo---"+loginRsp.getUserInfo().getId());
        Log.e("--janus--","getUserName---"+loginRsp.getUserInfo().getUserName());
        Log.e("--janus--","getNickName---"+loginRsp.getUserInfo().getNickName());
        Log.e("--janus--","getLockGroupId---"+loginRsp.getUserInfo().getLockGroupId());

        ArrayList<GroupBean> groupBeanArrayList = new ArrayList<>();
        for (TalkCloudApp.GroupInfo groupRecord: loginRsp.getGroupListList()) {
            GroupBean groupBean = new GroupBean();
            groupBean.setGroupName(groupRecord.getGroupName());
            groupBean.setGroupId(groupRecord.getGid());
            groupBeanArrayList.add(groupBean);
        }
        int myid = 0;
        if(groupBeanArrayList.size()>0){
            myid = groupBeanArrayList.get(0).getGroupId();
        }
        userBean.setDefaultGroupId((loginRsp.getUserInfo().getLockGroupId() == 0)?myid:loginRsp.getUserInfo().getLockGroupId());
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
                case 1:
                    autoLogin((TalkCloudApp.LoginRsp)msg.obj);
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
