package janusclientapi;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusSendPluginMessageTransaction implements ITransactionCallbacks {
    private final IPluginHandleSendMessageCallbacks callbacks;

    public JanusSendPluginMessageTransaction(JanusSupportedPluginPackages plugin, IPluginHandleSendMessageCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public TransactionType getTransactionType() {
        return TransactionType.plugin_handle_message;
    }

    public void reportSuccess(JSONObject obj) {
        try {
            JanusMessageType type = JanusMessageType.fromString(obj.getString("janus"));
            switch (type) {
                case success: {
                    if(obj.has("plugindata")){
                        JSONObject plugindata = obj.getJSONObject("plugindata");
                        JanusSupportedPluginPackages plugin = JanusSupportedPluginPackages.fromString(plugindata.getString("plugin"));
                        JSONObject data = plugindata.getJSONObject("data");
                        if (plugin == JanusSupportedPluginPackages.JANUS_NONE) {
                            callbacks.onCallbackError("unexpected message: \n\t" + obj.toString());
                        } else {
                            callbacks.onSuccessSynchronous(data);
                        }
                    }else{
                        callbacks.onSuccessSynchronous(new JSONObject().put("claim","success"));
                    }
                    break;
                }
                case ack: {
                    callbacks.onSuccessAsynchronous();
                    break;
                }
                default: {
                    callbacks.onCallbackError(obj.toString());
                    break;
                }
            }

        } catch (JSONException ex) {
            callbacks.onCallbackError(ex.getMessage());
        }
    }
}
