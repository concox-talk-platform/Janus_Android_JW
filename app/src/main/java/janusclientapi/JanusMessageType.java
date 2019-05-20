package janusclientapi;

/**
 * Created by ben.trent on 5/8/2015.
 */
public enum JanusMessageType {
    message,
    trickle,
    detach,
    destroy,
    keepalive,
    create,
    attach,
    event,
    error,
    ack,
    success,
    webrtcup,
    hangup,
    detached,
    media,
    user_call,
    slowlink,
    timeout,
    claim,
    user_called;
    @Override
    public String toString() {
        return name();
    }

    public boolean EqualsString(String type) {
        return this.toString().equals(type);
    }

    public static JanusMessageType fromString(String string) {
        return (JanusMessageType) valueOf(JanusMessageType.class, string.toLowerCase());
    }
}
