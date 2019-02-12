/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.amzport.haitang;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
// import com.mob.MobSDK;
import com.mob.moblink.MobLink;
import com.mob.moblink.Scene;
import com.mob.moblink.SceneRestorable;
import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;
import org.apache.cordova.*;

import java.util.HashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends CordovaActivity implements SceneRestorable
{

    private final static int MY_INIT_PERMISSIONS_REQUES = 168666;
    private final static int REQUEST_PERMISSION_SETTING = 168888;
    public Boolean tbsInited = false;
    public String toPath = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        RelativeLayout preloadLayout = new RelativeLayout(this);
        preloadLayout.setBackgroundColor(Color.TRANSPARENT);
        preloadLayout.setBackgroundResource(R.drawable.splash_screen);
        setContentView(preloadLayout);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // 在调用TBS初始化、创建WebView之前进行如下配置，以开启优化方案
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        QbSdk.initTbsSettings(map);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_INIT_PERMISSIONS_REQUES);
        } else {
            if (QbSdk.isTbsCoreInited()) {
                Log.d("amzport-link", "X5 preInit: isTbsCoreInited");
                launchTheUrl();
            } else {
                Log.d("amzport-link", "X5 preInit: initX5");
                QbSdk.initX5Environment(getApplicationContext(), sdkCB);
                // QbSdk.preInit(getApplicationContext(), sdkCB);
            }
        }

        // 初始化MobSDK
        // MobSDK.init(this);
    }

    private QbSdk.PreInitCallback sdkCB = new QbSdk.PreInitCallback() {
        @Override
        public void onViewInitFinished(boolean arg0) {
            Log.d("amzport-link","initMethod");
            launchTheUrl();
        }
        @Override
        public void onCoreInitFinished() {
        }
    };

    private void launchTheUrl() {
        tbsInited = true;
        if (toPath == null) {
            Log.d("amzport-link", "launchTheUrl load /");
            loadUrl(launchUrl);
        } else {
            Log.d("amzport-link", "launchTheUrl load the URL");
            loadUrl(launchUrl + '#' + toPath);
        }
    }

    @Override
    public void onReturnSceneData(Scene scene) {
        if (scene != null) {
            // 处理场景还原数据, 更新画面
            Object path = scene.params.get("path");
            if (path != null) {
                String _path = path.toString();
                if (_path.equals("/")) {
                    Log.d("amzport-link", "onReturnSceneData: Root Path [ / ]");
                } else {
                    toPath = _path;
                    Log.d("amzport-link", "onReturnSceneData: " + toPath);
                    if (tbsInited) {
                        // loadUrl(launchUrl + '#' + toPath);
                        appView.getEngine().evaluateJavascript("window.location.hash='#" + toPath + "';window.location.reload();", null);
                    }
                }
            } else {
                Log.d("amzport-link", "onReturnSceneData: Null Path");
            }
        }
    }

    // 必须重写该方法，防止MobLink在某些情景下无法还原
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        MobLink.updateNewIntent(getIntent(), this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_PERMISSION_SETTING:
                initMethod(false);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_INIT_PERMISSIONS_REQUES:
                initMethod(true);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initMethod(boolean again) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
            if (again) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                Toast.makeText(this, "电话权限被禁用，请在权限管理修改", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "电话权限被禁用，应用将自动退出", Toast.LENGTH_SHORT).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finishAndRemoveTask();
                    }
                }, 350);
            }
        } else {
            Log.d("amzport-link", "X5 initX5Environment");
            QbSdk.initX5Environment(getApplicationContext(), sdkCB);
        }
    }

    @Override
    protected void createViews() {
        appView.getView().setId(100);

        RelativeLayout webViewLayout = new RelativeLayout(this);
        webViewLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        FrameLayout mViewParent = new FrameLayout(this);
        mViewParent.setId(101);
        mViewParent.addView(appView.getView(), new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        webViewLayout.addView(mViewParent, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        appView.getView().setBackgroundColor(Color.TRANSPARENT);
        appView.getView().setBackgroundResource(R.drawable.splash_screen);
//        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        setContentView(webViewLayout);

        appView.getView().requestFocusFromTouch();
    }

}
