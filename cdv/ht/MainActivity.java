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
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.mob.MobSDK;
import com.mob.moblink.Scene;
import com.mob.moblink.SceneRestorable;
import com.tencent.smtt.sdk.QbSdk;
import org.apache.cordova.*;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends CordovaActivity implements SceneRestorable
{

    private final static int MY_INIT_PERMISSIONS_REQUES = 168666;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        if (!QbSdk.isTbsCoreInited()) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_INIT_PERMISSIONS_REQUES);
            } else {

                QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
                    @Override
                    public void onViewInitFinished(boolean arg0) {
                        loadUrl(launchUrl);
                    }

                    @Override
                    public void onCoreInitFinished() {
                    }
                };
                QbSdk.initX5Environment(getApplicationContext(), cb);
                // QbSdk.preInit(getApplicationContext(), cb);
            }

        } else {
            /*
            QbSdk.initX5Environment(getApplicationContext(), null);
            new Handler().postDelayed(new Runnable(){
                public void run() {
                }
            }, 500);
            */

            // Set by <content src="index.html" /> in config.xml
            loadUrl(launchUrl);
        }

        // 初始化MobSDK
        MobSDK.init(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_INIT_PERMISSIONS_REQUES:
                QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
                    @Override
                    public void onViewInitFinished(boolean arg0) {
                        loadUrl(launchUrl);
                    }

                    @Override
                    public void onCoreInitFinished() {
                    }
                };
                QbSdk.initX5Environment(getApplicationContext(), cb);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        setContentView(webViewLayout);

        appView.getView().setBackgroundColor(Color.TRANSPARENT);

        appView.getView().requestFocusFromTouch();
    }

    @Override
    public void onReturnSceneData(Scene scene) {
        // 处理场景还原数据, 更新画面
        String path = this.launchUrl + "#" + scene.params.get("path").toString();
        Log.d("hellocordova", path);
        this.appView.getEngine().loadUrl(path, true);
    }

}
