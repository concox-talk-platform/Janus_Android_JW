package com.example.janus_android_jw;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.janus_android_jw.bean.MessageBean;
import com.example.janus_android_jw.bean.UserBean;
import com.example.janus_android_jw.signalingcontrol.JanusControl;
import com.example.janus_android_jw.signalingcontrol.MyControlCallBack;
import com.example.janus_android_jw.tool.GrpcConnectionManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import talk_cloud.TalkCloudApp;
import talk_cloud.TalkCloudGrpc;
import webrtc.AppRTCAudioManager;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener,MyControlCallBack {

    private  MyBroadcastReceiver myBroadcastReceiver;
    private TextToSpeech textToSpeech;
    private JanusControl janusControl;
    private int position = 0;
    private int playBackPosition = 0;
    private AppRTCAudioManager audioManager = null;
    private MediaPlayer mediaPlayer;

    private Thread instantMessageThread;

    private Button button1 = null;
    private Button button2 = null;
    private Button button3 = null;
    private Button button4 = null;
    private Button button5 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取position
        for(int i=0;i<UserBean.getUserBean().getGroupBeanArrayList().size();i++){
            if(UserBean.getUserBean().getDefaultGroupId() == UserBean.getUserBean().getGroupBeanArrayList().get(i).getGroupId()){
                position = i;
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
        janusControl.Start();

        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button3 = (Button) findViewById(R.id.button3);
        button4 = (Button) findViewById(R.id.button4);
        button5 = (Button) findViewById(R.id.button5);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast( new Intent("android.intent.action.ext_p1.down"));
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
                sendBroadcast( new Intent("android.intent.action.ext_fun.down"));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        myBroadcastReceiver = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ext_ptt.down");
        intentFilter.addAction("android.intent.action.ext_ptt.longpress");
        intentFilter.addAction("android.intent.action.ext_ptt.up");

        intentFilter.addAction("android.intent.action.ext_p1.down");
        intentFilter.addAction("android.intent.action.ext_p1.longpress");
        intentFilter.addAction("android.intent.action.ext_p1.up");

        intentFilter.addAction("android.intent.action.ext_p2.down");
        intentFilter.addAction("android.intent.action.ext_p2.longpres");
        intentFilter.addAction("android.intent.action.ext_p2.up");

        intentFilter.addAction("android.intent.action.ext_p3.down");
        intentFilter.addAction("android.intent.action.ext_p3.longpress");
        intentFilter.addAction("android.intent.action.ext_p3.up");

        intentFilter.addAction("android.intent.action.ext_fun.down");
        intentFilter.addAction("android.intent.action.ext_fun.longpress");
        intentFilter.addAction("android.intent.action.ext_fun.up");
        registerReceiver(myBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(myBroadcastReceiver);
    }

    @Override
    public void janusServer(int code, String msg) {
        switch (code){
            case 100:
                Message message100 = new Message();
                message100.what = 100;
                handler.sendMessage(message100);
                break;
            case 101:
                JanusControl.sendAttachPocRoomPlugin(this,false);
                break;
        }
    }

    @Override
    public void showMessage(JSONObject msg, JSONObject jsepLocal) {
        try {
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
                    Message message208 = new Message();
                    message208.what = 208;
                    handler.sendMessage(message208);
                }else if(msg.has("error_code")){
                    Message message203 = new Message();
                    message203.what = 203;
                    handler.sendMessage(message203);
                }

                if (msg.has("talkfreed")){
                    if(msg.getInt("talkfreed") != UserBean.getUserBean().getUserId()){
                        Message message207 = new Message();
                        message207.what = 207;
                        handler.sendMessage(message207);
                    }
                }
            }else if(msg.getString("pocroom").equals("webRtcisok")){
                Log.e("-pocroom------",msg.getString("pocroom"));
                Message message202 = new Message();
                message202.what = 202;
                handler.sendMessage(message202);
            }else if (msg.getString("pocroom").equals("roomchanged")) {
                Log.e("-pocroom------",msg.getString("pocroom"));
                Message message201 = new Message();
                message201.what = 201;
                handler.sendMessage(message201);
            }else if(msg.getString("pocroom").equals("talked")){
                Log.e("-pocroom------",msg.getString("pocroom"));
                if(msg.has("id")){
                    Message message204 = new Message();
                    message204.what = 204;
                    handler.sendMessage(message204);
                }else if(msg.has("talkholder")){
                    Message message205 = new Message();
                    message205.what = 205;
                    handler.sendMessage(message205);
                }
            }else if(msg.getString("pocroom").equals("untalked")){
                Log.e("-pocroom------",msg.getString("pocroom"));
                Message message206 = new Message();
                message206.what = 206;
                handler.sendMessage(message206);
            }else if(msg.getString("pocroom").equals("configured")){

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

        JanusControl.closeJanusServer();
        GrpcConnectionManager.closeGrpcConnectionManager();
        UserBean.clearUserBean();
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
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
            if("android.intent.action.ext_ptt.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message0 = new Message();
                message0.what = 0;
                handler.sendMessage(message0);
            }else if("android.intent.action.ext_ptt.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message1 = new Message();
                message1.what = 1;
                handler.sendMessage(message1);
            }else if("android.intent.action.ext_ptt.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message2 = new Message();
                message2.what = 2;
                handler.sendMessage(message2);
            }else if("android.intent.action.ext_p1.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message3 = new Message();
                message3.what = 3;
                handler.sendMessage(message3);
            }else if("android.intent.action.ext_p1.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message4 = new Message();
                message4.what = 4;
                handler.sendMessage(message4);
            }else if("android.intent.action.ext_p1.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message5 = new Message();
                message5.what = 5;
                handler.sendMessage(message5);
            }else if("android.intent.action.ext_p2.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message6 = new Message();
                message6.what = 6;
                handler.sendMessage(message6);
            }else if("android.intent.action.ext_p2.longpres".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message7 = new Message();
                message7.what = 7;
                handler.sendMessage(message7);
            }else if("android.intent.action.ext_p2.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message8 = new Message();
                message8.what = 8;
                handler.sendMessage(message8);
            }else if("android.intent.action.ext_p3.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message9 = new Message();
                message9.what = 9;
                handler.sendMessage(message9);
            }else if("android.intent.action.ext_p3.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message10 = new Message();
                message10.what = 10;
                handler.sendMessage(message10);
            }else if("android.intent.action.ext_p3.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message11 = new Message();
                message11.what = 11;
                handler.sendMessage(message11);
            }else if("android.intent.action.ext_fun.down".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message12 = new Message();
                message12.what = 12;
                handler.sendMessage(message12);
            }else if("android.intent.action.ext_fun.longpress".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message13 = new Message();
                message13.what = 13;
                handler.sendMessage(message13);
            }else if("android.intent.action.ext_fun.up".equals(intent.getAction())){
                Log.e("-anniu------",intent.getAction());
                Message message14 = new Message();
                message14.what = 14;
                handler.sendMessage(message14);
            }
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 0:
                    //ptt down
                    JanusControl.sendTalk(MainActivity.this);
                    break;
                case 1:
                    //ptt lang

                    break;
                case 2:
                    //ptt up
                    JanusControl.sendConfigure(MainActivity.this,true);
                    JanusControl.sendUnTalk(MainActivity.this);
                    break;
                case 3:
                    //p1 down

                    break;
                case 4:
                    //p1 lang

                    break;
                case 5:
                    //p1 up

                    break;
                case 6:
                    //p2 down
                    //change group
                    if(UserBean.getUserBean().getGroupBeanArrayList().size() > 1){
                        if(position == UserBean.getUserBean().getGroupBeanArrayList().size()-1){
                            position = 0;
                        }else{
                            position=position+1;
                        }
                        JanusControl.sendChangeGroup(MainActivity.this,UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupId() );
                        UserBean.getUserBean().setDefaultGroupId(UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupId());
                        playBackPosition = 0;
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
                    //p4 down

                    break;
                case 13:
                    //p4 lang

                    break;
                case 14:
                    //p4 up

                    break;
                case 100:
                    myToast(R.string.websocket_connection_fail,null);
                    JanusControl.closeJanusServer();
                    Thread myThread11=new Thread(){
                        @Override
                        public void run() {
                            try{
                                sleep(5000);
                                janusControl.Start();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    };
                    myThread11.start();
                    break;
                case 201:
                    //切换群组
                    String tips1 = getResources().getString(R.string.have_to_switch)+UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupName();
                    myToast(R.string.app_name,tips1);
                    sendDefaultGroupId();
                    break;
                case 202:
                    //进入群组成功
                    myToast(R.string.app_name,"entry into "+UserBean.getUserBean().getGroupBeanArrayList().get(position).getGroupName());
                    break;
                case 203:
                    //进入群组失败
                    //myToast(R.string.entry_room_failure,null);
                    break;
                case 204:
                    //获取讲话权限
                    JanusControl.sendConfigure(MainActivity.this,false);
                    sendBroadcast( new Intent("com.dfl.redled.on"));
                    myMediaPlayer("",R.raw.start);
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
                    //收到当前群组中的SOS信息

                    break;
            }
        };
    };

    private void myToast(int id,String msg){
        Thread myThread=new Thread(){
            @Override
            public void run() {
                try{
                    sleep(300);
                    String text;
                    if(msg == null){
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
            lockGroupIdResp = future.get();
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
    public void getInstantMessage(){
        instantMessageThread = new Thread(){
            @Override
            public void run() {
                try{
                    StreamObserver<TalkCloudApp.StreamResponse> response = new StreamObserver<TalkCloudApp.StreamResponse>() {
                        @Override
                        public void onNext(TalkCloudApp.StreamResponse value) {
                            //接收服务器下发的消息
                            int type = value.getDataType();
                            if (type == 3) {//在线消息
                                if(value.getImMsgData().getReceiverType() == 2){
                                    if(UserBean.getUserBean().getDefaultGroupId() == value.getImMsgData().getReceiverId()){
                                        playBackPosition = 0;
                                        if(value.getImMsgData().getResourcePath().indexOf("SOS") != -1){
                                            Message message301 = new Message();
                                            message301.what = 301;
                                            handler.sendMessage(message301);
                                        }
                                    }
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
                            Log.e("--userbean--",UserBean.getUserBean().toString());
                        }

                        @Override
                        public void onError(Throwable t) {

                        }

                        @Override
                        public void onCompleted() {

                        }
                    };
                    ManagedChannel channel = GrpcConnectionManager.getInstance().getManagedChannel();
                    TalkCloudGrpc.TalkCloudStub stub = TalkCloudGrpc.newStub(channel);
                    StreamObserver<TalkCloudApp.StreamRequest> request = stub.dataPublish(response);
                    TalkCloudApp.ImMsgReqData data = TalkCloudApp.ImMsgReqData.newBuilder().setId(1).setReceiverType(1).setReceiverId(1)
                            .setResourcePath("").setMsgType(1).build();
                    TalkCloudApp.StreamRequest value = TalkCloudApp.StreamRequest.newBuilder().setUid(UserBean.getUserBean().getUserId()).setDataType(2).build();
                    request.onNext(value);
                    request.onCompleted();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        instantMessageThread.start();
    }

    private  String destFileDir = Environment.getExternalStorageDirectory()+"/media";

    private void playBack(){
        if(UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList()!= null){
            if(UserBean.getUserBean().getGroupBeanArrayList().get(position).getMessageBeanArrayList().get(playBackPosition).getType() == 3){
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

    private void sendSOS(){

    }

    private void receiverSOS(){

    }

}
