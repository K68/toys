package com.mob.js;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.mob.js.listener.MobPushDemoListener;
import com.mob.js.listener.MobPushListener;
import com.mob.js.listener.MobPushRegIdCallback;
import com.mob.pushsdk.MobPush;
import com.mob.pushsdk.MobPushCustomNotification;
import com.mob.pushsdk.MobPushLocalNotification;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.UIHandler;
import com.zsxsoft.cordova.x5.X5WebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class MobPushUtils implements Handler.Callback {
    private static final String GET_REGISTRATION_ID = "getRegistrationID";
    private static final String ADD_PUSH_RECEIVER = "addPushReceiver";
    public static final String SET_ALIAS = "setAlias";
    public static final String GET_ALIAS = "getAlias";
    public static final String DELETE_ALIAS = "deleteAlias";
    public static final String ADD_TAGS = "addTags";
    public static final String GET_TAGS = "getTags";
    public static final String DELETE_TAGS = "deleteTags";
    public static final String CLEAN_TAGS = "cleanAllTags";
    public static final String SEND_CUSTOM_MSG = "sendCustomMsg";
    public static final String SEND_APNS_MSG = "sendAPNsMsg";
    public static final String SEND_LOCAL_NOTIFY = "sendLocalNotify";

    public static final int MSG_LOAD_URL = 1; // load js script
    public static final int MSG_JS_CALL = 2; // process js callback on ui thread

    private X5WebView webview;
    private Hashon hashon;
    private MobPushListener mobPushListener;
    private boolean inited = false;

    public static MobPushUtils prepare(X5WebView webview) {
        return new MobPushUtils(webview);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private MobPushUtils(X5WebView webview) {
        hashon = new Hashon();

        this.webview = webview;

        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(this, "JSInterfaceMobPush");
    }

    /* process js init function */
    public void onInit() {
        if (inited)
            return;
        inited = true;
        // platform type: 1 for android, 2 for ios
        Log.d("MobPushUtils ===", "initPushSDK");
        String script = "javascript:$mobpush.initPushSDK(1);";
        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = script;
        UIHandler.sendMessage(msg, this);
    }

    /**
     * receive js callback
     * <p>
     * respons: {
     * "seqId" : "111111",
     * "platform" : 1,
     * "state" : 1, // Success = 1, Fail = 2, Cancel = 3
     * "data" : "user or share response",
     * "method":"geiUserInfo",
     * "callback" : "function string",
     * "error" :
     * {
     * "error_level" : 1,
     * "error_code" : 11,
     * "error_msg" : "adsfdsaf",
     * }
     * }
     */
    @JavascriptInterface
    public void jsCallback(String seqId, String api, String data, String callback) {
        // this is in webview core thread, not in ui thread
        Message msg = new Message();
        msg.what = MSG_JS_CALL;
        msg.obj = new Object[]{seqId, api, data, callback};
        UIHandler.sendMessage(msg, this);
    }

    /**
     * receive js log
     */
    @JavascriptInterface
    public void jsLog(String msg) {
        Log.w("MobPush for JS", msg == null ? "" : msg);
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_LOAD_URL: {
                webview.loadUrl(String.valueOf(msg.obj));
            }
            break;
            case MSG_JS_CALL: {
                jsCallback((Object[]) msg.obj);
            }
            break;
        }
        return false;
    }

    /**
     * callback main method
     * Every request is allocated by this function call,
     * Including the number of requests, the callback and data
     *
     * @param objs
     */
    private void jsCallback(Object[] objs) {

        String seqId = (String) objs[0];
        String api = (String) objs[1];
        String data = (String) objs[2];
        String callback = (String) objs[3];

        HashMap<String, Object> req = null;
        try {
            req = hashon.fromJson(data);
            if (req == null) {
                Throwable t = new Throwable("wrong request data: " + data);
                onRequestFailed(seqId, api, callback, null, t);
                return;
            }
        } catch (Throwable t) {
            onRequestFailed(seqId, api, callback, null, t);
            return;
        }
        String oriCallback = (String) req.get("callback");
        HashMap<String, Object> resp = new HashMap<String, Object>();
        resp.put("seqId", seqId);
        resp.put("method", api);
        resp.put("callback", oriCallback);
        if (GET_REGISTRATION_ID.equals(api)) {
            getRegistrationId(seqId, api, callback, oriCallback);
            return;
        } else if (ADD_PUSH_RECEIVER.equals(api)) {
            addPushReceiver(seqId, api, callback, oriCallback);
        } else if (SET_ALIAS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                if (parmsMap != null && parmsMap.containsKey("alias")) {
                    String alias = (String) parmsMap.get("alias");
                    setAlias(alias);
                }
            }
        } else if (GET_ALIAS.equals(api)) {
            getAlias();
        } else if (DELETE_ALIAS.equals(api)) {
            deleteAlias();
        } else if (ADD_TAGS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                ArrayList<String> tags = (ArrayList<String>) parmsMap.get("tags");
                String[] ts = tags.toArray(new String[]{});
                addTags(ts);
            }
        } else if (GET_TAGS.equals(api)) {
            getTags();
        } else if (DELETE_TAGS.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                ArrayList<String> tags = (ArrayList<String>) parmsMap.get("tags");
                String[] ts = tags.toArray(new String[]{});
                deleteTags(ts);
            }
        } else if (CLEAN_TAGS.equals(api)) {
            cleanTags();
        } else if (SEND_CUSTOM_MSG.equals(api) || SEND_APNS_MSG.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                String text = (String) parmsMap.get("content");
                int space = 0;
                if (parmsMap.containsKey("timedSpace")) {
                    space = Integer.parseInt(parmsMap.get("timedSpace").toString());
                } else if (parmsMap.containsKey("space")) {
                    space = Integer.parseInt(parmsMap.get("space").toString());
                }
                int msgType = Integer.parseInt(parmsMap.get("msgType").toString());
                String extras = (String) parmsMap.get("extras");
                sendNotify(msgType, text, space, seqId, api, callback, oriCallback, extras);
                return;
            }
        } else if (SEND_LOCAL_NOTIFY.equals(api)) {
            if (req.containsKey("msgParams")) {
                HashMap<String, Object> parmsMap = (HashMap<String, Object>) req.get("msgParams");
                String text = (String) parmsMap.get("content");
                String title = (String) parmsMap.get("title");
                int space = 0;
                if (parmsMap.containsKey("space")) {
                    space = Integer.parseInt(parmsMap.get("space").toString());
                }
                
                String path = (String) parmsMap.get("path");
                HashMap<String, String> extras = new HashMap<String, String>();
                extras.put("path", path);
                
                MobPushLocalNotification noti = new MobPushLocalNotification();
                noti.setTitle(title);
                noti.setContent(text);
                noti.setNotificationId(new Random().nextInt());
                noti.setTimestamp(space * 60 * 1000 + System.currentTimeMillis());
                addLocalNotification(noti);
                resp.put("content", text);
                resp.put("title", title);
                resp.put("subTitle", parmsMap.get("subTitle"));
                resp.put("sound", parmsMap.get("sound"));
                resp.put("badge", parmsMap.get("badge"));
            }
        }

        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = "javascript:" + callback + "(" + hashon.fromHashMap(resp) + ");";
        UIHandler.sendMessage(msg, this);
    }

    private HashMap<String, Object> throwableToMap(Throwable t) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("msg", t.getMessage());
        ArrayList<HashMap<String, Object>> traces = new ArrayList<HashMap<String, Object>>();
        for (StackTraceElement trace : t.getStackTrace()) {
            HashMap<String, Object> element = new HashMap<String, Object>();
            element.put("cls", trace.getClassName());
            element.put("method", trace.getMethodName());
            element.put("file", trace.getFileName());
            element.put("line", trace.getLineNumber());
            traces.add(element);
        }
        map.put("stack", traces);
        Throwable cause = t.getCause();
        if (cause != null) {
            map.put("cause", throwableToMap(cause));
        }
        return map;
    }

    //request fail
    private void onRequestFailed(String seqId, String api, String callback, String oriCallback, Throwable t) {
        HashMap<String, Object> resp = new HashMap<String, Object>();
        resp.put("seqId", seqId);
        resp.put("state", 2);
        resp.put("method", api);
        resp.put("callback", oriCallback);
        HashMap<String, Object> error = new HashMap<String, Object>();
        error.put("error_level", 1);
        error.put("error_code", 0);
        error.put("error_msg", t.getMessage());
        error.put("error_detail", throwableToMap(t));
        resp.put("error", error);

        Message msg = new Message();
        msg.what = MSG_LOAD_URL;
        msg.obj = "javascript:" + callback + "(" + hashon.fromHashMap(resp) + ");";
        UIHandler.sendMessage(msg, this);
    }

    // ============================ Java Actions ============================

    private void getRegistrationId(String seqId, String api, String callback, String oriCallback) {
        MobPushRegIdCallback idCallback = new MobPushRegIdCallback();
        idCallback.setApi(api);
        idCallback.setSeqId(seqId);
        idCallback.setCallback(this);
        idCallback.setJsCallback(callback);
        idCallback.setOriCallback(oriCallback);
        MobPush.getRegistrationId(idCallback);
    }

    private void addPushReceiver(String seqId, String api, String callback, String oriCallback) {
        if (mobPushListener == null) {
            mobPushListener = new MobPushListener();
            MobPush.addPushReceiver(mobPushListener);
            mobPushListener.setCallback(this);
        }
    }

    private void setAlias(String alias) {
        MobPush.setAlias(alias);
    }

    private void getAlias() {
        MobPush.getAlias();
    }

    private void deleteAlias() {
        MobPush.deleteAlias();
    }

    private void addTags(String[] tags) {
        MobPush.addTags(tags);
    }

    private void getTags() {
        MobPush.getTags();
    }

    private void deleteTags(String[] tags) {
        MobPush.deleteTags(tags);
    }

    private void cleanTags() {
        MobPush.cleanTags();
    }

    private void addLocalNotification(MobPushLocalNotification var0) {
        MobPush.addLocalNotification(var0);
    }

    private void sendNotify(int type, String text, int space, String seqId, String api, String callback, String oriCallback, String extras) {
        MobPushDemoListener mobPushDemoListener = new MobPushDemoListener();
        mobPushDemoListener.setApi(api);
        mobPushDemoListener.setSeqId(seqId);
        mobPushDemoListener.setCallback(this);
        mobPushDemoListener.setJsCallback(callback);
        mobPushDemoListener.setOriCallback(oriCallback);
        SimulateRequest.sendPush(type, text, space, extras, mobPushDemoListener);
    }

}
