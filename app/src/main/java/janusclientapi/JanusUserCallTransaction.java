package janusclientapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusUserCallTransaction implements ITransactionCallbacks {

    private final IJanusUserCallCallbacks callbacks;

    public JanusUserCallTransaction(IJanusUserCallCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public TransactionType getTransactionType() {
        return TransactionType.user_call;
    }

    @Override
    public void reportSuccess(JSONObject obj) {
        try {
            JanusMessageType type = JanusMessageType.fromString(obj.getString("janus"));
            if (type != JanusMessageType.user_called) {
                callbacks.onCallbackError(obj.getJSONObject("error").getString("reason"));
            } else {
                callbacks.onUserCallSuccess((obj));
            }
        } catch (JSONException ex) {
            callbacks.onCallbackError(ex.getMessage());
        }
    }
}
