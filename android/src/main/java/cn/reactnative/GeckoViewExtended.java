package cn.reactnative;

import android.app.AlertDialog;
import android.util.Log;
import android.view.ContentInfo;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.gecko.GeckoNetworkManager;
import org.mozilla.gecko.GeckoThread;
import org.mozilla.gecko.util.DebugConfig;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebRequestError;

import com.facebook.react.views.scroll.ScrollEventType;
import com.google.gson.*;

public class GeckoViewExtended extends GeckoView implements WebExtension.MessageDelegate, GeckoSession.PromptDelegate, GeckoSession.NavigationDelegate, GeckoSession.ProgressDelegate, WebExtension.PortDelegate, GeckoSession.ScrollDelegate, GeckoSession.ContentDelegate, ContentBlocking.Delegate {
    private ReactContext reactContext;
    public WebExtension.Port messagePort;
    private GeckoRuntime rt;
    private WebExtension mExtension;
    private JSONObject mMessageToSendOnConnect;
    private String mInjectedJavaScript;

    public GeckoViewExtended(ReactContext context, GeckoRuntime runtime) {
        super(context);
        rt = runtime;
        reactContext = context;
        GeckoSession session = new GeckoSession();
        session.open(runtime);
        this.setSession(session);
        this.setLayoutParams(
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        connectMessagingPort(null);
        session.setNavigationDelegate(this);
        session.setProgressDelegate(this);
        session.setContentBlockingDelegate(this);
        session.setScrollDelegate(this);
        session.setContentDelegate(this);
        runtime.getSettings().setJavaScriptEnabled(true);
        runtime.getSettings().setWebManifestEnabled(true);
        runtime.getSettings().setConsoleOutputEnabled(true);
    }

    public  void setInjectedJavaScript(String script) {
        mInjectedJavaScript = script;
    }


    public void connectMessagingPort(JSONObject object) {
        GeckoViewExtended view = this;
        if (mExtension != null) {
            mMessageToSendOnConnect = object;
            mExtension.setMessageDelegate(view,"browser");
            return;
        }
        rt.getWebExtensionController()
                .ensureBuiltIn("resource://android/assets/messaging/", "messaging@notesnook.com")
                .accept(
                        extension -> {

                            Log.i("MessageDelegate", "Extension installed: " + extension);
                            reactContext.runOnUiQueueThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (extension != null) {
                                        mExtension = extension;
                                        extension.setMessageDelegate(view, "browser");
                                        Log.i("MessageDelegate", "Message Delegate Installed");
                                    }

                                }
                            });
                        },
                        e -> Log.e("MessageDelegate", "Error registering WebExtension", e)
                );


    }

    @Nullable
    @Override
    public GeckoResult<String> onLoadError(@NonNull GeckoSession session, @Nullable String uri, @NonNull WebRequestError error) {
        WritableMap map = Arguments.createMap();
        map.putString("error",error.getLocalizedMessage());
        map.putString("uri",uri);
        dispatchEvent(this,GeckoViewExtendedEvents.loadingError,map);
        return GeckoSession.NavigationDelegate.super.onLoadError(session, uri, error);
    }

    @Nullable
    @Override
    public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
        return GeckoSession.NavigationDelegate.super.onLoadRequest(session, request);
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onAlertPrompt(@NonNull GeckoSession session, @NonNull AlertPrompt prompt) {
        Log.d("GeckoAlert",prompt.message + prompt.title);
        AlertDialog dialog = new AlertDialog.Builder(reactContext).setTitle(prompt.title).setMessage(prompt.message).setCancelable(true).create();
        dialog.show();
        return GeckoSession.PromptDelegate.super.onAlertPrompt(session, prompt);
    }

    public void setSource(ReadableMap source) {
        GeckoSession session = this.getSession();
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                GeckoSession.Loader loader = new GeckoSession.Loader();
                loader.data(html, "text/html");
                session.load(loader);
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                session.loadUri(url);
                return;
            }
        }
        session.loadUri("about:blank");
    }

    @Override
    public void onPageStart(@NonNull GeckoSession session, @NonNull String url) {
        WritableMap map = Arguments.createMap();
        map.putString("uri",url);
        dispatchEvent(this,GeckoViewExtendedEvents.loadingStart,map);
        GeckoSession.ProgressDelegate.super.onPageStart(session, url);
    }

    @Override
    public void onProgressChange(@NonNull GeckoSession session, int progress) {
        WritableMap map = Arguments.createMap();
        map.putInt("progress",progress);
        dispatchEvent(this,GeckoViewExtendedEvents.loadingProgress,map);
        GeckoSession.ProgressDelegate.super.onProgressChange(session, progress);
    }

    @Override
    public void onPageStop(@NonNull GeckoSession session, boolean success) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("success",success);
        if (success) {
            injectJavascript();
        }
        dispatchEvent(this,GeckoViewExtendedEvents.loadingFinish,map);
        GeckoSession.ProgressDelegate.super.onPageStop(session, success);
    }

    @Override
    public void onFirstContentfulPaint(@NonNull GeckoSession session) {
        GeckoSession.ContentDelegate.super.onFirstContentfulPaint(session);
    }

    @Nullable
    @Override
    public ContentInfo onReceiveContent(@NonNull ContentInfo payload) {
        return super.onReceiveContent(payload);
    }

    @Override
    public void onKill(@NonNull GeckoSession session) {
        GeckoSession.ContentDelegate.super.onKill(session);
    }

    @Override
    public void onScrollChanged(@NonNull GeckoSession session, int scrollX, int scrollY) {
        WritableMap map = Arguments.createMap();
        map.putDouble("scrollX",scrollX);
        map.putDouble("scrollY",scrollY);
        dispatchEvent(this, ScrollEventType.getJSEventName(ScrollEventType.SCROLL),map);
        GeckoSession.ScrollDelegate.super.onScrollChanged(session, scrollX, scrollY);
    }

    @Nullable
    @Override
    public GeckoResult<Object> onMessage(@NonNull String nativeApp, @NonNull Object message, @NonNull WebExtension.MessageSender sender) {
        Log.d("MessageDelegate","message" + message);
        if (message instanceof JSONObject) {
            WritableMap map = Arguments.createMap();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            map.putString("data",gson.toJson(message));
            dispatchEvent(this, GeckoViewExtendedEvents.message,map);
        }
        return WebExtension.MessageDelegate.super.onMessage(nativeApp, message, sender);

    }

    public void injectJavascript() {
        if (mInjectedJavaScript != null && this.messagePort != null) {
            try {
                JSONObject object = new JSONObject();
                object.put("inject", mInjectedJavaScript);
                this.messagePort.postMessage(object);
            } catch (JSONException e) {
                Log.d("GeckoViewExtended", e.getMessage());
            }
        }
    }

    @Nullable
    @Override
    public void onConnect(@NonNull WebExtension.Port port) {
        WebExtension.MessageDelegate.super.onConnect(port);
        Log.d("PortConnect","onConnect" + port);
        this.messagePort = port;
        injectJavascript();
        if (mMessageToSendOnConnect != null) {
            port.postMessage(mMessageToSendOnConnect);
            mMessageToSendOnConnect = null;
        }
        port.setDelegate(this);
    }

    @Override
    public void onPortMessage(@NonNull Object message, @NonNull WebExtension.Port port) {
        WebExtension.PortDelegate.super.onPortMessage(message, port);
        messagePort = port;
        Log.d("PortMessage","message" + message);
        if (message instanceof JSONObject) {
            WritableMap map = Arguments.createMap();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            map.putString("data",gson.toJson(message));
            dispatchEvent(this, GeckoViewExtendedEvents.message,map);
        } else if (message instanceof String){
            WritableMap map = Arguments.createMap();
            map.putString("data", (String) message);
            dispatchEvent(this, GeckoViewExtendedEvents.message,map);
        }
    }

    @NonNull
    @Override
    public void onDisconnect(@NonNull WebExtension.Port port) {
        this.messagePort = null;
        dispatchEvent(this,GeckoViewExtendedEvents.messagingDisconnected, Arguments.createMap());
        WebExtension.PortDelegate.super.onDisconnect(port);
    }

    protected void dispatchEvent(GeckoViewExtended webView,String eventName, WritableMap event) {
        ReactContext reactContext = (ReactContext) webView.getContext();
        reactContext
                .getJSModule(RCTEventEmitter.class)
                .receiveEvent(webView.getId(), eventName, event);
    }
}
