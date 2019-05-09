package janusclientapi;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusPluginHandle {

    private  MediaStream myStream = null;
    private  MediaStream remoteStream = null;
    private  SessionDescription mySdp = null;
    private  static PeerConnection pc = null;
    private DataChannel dataChannel = null;
    private  boolean trickle = true;
    private  boolean iceDone = false;
    private  boolean sdpSent = false;

    private  String VIDEO_TRACK_ID = "1929283";
    private  String AUDIO_TRACK_ID = "1928882";
    private  String LOCAL_MEDIA_ID = "1198181";

    private static VideoSource videoSource;
    private static AudioSource audioSource;

    private  final ExecutorService executor = Executors.newSingleThreadExecutor();
    private  static PeerConnectionFactory sessionFactory = null;
    private  JanusServer server;
    public  JanusSupportedPluginPackages plugin;
    public  BigInteger id;
    private  IJanusPluginCallbacks callbacks;

    private  Context _context;
    private  EglBase _rootEglBase;
    private  SurfaceTextureHelper _surfaceTextureHelper;
    private  VideoCapturer _captureAndroid;
    public  final int VIDEO_RESOLUTION_WIDTH = 320;
    public  final int VIDEO_RESOLUTION_HEIGHT = 240;
    public  final int FPS = 10;

    public JanusPluginHandle(JanusServer server, JanusSupportedPluginPackages plugin, BigInteger handle_id, IJanusPluginCallbacks callbacks) {
        this.server = server;
        this.plugin = plugin;
        this.id = handle_id;
        this.callbacks = callbacks;
    }

    //创建Factory
    public void createPeerConnectionFactory(Context context,EglBase rootEglBase) {
        executor.execute(() -> {
            _context =  context;
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(_context)
                        .createInitializationOptions());
            _rootEglBase = rootEglBase;
            final VideoEncoderFactory encoderFactory;
            final VideoDecoderFactory decoderFactory;
            boolean hardwareAccelerated = true;
            if (hardwareAccelerated) {
                encoderFactory = new DefaultVideoEncoderFactory(
                        _rootEglBase.getEglBaseContext(),
                        true,
                        true);
                decoderFactory = new DefaultVideoDecoderFactory(_rootEglBase.getEglBaseContext());
            } else {
                encoderFactory = new SoftwareVideoEncoderFactory();
                decoderFactory = new SoftwareVideoDecoderFactory();
            }
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            sessionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setAudioDeviceModule(JavaAudioDeviceModule.builder(_context).createAudioDeviceModule())
                    .setVideoEncoderFactory(encoderFactory)
                    .setVideoDecoderFactory(decoderFactory)
                    .createPeerConnectionFactory();

        });
    }
    public void createPeerConnectionFactory(Context context) {
        executor.execute(() -> {
            _context =  context;
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(_context)
                    .createInitializationOptions());

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            sessionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setAudioDeviceModule(JavaAudioDeviceModule.builder(_context).createAudioDeviceModule())
                    .createPeerConnectionFactory();
        });
    }

    //创建peerConnection
    public void createPeerConnection(IPluginHandleWebRTCCallbacks callbacks) {
        executor.execute(() -> {
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(server.iceServers);
            pc = sessionFactory.createPeerConnection(rtcConfig, new WebRtcObserver(callbacks));

            trickle = callbacks.getTrickle() != null ? callbacks.getTrickle() : false;
            AudioTrack audioTrack = null;
            VideoTrack videoTrack = null;
            MediaStream stream = null;
            if (callbacks.getMedia().getSendAudio()) {
                audioSource = sessionFactory.createAudioSource(new MediaConstraints());
                audioTrack = sessionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            }
            if (callbacks.getMedia().getSendVideo()) {
                switch (callbacks.getMedia().getCamera()) {
                    case back:
                        if(useCamera2()){
                            _captureAndroid =  createCameraCapture(new Camera2Enumerator(_context));
                        }else{
                            _captureAndroid = createCameraCapture(new Camera1Enumerator(true));
                        }
                        break;
                    case front:
                        _captureAndroid = createCameraCapture(new Camera1Enumerator(true));
                        break;
                }
                _surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", _rootEglBase.getEglBaseContext());
                videoSource = sessionFactory.createVideoSource(_captureAndroid.isScreencast());
                _captureAndroid.initialize(_surfaceTextureHelper, _context, videoSource.getCapturerObserver());
                _captureAndroid.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);
                videoTrack = sessionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            }else{
                _rootEglBase = null;
            }
            if (audioTrack != null || videoTrack != null) {
                stream = sessionFactory.createLocalMediaStream(LOCAL_MEDIA_ID);
                if (audioTrack != null) {
                    stream.addTrack(audioTrack);
                }
                if (videoTrack != null) {
                    stream.addTrack(videoTrack);
                }
            }
            myStream = stream;
            if (stream != null) {
                onLocalStream(stream);
            }
            if (myStream != null) {
                pc.addStream(myStream);
            }

            if (callbacks.getJsep() == null) {
                pc.createOffer(new WebRtcObserver(callbacks), offerOrAnswerConstraint());
            } else {
                try {
                    JSONObject obj = callbacks.getJsep();
                    String sdp = obj.getString("sdp");
                    SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(obj.getString("type"));
                    SessionDescription sessionDescription = new SessionDescription(type, sdp);
                    pc.setRemoteDescription(new WebRtcObserver(callbacks), sessionDescription);
                } catch (Exception ex) {
                    callbacks.onCallbackError(ex.getMessage());
                }
            }
        });
    }

    public void JanusSetRemoteDescription(IPluginHandleWebRTCCallbacks callbacks,boolean isUpdateMySdp){
        try {
            if(isUpdateMySdp){
                mySdp = null;
            }
            JSONObject obj = callbacks.getJsep();
            String sdp = obj.getString("sdp");
            SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(obj.getString("type"));
            SessionDescription sessionDescription = new SessionDescription(type, sdp);
            pc.setRemoteDescription(new WebRtcObserver(callbacks), sessionDescription);
        } catch (Exception ex) {
            callbacks.onCallbackError(ex.getMessage());
        }
    }

    public void createOffer(IPluginHandleWebRTCCallbacks callbacks) {
        executor.execute(() -> {
            pc.createOffer(new WebRtcObserver(callbacks), offerOrAnswerConstraint());
        });
    }

    public void createAnswer(IPluginHandleWebRTCCallbacks callbacks) {
        executor.execute(() -> {
            pc.createAnswer(new WebRtcObserver(callbacks), offerOrAnswerConstraint());
        });
    }


    public void onMessage(String msg) {
        try {
            JSONObject obj = new JSONObject(msg);
            callbacks.onMessage(obj, null);
        } catch (JSONException ex) {
            //TODO do we want to notify the GatewayHandler?
        }
    }

    public void onMessage(JSONObject msg, JSONObject jsep) {
        callbacks.onMessage(msg, jsep);
    }

    private void onLocalStream(MediaStream stream) {
        callbacks.onLocalStream(stream);
    }

    private void onRemoteStream(MediaStream stream) {
        callbacks.onRemoteStream(stream);
    }

    public void onDataOpen(Object data) {
        callbacks.onDataOpen(data);
    }

    public void onData(Object data) {
        callbacks.onData(data);
    }

    public void onCleanup() {
        callbacks.onCleanup();
    }

    public void onDetached() {
        callbacks.onDetached();
    }

    public void sendMessage(IPluginHandleSendMessageCallbacks obj) {
        executor.execute(() -> {
            server.sendMessage(TransactionType.plugin_handle_message, id, obj, plugin);
        });
    }

    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(_context);
    }

    public void hangUp() {
       if (audioSource != null) {
           audioSource.dispose();
           audioSource = null;
       }

       if (videoSource != null) {
           videoSource.dispose();
           videoSource = null;
       }

       if (_captureAndroid != null) {
           try {
               _captureAndroid.stopCapture();
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
           _captureAndroid.dispose();
           _captureAndroid = null;
       }

       if (_surfaceTextureHelper != null) {
           _surfaceTextureHelper.dispose();
           _surfaceTextureHelper = null;
       }

        if (pc != null && pc.signalingState() != PeerConnection.SignalingState.CLOSED){
            pc.close();
            pc = null;
        }

       if (sessionFactory != null) {
           sessionFactory.dispose();
           sessionFactory = null;
       }

       mySdp = null;
       if (dataChannel != null)
           dataChannel.close();
       dataChannel = null;
       trickle = true;
       iceDone = false;
       sdpSent = false;

    }

    public void detach() {
        hangUp();
        JSONObject obj = new JSONObject();
        server.sendMessage(obj, JanusMessageType.detach, id);
    }

    public void janusDetach() {
        JSONObject obj = new JSONObject();
        server.sendMessage(obj, JanusMessageType.detach, id);
    }

    private  final String VIDEO_CODEC_H264 = "H264";
    private  void onLocalSdp(SessionDescription sdp, IPluginHandleWebRTCCallbacks callbacks) {
        executor.execute(() ->{
            if (mySdp == null) {
                mySdp = sdp;
                if(_rootEglBase != null){
                    String sdpDescription = sdp.description;
                    sdpDescription = preferCodec(sdpDescription, VIDEO_CODEC_H264, false);
                    SessionDescription localsdp = new SessionDescription(sdp.type, sdpDescription);
                    pc.setLocalDescription(new WebRtcObserver(callbacks), localsdp);
                }else{
                    pc.setLocalDescription(new WebRtcObserver(callbacks), sdp);
                }
            }
            if (!iceDone && !trickle)
                return;
            if (sdpSent)
                return;
            try {
                sdpSent = true;
                JSONObject obj = new JSONObject();
                obj.put("sdp", mySdp.description);
                obj.put("type", mySdp.type.canonicalForm());
                callbacks.onSuccess(obj);
            } catch (JSONException ex) {
                callbacks.onCallbackError(ex.getMessage());
            }

        });
    }

    private static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        final String[] lines = sdpDescription.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            return sdpDescription;
        }

        final List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (String line : lines) {
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            return sdpDescription;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdpDescription;
        }

        lines[mLineIndex] = newMLine;
        return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
    }

    private static @Nullable
    String movePayloadTypesToFront(
            List<String> preferredPayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
        // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    private static String joinString(
            Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
       if(_rootEglBase == null){
           keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(false)));
       }else{
           keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(true)));
       }
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    private void sendTrickleCandidate(IceCandidate candidate) {
        try {
            JSONObject message = new JSONObject();
            JSONObject cand = new JSONObject();
            if (candidate == null)
                cand.put("completed", true);
            else {
                cand.put("candidate", candidate.sdp);
                cand.put("sdpMid", candidate.sdpMid);
                cand.put("sdpMLineIndex", candidate.sdpMLineIndex);
            }
            message.put("candidate", cand);

            server.sendMessage(message, JanusMessageType.trickle, id);
        } catch (JSONException ex) {

        }
    }

    private void sendSdp(IPluginHandleWebRTCCallbacks callbacks) {
        if (mySdp != null) {
            mySdp = pc.getLocalDescription();
            if (!sdpSent) {
                sdpSent = true;
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("sdp", mySdp.description);
                    obj.put("type", mySdp.type.canonicalForm());
                    callbacks.onSuccess(obj);
                } catch (JSONException ex) {
                    callbacks.onCallbackError(ex.getMessage());
                }
            }
        }
    }

    private class WebRtcObserver implements SdpObserver, PeerConnection.Observer {
        private final IPluginHandleWebRTCCallbacks webRtcCallbacks;
        public WebRtcObserver(IPluginHandleWebRTCCallbacks callbacks) {
            this.webRtcCallbacks = callbacks;
        }

        @Override
        public void onSetSuccess() {
            Log.d("JANUSCLIENT", "On Set Success");
            if (mySdp == null) {
                createAnswer(webRtcCallbacks);
            }
        }

        @Override
        public void onSetFailure(String error) {
            Log.d("JANUSCLIENT", "On set Failure");
            //todo JS api does not account for this
            webRtcCallbacks.onCallbackError(error);
        }

        @Override
        public void onCreateSuccess(SessionDescription sdp) {
            Log.d("JANUSCLIENT", "Create success");
            onLocalSdp(sdp, webRtcCallbacks);
        }

        @Override
        public void onCreateFailure(String error) {
            Log.d("JANUSCLIENT", "Create failure");
            webRtcCallbacks.onCallbackError(error);
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState state) {
            Log.d("JANUSCLIENT", "Signal change " + state.toString());
            switch (state) {
                case STABLE:
                    break;
                case HAVE_LOCAL_OFFER:
                    break;
                case HAVE_LOCAL_PRANSWER:
                    break;
                case HAVE_REMOTE_OFFER:
                    break;
                case HAVE_REMOTE_PRANSWER:
                    break;
                case CLOSED:
                    break;
            }
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state) {
            Log.d("JANUSCLIENT", "Ice Connection change " + state.toString());
            switch (state) {
                case DISCONNECTED:
                    Log.e("janusServer","-----------webRTC disconnected------------");
                    break;
                case CONNECTED:
                    Log.e("janusServer","-----------webRTC connected------------");
                    callbacks.onCallbackError("webRtcconnected");
                    break;
                case NEW:
                    Log.e("janusServer","-----------webRTC new------------");
                    break;
                case CHECKING:
                    Log.e("janusServer","-----------webRTC checking------------");
                    break;
                case CLOSED:
                    Log.e("janusServer","-----------webRTC closed------------");
                    break;
                case FAILED:
                    Log.e("janusServer","-----------webRTC failed------------");
                    callbacks.onCallbackError("webRtcfailed");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state) {
            switch (state) {
                case NEW:
                    break;
                case GATHERING:
                    break;
                case COMPLETE:
                    if(!trickle) {
                        mySdp = pc.getLocalDescription();
                        sendSdp(webRtcCallbacks);
                    } else {
                        sendTrickleCandidate(null);
                    }
                    break;
                default:
                    break;
            }
            Log.d("JANUSCLIENT", "Ice Gathering " + state.toString());
        }

        @Override
        public void onIceCandidate(IceCandidate candidate) {
            if(trickle){
                sendTrickleCandidate(candidate);
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream stream) {
            onRemoteStream(stream);
        }

        @Override
        public void onRemoveStream(MediaStream stream) {
            Log.d("JANUSCLIENT", "onRemoveStream");
        }

        @Override
        public void onDataChannel(DataChannel channel) {
            Log.d("JANUSCLIENT", "onDataChannel");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d("JANUSCLIENT", "Renegotiation needed");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }

    }

}
