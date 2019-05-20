package com.example.janus_android_jw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.janus_android_jw.bean.MessageBean;
import com.example.janus_android_jw.bean.UserBean;
import com.example.janus_android_jw.gps.LocationService;
import com.example.janus_android_jw.receiver.BatteryReceiver;
import com.example.janus_android_jw.signalingcontrol.JanusControl;
import com.example.janus_android_jw.signalingcontrol.MyControlCallBack;
import com.example.janus_android_jw.tool.AppTools;
import com.example.janus_android_jw.tool.GrpcConnectionManager;
import com.example.janus_android_jw.tool.NetworkUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;
import webrtc.AppRTCAudioManager;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener,MyControlCallBack {

    private static int SEND_SOS_TYPE = 6;
    private static int CANCEL_SOS_TYPE = 7;
    private  MyBroadcastReceiver myBroadcastReceiver;
    private BatteryReceiver mBatteryReceiver;
    private TextToSpeech textToSpeech;
    private JanusControl janusControl;
    private int position = 0;
    private int playBackPosition = 0;
    private AppRTCAudioManager audioManager = null;
    private MediaPlayer mediaPlayer;
    private boolean isSenderSOS = false;

    private Thread instantMessageThread;

    private Button button1 = null;
    private Button button2 = null;
    private Button button3 = null;
    private Button button4 = null;
    private Button button5 = null;

    private boolean network = true;

    ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取position
        if(UserBean.getUserBean().getGroupBeanArrayList() !=null){
            for(int i=0;i<UserBean.getUserBean().getGroupBeanArrayList().size();i++){
                if(UserBean.getUserBean().getDefaultGroupId() == UserBean.getUserBean().getGroupBeanArrayList().get(i).getGroupId()){
                    position = i;
                }
            }
        }

        audioManager = AppRTCAudioManager.create(MainActivity.this, new Runnable() {
                    @Override
                    public void run() {

                    }
                }
        );
        audioManager.init();

        //开启即时消息线程
        getInstantMessage();

        textToSpeech = new TextToSpeech(this, this);
        janusControl = new JanusControl(this,UserBean.getUserBean().getUserName(),UserBean.getUserBean().getUserId(),UserBean.getUserBean().getDefaultGroupId());
        janusControl.Start(false);

        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast( new Intent("android.intent.action.ext_p1.longpress"));
                myMediaPlayer("",R.raw.start);
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast( new Intent("android.intent.action.ext_p2.down"));
            }
        });

        button3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        sendBroadcast( new Intent("android.intent.action.ext_ptt.down"));
                        break;
                    case MotionEvent.ACTION_MOVE:

                        break;
                    case MotionEvent.ACTION_UP:
                        sendBroadcast( new Intent("android.intent.action.ext_ptt.up"));
                        break;
                }
                return false;
            }
        });

        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast( new Intent("android.intent.action.ext_p3.down"));
            }
        });

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendBroadcast( new Intent("android.intent.action.ext_fun.down"));
                Message message3 = Message.obtain();
                message3.what = 3;
                handler.sendMessage(message3);
            }
        });
        //Intent intent = new Intent(this, LocationService.class);
        //startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(myBroadcastReceiver == null){
            myBroadcastReceiver = new MyBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.ext_ptt.down");
            intentFilter.addAction("android.intent.action.ext_ptt.longpress");
            intentFilter.addAction("android.intent.action.ext_ptt.up");

            intentFilter.addAction("android.intent.action.ext_p1.down");
            intentFilter.addAction("android.intent.action.ext_p1.longpress");
            intentFilter.addAction("android.intent.action.ext_p1.up");

            intentFilter.addAction("android.intent.action.ext_p2.down");
            intentFilter.addAction("android.intent.action.ext_p2.longpress");
            intentFilter.addAction("android.intent.action.ext_p2.up");

            intentFilter.addAction("android.intent.action.ext_p3.down");
            intentFilter.addAction("android.intent.action.ext_p3.longpress");
            intentFilter.addAction("android.intent.action.ext_p3.up");

            intentFilter.addAction("android.intent.action.ext_fun.down");
            intentFilter.addAction("android.intent.action.ext_fun.longpress");
            intentFilter.addAction("android.intent.action.ext_fun.up");

            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            registerReceiver(myBroadcastReceiver, intentFilter);

            mBatteryReceiver = new BatteryReceiver(handler);
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mBatteryReceiver, filter2);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void janusServer(int code, String msg) {
        switch (code){
            case 100:
                Log.e("-janus------","-----100的错误-----"+msg);
                if(msg.indexOf("Exception: null") == -1) {
                    Message message100 = Message.obtain();
                    message100.what = 100;
                    handler.sendMessage(message100);
                }
                break;
            case 101:
                //webSocket 连接
                JanusControl.sendAttachPocRoomPlugin(this,false);
                break;
            case 102:
                //webSocket 重新连接
                Log.e("-janus------","-----重新连接成功，发送Claim-----");
                JanusControl.sendClaim(MainActivity.this);
                break;
        }
    }

    @Override
    public void showMessage(JSONObject msg, JSONObject jsepLocal) {
        try {
            if(msg.has("error") && msg.getJSONObject("error").has("code") && msg.getJSONObject("error").getInt("code") == 458){
                //No such session
                //关闭心跳，重新创建session
                JanusControl.closeWebRtc();
                JanusControl.stopKeepAliveThread();
                JanusControl.createSessionId();
            }else if(msg.has("claim")){
                // session 未超时 离开房间
                JanusControl.sendLeave(MainActivity.this);
            }else{
                if (msg.getString("pocroom").equals("audiobridgeisok")) {
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    JanusControl.janusControlCreatePeerConnectionFactory(MainActivity.this);
                    JanusControl.sendPocRoomJoinRoom(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                }else if(msg.getString("pocroom").equals("joined")){
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    if(msg.has("id") && msg.getInt("id") == UserBean.getUserBean().getUserId() ){
                        JanusControl.sendPocRoomCreateOffer(MainActivity.this);
                    }
                }else if(msg.getString("pocroom").equals("event")){
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    if(msg.has("error_code") && msg.getInt("error_code") ==490){
//                        JanusControl.sendLeave(MainActivity.this);
                        //JanusControl.sendClaim(MainActivity.this);
                    }else if(msg.has("error_code") && msg.getInt("error_code") ==487 ){
                        //"error":"Can't change room (not in a room in the first place)","error_code":487,"
//                        if(position == 0){
//                            position = UserBean.getUserBean().getGroupBeanArrayList().size()-1;
//                        }else{
//                            position=position-1;
//                        }
                        //JanusControl.sendPocRoomJoinRoom(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                    }else{

                    }
                    if (msg.has("talkfreed")){
                        if(msg.getInt("talkfreed") != UserBean.getUserBean().getUserId()){
                            Message message207 = Message.obtain();
                            message207.what = 207;
                            handler.sendMessage(message207);
                        }
                    }
                }else if(msg.getString("pocroom").equals("webRtcisok")){
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    Message message202 = Message.obtain();
                    message202.what = 202;
                    handler.sendMessage(message202);
                }else if(msg.getString("pocroom").equals("webRtcfailed")){
                    //webRtc failed todo webrtc 因为网络中断 ，该怎么操作 心跳重连
                    if(network){
                        Thread thread201 = new Thread(){
                            @Override
                            public void run() {
                                super.run();
                                try {
                                Thread.currentThread().sleep(5000);
                                Log.e("---janus---","---re connection webrtc---");
                                JanusControl.closeWebRtc();
                                JanusControl.sendPocRoomCreateOffer(MainActivity.this);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        thread201.run();
                    }
                }else if (msg.getString("pocroom").equals("roomchanged")) {
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    Message message201 = Message.obtain();
                    message201.what = 201;
                    handler.sendMessage(message201);
                }else if(msg.getString("pocroom").equals("talked")){
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    if(msg.has("id")){
                        Message message204 = Message.obtain();
                        message204.what = 204;
                        handler.sendMessage(message204);
                    }else if(msg.has("talkholder")){
                        Message message205 = Message.obtain();
                        message205.what = 205;
                        handler.sendMessage(message205);
                    }
                }else if(msg.getString("pocroom").equals("untalked")){
                    Log.e("-pocroom------",msg.getString("pocroom"));
                    Message message206 = Message.obtain();
                    message206.what = 206;
                    handler.sendMessage(message206);
                }else if(msg.getString("pocroom").equals("configured")){

                }else if(msg.getString("pocroom").equals("left")){
                    if(msg.getInt("id") == UserBean.getUserBean().getUserId()){
                        //自己离开房间
                        JanusControl.closeWebRtc();
                        JanusControl.stopKeepAliveThread();
                        JanusControl.createSessionId();
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    private void close(){
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }

        JanusControl.closeWebRtc();
        JanusControl.closeJanusServer();
        GrpcConnectionManager.closeGrpcConnectionManager();
        UserBean.clearUserBean();
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        singleThreadExecutor.shutdown();

        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);

        unregisterReceiver(myBroadcastReceiver);
        unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS){
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Toast.makeText(MainActivity.this, "no support", Toast.LENGTH_SHORT).show();
            }else{
                textToSpeech.setLanguage(Locale.US);
            }
        }
    }

    @Override
    public void onSetLocalStream(MediaStream stream) {

    }

    @Override
    public void onAddRemoteStream(MediaStream stream) {

    }

    class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainActivity","MainActivity intent="+intent.getAction());
            if("android.intent.action.ext_ptt.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message0 = Message.obtain();
                message0.what = 0;
                handler.sendMessage(message0);
            }else if("android.intent.action.ext_ptt.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message1 = Message.obtain();
                message1.what = 1;
                handler.sendMessage(message1);
            }else if("android.intent.action.ext_ptt.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message2 = Message.obtain();
                message2.what = 2;
                handler.sendMessage(message2);
            }else if("android.intent.action.ext_p1.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message3 = Message.obtain();
                message3.what = 3;
                handler.sendMessage(message3);
            }else if("android.intent.action.ext_p1.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message4 = Message.obtain();
                message4.what = 4;
                handler.sendMessage(message4);
            }else if("android.intent.action.ext_p1.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message5 = Message.obtain();
                message5.what = 5;
                handler.sendMessage(message5);
            }else if("android.intent.action.ext_p2.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message6 = Message.obtain();
                message6.what = 6;
                handler.sendMessage(message6);
            }else if("android.intent.action.ext_p2.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message7 = Message.obtain();
                message7.what = 7;
                handler.sendMessage(message7);
            }else if("android.intent.action.ext_p2.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message8 = Message.obtain();
                message8.what = 8;
                handler.sendMessage(message8);
            }else if("android.intent.action.ext_p3.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message9 = Message.obtain();
                message9.what = 9;
                handler.sendMessage(message9);
            }else if("android.intent.action.ext_p3.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message10 = Message.obtain();
                message10.what = 10;
                handler.sendMessage(message10);
            }else if("android.intent.action.ext_p3.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message11 = Message.obtain();
                message11.what = 11;
                handler.sendMessage(message11);
            }else if("android.intent.action.ext_fun.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message12 = Message.obtain();
                message12.what = 12;
                handler.sendMessage(message12);
            }else if("android.intent.action.ext_fun.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message13 = Message.obtain();
                message13.what = 13;
                handler.sendMessage(message13);
            }else if("android.intent.action.ext_fun.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message14 = Message.obtain();
                message14.what = 14;
                handler.sendMessage(message14);
            }else if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())){
                Log.e("-wangluo------",intent.getAction()+"---"+NetworkUtil.getNetWorkStates(MainActivity.this));
                Message message15 = Message.obtain();
                message15.what = 15;
                handler.sendMessage(message15);
            }
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    //ptt down
                    JanusControl.sendTalk(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                    break;
                case 1:
                    //ptt lang

                    break;
                case 2:
                    //ptt up
                    //JanusControl.sendConfigure(MainActivity.this,true);
                    JanusControl.setRemoveAudioTrack();
                    JanusControl.sendUnTalk(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                    break;
                case 3:
                    if(isSenderSOS){
                        cancelSOS();
                        isSenderSOS = false;
                    }
                    handler.removeMessages(301);
                    break;
                case 4:
                    //sendSOS();
                    handleSOSTaskBack(SEND_SOS_TYPE);
                    isSenderSOS = true;
                    break;
                case 5:
                    //p1 up

                    break;
                case 6:
                    //p2 down
                    //change group 如果切换失败，position不能增加 todo
                    if(UserBean.getUserBean().getGroupBeanArrayList().size() > 1){
                        //切换群组，取消之前群组中的sos,
                        if(isSenderSOS){
                            cancelSOS();
                            isSenderSOS = false;
                        }
                        handler.removeMessages(301);
                        int nextPosition = position;
                        if(nextPosition == UserBean.getUserBean().getGroupBeanArrayList().size()-1){
                            nextPosition = 0;
                        }else{
                            nextPosition=nextPosition+1;
                        }
                        JanusControl.sendChangeGroup(MainActivity.this,UserBean.getUserBean().getGroupBeanArrayList().get(nextPosition).getGroupId() );
                    }else{
                        String text = UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupName();
                        myToast(R.string.app_name,text);
                    }
                    break;
                case 7:
                    //p2 lang

                    break;
                case 8:
                    //p2 up

                    break;
                case 9:
                    //p3 down
                    playBack();
                    break;
                case 10:
                    //p3 lang

                    break;
                case 11:
                    //p3 up

                    break;
                case 12:
                    String batteryMsg = "current battery is "+BatteryReceiver.mBatteryPower+"%";
                    myToast(0,batteryMsg);
                    break;
                case 13:
                    //p4 lang

                    break;
                case 14:
                    //p4 up

                    break;
                case 15:
                    //网络状态
                    int netWorkStates = NetworkUtil.getNetWorkStates(MainActivity.this);
                    switch (netWorkStates) {
                        case NetworkUtil.TYPE_NONE:
                            //断网了
                            network = false;
                            grpc_keep_alive = false;
                            if(thread101 !=null){
                                thread101.interrupt();
                            }
                            JanusControl.stopKeepAliveThread();
                            myToast(R.string.network_unavailable,null);
                            Log.e("-wangluo------","==="+NetworkUtil.getNetWorkStates(MainActivity.this));
                            break;
                        case NetworkUtil.TYPE_MOBILE:
                            //打开了移动网络和wifi
                            if(!network){
                                Log.e("-wangluo------","==="+network+"---开始start,getInstantMessage");
                                network = true;
                                janusControl.Start(true);
                                reConnectGrpc();
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case 100:
                    //心跳 重连
                    myToast(R.string.websocket_connection_fail,null);
                    if(network) {
                        Thread thread202 = new Thread(){
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    Thread.currentThread().sleep(5000);
                                    Log.e("---janus---","---re connection websocket---");
                                    janusControl.Start(true);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        thread202.start();
                    }
                    break;
                case 201:
                    //切换群组
                    if(position == UserBean.getUserBean().getGroupBeanArrayList().size()-1){
                        position = 0;
                    }else{
                        position=position+1;
                    }
                    String tips1 = getResources().getString(R.string.have_to_switch)+UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupName();
                    myToast(R.string.app_name,tips1);
                    UserBean.getUserBean().setDefaultGroupId(UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupId());
                    playBackPosition = 0;
                    sendDefaultGroupId();
                    break;
                case 202:
                    //进入群组成功
                    myToast(R.string.app_name,"entry into "+UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupName());
                    JanusControl.setRemoveAudioTrack();
                    break;
                case 203:
                    //进入群组失败
                    //myToast(R.string.entry_room_failure,null);
                    break;
                case 204:
                    //获取讲话权限
                    //JanusControl.sendConfigure(MainActivity.this,false);
                    sendBroadcast( new Intent("com.dfl.redled.on"));
                    myMediaPlayer("",R.raw.start);
                    JanusControl.setAddAudioTrack();
                    break;
                case 205:
                    //未获取到讲话权限-有人在讲话-指示灯为绿色开启
                    sendBroadcast( new Intent("com.dfl.greenled.on"));
                    break;
                case 206:
                    //放麦
                    sendBroadcast( new Intent("com.dfl.redled.off"));
                    break;
                case 207:
                    //讲话结束-指示灯为绿色开启
                    sendBroadcast( new Intent("com.dfl.greenled.off"));
                    break;
                case 208:
                   //pocRoom already exists
                    //JanusControl.sendLeave(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                    //JanusControl.sendPocRoomJoinRoom(MainActivity.this,UserBean.getUserBean().getDefaultGroupId());
                    break;
                case 301:
                    Log.d("MainActivity","MainActivity this is 301 receiverSOS");
                    String sosName = (String)msg.obj;
                    Log.d("MainActivity","MainActivity this is 301 sosName="+sosName);
                    //收到当前群组中的SOS信息
                    receiverSOS(sosName);
                    break;
                case 302:
                    String lowBattery = "current battery is less than 20%";
                    myToast(0,lowBattery);
                    break;
                case 401:
                    sendSOS((TalkCloudApp.ImMsgRespData)msg.obj);
                    break;
            }
        };
    };

    private void myToast(int id,String msg){
        Log.e("MainActivity","MainActivity myToast msg="+msg);
        String text;
        if(msg == null){
            text = getResources().getString(id);
        }else{
            text = msg;
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    private void sendDefaultGroupId(){
        TalkCloudApp.SetLockGroupIdReq lockGroupIdReq = TalkCloudApp.SetLockGroupIdReq.newBuilder().setUId(UserBean.getUserBean().getUserId()).setGId(UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupId()).build();
        TalkCloudApp.SetLockGroupIdResp lockGroupIdResp = null;
        Future<TalkCloudApp.SetLockGroupIdResp> future = GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Callable<TalkCloudApp.SetLockGroupIdResp>() {
            @Override
            public TalkCloudApp.SetLockGroupIdResp call() {
                return GrpcConnectionManager.getInstance().getBlockingStub().setLockGroupId(lockGroupIdReq);
            }
        });

        try {
            Log.d("MainActivity","MainActivity this is sendDefaultGroupId and start");
            lockGroupIdResp = future.get();
            Log.d("MainActivity","MainActivity this is sendDefaultGroupId and end");
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(lockGroupIdResp == null){
            myToast(R.string.request_data_null,null);
            return;
        }

        if(lockGroupIdResp.getRes().getCode() != 200){
            myToast(R.string.app_name,lockGroupIdResp.getRes().getMsg());
            return;
        }
    }

    // 物理返回键退出程序
    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 3000) {
                exitTime =  System.currentTimeMillis();
            }else{
                close();
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    //获取IM消息
    private boolean isSendKeepAlive = false;
    private boolean grpc_keep_alive = true;
    Thread thread101 = null;
    public void getInstantMessage(){
       Thread thread102 = new Thread(){
           @Override
           public void run() {
               super.run();
               ManagedChannel channel = GrpcConnectionManager.getInstance().getManagedChannel();
               TalkCloudGrpc.TalkCloudStub stub = TalkCloudGrpc.newStub(channel);
               StreamObserver<TalkCloudApp.StreamResponse> response = new StreamObserver<TalkCloudApp.StreamResponse>() {
                   @Override
                   public void onNext(TalkCloudApp.StreamResponse value) {
                       //接收服务器下发的消息
                       int type = value.getDataType();
                       Log.e("---janus---","----即时消息-Type---"+type);
                       if (type == 3) {//在线消息
                           if(value.getImMsgData().getReceiverType() == 2){
                               if(UserBean.getUserBean().getDefaultGroupId() == value.getImMsgData().getReceiverId()){
                                   playBackPosition = 0;
                                   if(value.getImMsgData().getMsgType()==SEND_SOS_TYPE){
                                       Log.d("MainActivity","MainActivity StreamObserver SOS 。。。");
                                       Message message301 = Message.obtain();
                                       message301.what = 301;
                                       message301.obj = value.getImMsgData().getSenderName();
                                       handler.sendMessage(message301);
                                   }else if(value.getImMsgData().getMsgType()==CANCEL_SOS_TYPE){
                                       Log.d("MainActivity","MainActivity StreamObserver cancelSOS 。。。");
                                       Message message3 = Message.obtain();
                                       message3.what = 3;
                                       handler.sendMessage(message3);
                                   }
                               }
                               if(value.getImMsgData().getMsgType() == 3){
                                   for(int i=0;i<UserBean.getUserBean().getGroupBeanArrayList().size();i++){
                                       if(UserBean.getUserBean().getGroupBeanArrayList().get(i).getGroupId() == value.getImMsgData().getReceiverId()){
                                           MessageBean messageBean = new MessageBean();
                                           messageBean.setType(value.getImMsgData().getMsgType());
                                           messageBean.setMessage(value.getImMsgData().getResourcePath());
                                           if(UserBean.getUserBean().getGroupBeanArrayList().get(i).getMessageBeanArrayList() == null){
                                               ArrayList<MessageBean> messageBeanArrayList = new ArrayList<>();
                                               UserBean.getUserBean().getGroupBeanArrayList().get(i).setMessageBeanArrayList(messageBeanArrayList);
                                           }
                                           UserBean.getUserBean().getGroupBeanArrayList().get(i).getMessageBeanArrayList().add(0,messageBean);
                                           if(UserBean.getUserBean().getGroupBeanArrayList().get(i).getMessageBeanArrayList().size()>=11){
                                               UserBean.getUserBean().getGroupBeanArrayList().get(i).getMessageBeanArrayList().remove(10);
                                           }
                                       }
                                   }
                               }
                           }

                       } else if (type == 2) {//离线消息
                           for (TalkCloudApp.OfflineImMsg offlineImMsg: value.getOfflineImMsgResp().getOfflineGroupImMsgsList()) {
                               if(offlineImMsg.getMsgReceiverType() == 2){
                                   for(int i=0;i<UserBean.getUserBean().getGroupBeanArrayList().size();i++){
                                       if(UserBean.getUserBean().getGroupBeanArrayList().get(i).getGroupId() == offlineImMsg.getGroupId()){
                                           ArrayList<MessageBean> messageBeanArrayList = new ArrayList<>();
                                           int msgSize = 0;
                                           if(offlineImMsg.getImMsgDataList().size()>10){
                                               msgSize = 10;
                                           }else{
                                               msgSize = offlineImMsg.getImMsgDataList().size();
                                           }
                                           for(int j=0;j<msgSize;j++){
                                               MessageBean messageBean = new MessageBean();
                                               messageBean.setType(offlineImMsg.getImMsgDataList().get(j).getMsgType());
                                               messageBean.setMessage(offlineImMsg.getImMsgDataList().get(j).getResourcePath());
                                               messageBeanArrayList.add(messageBean);
                                           }
                                           UserBean.getUserBean().getGroupBeanArrayList().get(i).setMessageBeanArrayList(messageBeanArrayList);
                                       }
                                   }
                               }
                           }
                       }
                   }

                   @Override
                   public void onError(Throwable t) {
                       Log.e("--janus---","---StreamObserver onError---"+t.getMessage());
                       if(network){
                           reConnectGrpc();
                       }

                   }

                   @Override
                   public void onCompleted() {
                       Log.e("--janus---","---StreamObserver onCompleted---");
                       if(network){
                           reConnectGrpc();
                       }

                   }
               };
               grpc_keep_alive = true;
               StreamObserver<TalkCloudApp.StreamRequest> request = stub.dataPublish(response);
               thread101 = new Thread(){
                   @Override
                   public void run() {
                       super.run();
                       while (grpc_keep_alive){
                           try {
                               TalkCloudApp.StreamRequest value = null;
                               if(isSendKeepAlive){
                                   Log.e("---janus---","---心跳---");
                                   try{
                                       Thread.currentThread().sleep(18*1000);
                                       value = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(4).build();
                                   } catch (InterruptedException e) {
                                       grpc_keep_alive = false;
                                   }
                               }else{
                                   //发送
                                   Log.e("---janus---","---离线消息---");
                                   value = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(2).build();
                                   isSendKeepAlive = true;
                               }
                               request.onNext(value);
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                       }
                   }
               };
               thread101.start();
           }
       };
       thread102.start();

    }

    private void playBack(){
        if(UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList()!= null){
            if(UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().get(playBackPosition).getType() == 3){
                Log.e("-janus--",UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().get(playBackPosition).getMessage());
                Log.e("-janus--",UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().size()+"---"+playBackPosition);
                myMediaPlayer(UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().get(playBackPosition).getMessage(),0);
                playBackPosition = playBackPosition + 1;
                if(playBackPosition >= 10 || playBackPosition == UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().size()){
                    playBackPosition = 0;
                }
            }
        }
    }

    private void myMediaPlayer(String path,int rawId){
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }else  if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
        try {
            mediaPlayer = new MediaPlayer();
            if(path.equals("")){
                mediaPlayer.setDataSource(MainActivity.this,Uri.parse("android.resource://" + getPackageName() + "/" + rawId));
            }else{
                mediaPlayer.setDataSource(path);
            }
            //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer paramMediaPlayer) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            });
        }catch (Exception e){

        }
    }

    private void receiverSOS(String name){
         myToast(0,name+" sos");
         handler.removeMessages(301);
         Message message = Message.obtain();
         message.what = 301;
         message.obj = name;
         handler.sendMessageDelayed(message,5*1000);
    }

    private void cancelSOS(){
        handleSOSTaskBack(CANCEL_SOS_TYPE);
    }

    private void reConnectGrpc() {
        grpc_keep_alive = false;
        if (thread101 != null) {
            thread101.interrupt();
        }
        if (network) {
            Thread thread203 = new Thread(){
                @Override
                public void run() {
                    super.run();
                    try {
                        Log.e("---janus---", "---re connection grpc 开始睡眠---");
                        Thread.currentThread().sleep(18 * 1000);
                        Log.e("---janus---", "---re connection grpc 开始重连---");
                        getInstantMessage();
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            };
            thread203.start();
        }
    }

    public void handleSOSTaskBack(int msgType) {
        TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder().setId(UserBean.getUserBean().getUserId()).setSenderName(UserBean.getUserBean().getNickName()).
                setReceiverId(UserBean.getUserBean().getDefaultGroupId()).setReceiverType(2).setMsgType(msgType).setMsgCode("").setResourcePath("SOS").setSendTime("").build();

        try {
            GrpcConnectionManager.getInstance().getGrpcInstantRequestHandler().submit(new Runnable() {
                @Override
                public void run() {
                    TalkCloudApp.ImMsgRespData lockGroupIdResp =  GrpcConnectionManager.getInstance().getBlockingStub().imMessagePublish(data);
                    Message msg = Message.obtain();
                    msg.obj = lockGroupIdResp;
                    msg.what = 401;   //标志消息的标志
                    handler.sendMessage(msg);
                }
            });
        } catch (Exception e) {
            //TODO Nothing here
        }
    }

    private void sendSOS(TalkCloudApp.ImMsgRespData lockGroupIdResp){
        Log.d("MainActivity","MainActivity this is lockGroupIdResp="+lockGroupIdResp);
        if(lockGroupIdResp == null){
            myToast(R.string.request_data_null,null);
            return;
        }
        Log.d("MainActivity","MainActivity this is code="+lockGroupIdResp.getResult().getCode());
        if(lockGroupIdResp.getResult().getCode()!= 200){
            myToast(R.string.app_name,lockGroupIdResp.getResult().getMsg());
            return;
        }
    }
}
