var onMsgCB;
var onMsgBody;

function MobPush()
{
    var isRunning = false;         //是否正在与本地进行交互
    var isDebug = true;            //是否打开调试
    var isSendInitRequest = false; //是否已经发送初始化请求
    var initCallbackFuncs = [];    //初始化回调方法
    var apiCaller = null;          //API调用器

    var seqId = 0;
    var firstRequest = null;
    var lastRequest = null;
    var jsLog = null;

    /**
     * SDK方法名称
     * @type {object}
     */
    var MobPushMethodName =
    {
        "InitMobPushSDK" : "initMobPushSDK",
        "SendCustomMsg" : "sendCustomMsg",
        "SendAPNsMsg" : "sendAPNsMsg",
        "SendLocalNotify" : "sendLocalNotify",
        "GetRegistrationID" : "getRegistrationID",
        "SetAlias" : "setAlias",
        "GetAlias" : "getAlias",
        "DeleteAlias" : "deleteAlias",
        "AddTags" : "addTags",
        "GetTags" : "getTags",
        "DeleteTags" : "deleteTags",
        "CleanAllTags" : "cleanAllTags",
        "AddPushReceiver" : "addPushReceiver",
    };

    /**
     * 请求信息
     * @param seqId         流水号
     * @param method        方法
     * @param params        参数集合
     * @constructor
     */
    var RequestInfo = function (seqId, method, params)
    {
        this.seqId = seqId;
        this.method = method;
        this.params = params;
        this.nextRequest = null;
    };


    /**
     * Android接口调用器
     * @constructor
     */
    var AndroidAPICaller = function ()
    {
        /**
         * 调用方法
         * @param request       请求信息
         */
        this.callMethod = function (request)
        {
            if (isDebug) {
                jsLog.log("js request: " + request.method);
                jsLog.log("seqId = " + request.seqId.toString());
                jsLog.log("api = " + request.method);
                jsLog.log("data = " + ObjectToJsonString(request.params));
            }

            //java接口
            window.JSInterface.jsCallback(request.seqId.toString(), request.method, ObjectToJsonString(request.params), "$mobpush.callback");
        };

        /**
         * 返回回调
         * @param response      回复数据
         *
         * response结构
         * {
         *   "seqId" : "111111",
         *   "extra" : "sss",
         *   "content" : "sww",
         *   "callback" : "function string",
         *   "messageId" : "32353",
         *   "timeStamp" :  2313498473213,
         *   "errorCode" : 400,
         *   "errorMsg" : "error",
         * }
         */
        this.callback = function (response)
        {
            var logMsg = "java returns: " + JSON.stringify(response);
            if (isDebug) {
                jsLog.log(logMsg);
            }
            if (response.callback)
            {
                var callbackFunc = eval(response.callback);

                if (callbackFunc)
                {
                    var method = response.method;

                    switch (method)
                    {
                        case MobPushMethodName.SendCustomMsg:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.SendAPNsMsg:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.SendLocalNotify:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.GetRegistrationID:
                            callbackFunc(response.seqId, response.registrationID);
                            break;
                        case MobPushMethodName.SetAlias:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.GetAlias:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.DeleteAlias:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.AddTags:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.GetTags:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.DeleteTags:
                            callbackFunc(response.seqId);
                            break;
                        case MobPushMethodName.CleanAllTags:
                            callbackFunc(response.seqId);
                            break;
                    }
                }
            }
        };
    };

    /**
     * iOS接口调用器
     */
    var IOSAPICaller = function ()
    {
        var requestes = {};

        /**
         * 调用方法
         * @param request    请求信息
         */
        this.callMethod = function(request)
        {
            requestes[request.seqId] = request;
            window.location.href = "mobpush://call?seqId=" + request.seqId + "&methodName=" + request.method;
        };

        /**
         * 返回回调
         * @param response      回复数据
         *
         * response结构
         * {
         *   "seqId" : "111111",
         *   "extra" : "sss",
         *   "content" : "sww",
         *   "callback" : "function string",
         *   "messageId" : "32353",
         *   "timeStamp" :  2313498473213,
         *   "errorCode" : 400,
         *   "errorMsg" : "error",
         * }
         */
        this.callback = function (response)
        {
            if (response.callback)
            {
                var callbackFunc = eval(response.callback);
                if (callbackFunc)
                {
                    var method = response.method;
                    switch (method)
                    {
                        case MobPushMethodName.SendCustomMsg:
                            callbackFunc(response.seqId, response.content, response.messageId);
                            break;
                        case MobPushMethodName.SendAPNsMsg:
                            callbackFunc(response.seqId, response.content, response.mobpushMessageId);
                            break;
                        case MobPushMethodName.SendLocalNotify:
                            callbackFunc(response.seqId, response.content, response.title, response.subtitle, response.badge);
                            break;
                        case MobPushMethodName.GetRegistrationID:
                            callbackFunc(response.seqId, response.registrationID, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.SetAlias:
                            callbackFunc(response.seqId, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.GetAlias:
                            callbackFunc(response.seqId, response.alias, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.DeleteAlias:
                            callbackFunc(response.seqId, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.AddTags:
                            callbackFunc(response.seqId, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.GetTags:
                            callbackFunc(response.seqId, response.tags, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.DeleteTags:
                            callbackFunc(response.seqId, response.errorCode, response.errorMsg);
                            break;
                        case MobPushMethodName.CleanAllTags:
                            callbackFunc(response.seqId, response.errorCode, response.errorMsg);
                            break;

                    }
                }
            }
        };

        this.getParams = function (seqId)
        {
            var paramsStr = null;
            var request = requestes[seqId];

            if (request && request.params)
            {
                paramsStr = ObjectToJsonString(request.params);
            }

            requestes[seqId] = null;
            delete requestes[seqId];
            return paramsStr;
        };
    };

    /**
     * 推送环境
     * @type {object}
     */
    this.PushEnvironment = {
        Debug : 0,
        Release : 1,
    };

    /**
     * 消息发送类型
     * @type {object}
     */
    this.SendMsgType = {
        apns : 1,  //推送
        socket : 2, //应用内消息
        timed : 3, //定时消息
        local : 4, //本地通知
    };

    /**
     * 初始化MobPush.js (由系统调用)
     * @param platform  平台类型，1 安卓 2 iOS
     * @private
     */
    this.initMobPushJS = function (platform)
    {
        switch (platform)
        {
            case 1:
                jsLog = {
                log: function(msg) {
                    window.JSInterface.jsLog(msg);
                }
                };
                if(isDebug) {
                    jsLog.log("found platform type: Android");
                }
                apiCaller = new AndroidAPICaller();
                break;
            case 2:
                jsLog = {
                log: function(msg) {
                }
                };

                apiCaller = new IOSAPICaller();
                break;
        }

        //派发回调
        for (var i = 0; i < initCallbackFuncs.length; i++)
        {
            var obj = initCallbackFuncs[i];
            obj.callback (obj.method, obj.params);
        }
        initCallbackFuncs.splice(0);
    };

    /**
     * 检测是否已经初始化
     * @param callback  回调方法
     * @private
     */
    var CheckInit = function (method, params, callback)
    {
        if (apiCaller == null)
        {
            initCallbackFuncs.push({
                                   "method" : method,
                                   "params" : params,
                                   "callback" : callback
                                   });

            if (!isSendInitRequest)
            {
                window.location.href = "mobpush://init";
                isSendInitRequest = true;
            }
        }
        else
        {
            if (callback)
            {
                callback (method, params);
            }
        }
    };

    /**
     * 调用方法
     * @param method        方法
     * @param params        参数
     * @private
     */
    var CallMethod = function (method, params)
    {
        CheckInit(method, params, function (method, params) {
                  seqId ++;
                  var req = new RequestInfo(seqId, method, params);

                  if (firstRequest == null)
                  {
                  firstRequest = req;
                  lastRequest = firstRequest;
                  }
                  else
                  {
                  lastRequest.nextRequest = req;
                  lastRequest = req;
                  }

                  SendRequest();
                  });
        return seqId;
    };

    /**
     * 发送请求
     * @private
     */
    var SendRequest = function ()
    {
        if (!isRunning && firstRequest)
        {
            isRunning = true;
            apiCaller.callMethod(firstRequest);

            setTimeout(function(){

                       isRunning = false;
                       //直接发送下一个请求
                       NextRequest();
                       SendRequest();

                       }, 50);
        }
    };

    /**
     * 下一个请求
     * @private
     */
    var NextRequest = function ()
    {
        if (firstRequest == lastRequest)
        {
            firstRequest = null;
            lastRequest = null;
            isRunning = false;
        }
        else
        {
            firstRequest = firstRequest.nextRequest;
        }
    };


    /**
     * 回调方法 (由系统调用)
     * @param response  回复数据
     * @private
     */
    this.callback = function (response)
    {
        apiCaller.callback(response);
    };

    /**
     * 获取参数
     * @param seqId
     * @returns {*}
     * @private
     */
    this.getParams = function (seqId)
    {
        return apiCaller.getParams(seqId);
    };

    /**
     * 初始化MobPushSDK
     * @param pushConfig            配置信息
     */
    this.initMobPushSDK = function (pushConfig)
    {
        var params =
        {
            "pushConfig" : pushConfig
        };
        CallMethod(MobPushMethodName.InitMobPushSDK, params);
    };

    /**
     * 发送消息 仅供demo使用
     * @param msgParams
     * @param callback
     */
    this.sendCustomMsg = function (msgParams, callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };
        

        CallMethod(MobPushMethodName.SendCustomMsg, params);
    };

    /**
     * apns消息推送 仅供demo使用
     * @param msgParams
     * @param callback
     */
    this.sendAPNsMsg = function (msgParams, callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.SendAPNsMsg, params);
    };


    /**
     * 发送本地通知 仅供demo使用
     * @param msgParams
     * @param callback
     */
    this.sendLocalNotify = function (msgParams, callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.SendLocalNotify, params);
    };

    /**
     * 获取注册id（可与用户id绑定，实现向指定用户推送消息）
     * @param callback
     */
    this.getRegistrationID = function (callback)
    {
        var params =
        {
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.GetRegistrationID, params);
    };

    /**
     * 设置别名
     * @param msgParams : {"alias" : "我的别名"}
     * @param callback
     */
    this.setAlias = function (msgParams, callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };
        CallMethod(MobPushMethodName.SetAlias, params);
    };

    /**
     * 获取别名
     * @param callback
     */
    this.getAlias = function (callback)
    {
        var params =
        {
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.GetAlias, params);
    };

    /**
     * 删除别名
     * @param callback
     */
    this.deleteAlias = function (callback)
    {
        var params =
        {
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.DeleteAlias, params);
    };

    /**
     * 添加标签
     * @param msgParams : {"tags" : ['a','b','c'],}
     * @param callback
     */
    this.addTags = function (msgParams, callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.AddTags, params);
    };

    /**
     * 获取所有标签
     * @param callback
     */
    this.getTags = function (callback)
    {
        var params =
        {
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.GetTags, params);
    };

    /**
     * 删除标签
     * @param msgParams : {"tags" : ['a','b'],}
     * @param callback
     */
    this.deleteTags = function (msgParams,callback)
    {
        var params =
        {
            "msgParams" : msgParams,
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.DeleteTags, params);
    };

    /**
     * 清空所有标签
     * @param callback
     */
    this.cleanAllTags = function(callback)
    {
        var params =
        {
            "callback" : "(" + callback.toString() + ")"
        };

        CallMethod(MobPushMethodName.CleanAllTags, params);
    };

    /**
     * 清空所有标签(仅供android端使用，ios请忽略)
     * @param callback
     */
    this.addPushReceiver = function()
    {
        var params = {};

        CallMethod(MobPushMethodName.AddPushReceiver, params);
    };

    /**
     * 增加外部回调
     * @param cb
     */
    this.setMsgCB = function(cb) {
        onMsgCB = cb;
        if (onMsgBody) {
            onMsgCB(onMsgBody);
        }
    };

    /**
     * 添加MobPush推送接收监听(仅供android端使用，ios请忽略)
     * @param body
     * body: {"action":0,"result":{}}
     * action=0(透传),1(通知),2(点击打开通知),3(tags),4(alias)
     * action=0,result :{"messageId":"","content":"","extrasMap":{},"timestamp":""}
     * action=1,result :{"messageId":"","content":"","title":"","style":"","styleContent":"","extrasMap":{},"timestamp":"","inboxStyleContent":"","channel":""}
     * action=2,result :{"messageId":"","content":"","title":"","style":"","styleContent":"","extrasMap":{},"timestamp":"","inboxStyleContent":"","channel":""}
     * action=3,result :{"tags":"","operation":"","errorCode":""} operation 操作说明（0 获取， 1 设置， 2 删除，3 清空）,errorCode 操作结果（0 成功，其他失败，见{@link MobPushErrorCode}）
     * action=4,result :{"alias":"","operation":"","errorCode":""} operation 操作说明（0 获取， 1 设置， 2 删除）,errorCode 操作结果（0 成功，其他失败，见{@link MobPushErrorCode}）
     */
    this.onMessageCallBack = function(body){
        alert(body.action==0?"接收到透传信息"
        :(body.action ==1?"接收到通知"
        :(body.action == 2?"接收到通知点击打开"
        :(body.action == 3?"接收到标签操作"
        :"接收到别名操作"))));
        if(body.action ==0){
        //接收到透传信息回调
            alert(JSON.stringify(body.result));
        } else if(body.action ==1){
        //接收到通知回调
            alert(JSON.stringify(body.result));
        } else if(body.action ==2){
        //接收到通知点击打开回调
            alert(JSON.stringify(body.result));
        } else if(body.action ==3){
        //接收到标签操作回调
            alert(JSON.stringify(body.result));
        } else if(body.action ==4){
        //接收到别名操作回调
            alert(JSON.stringify(body.result));
        }

        /**
         *
         */
        if (onMsgCB) {
            onMsgCB(body);
        } else {
            onMsgBody = body;
        }
    }

    /**
     * JSON字符串转换为对象
     * @param string        JSON字符串
     * @returns {Object}    转换后对象
     */
    var JsonStringToObject = function (string)
    {
        try
        {
            return eval("(" + string + ")");
        }
        catch (err)
        {
            return null;
        }
    };

    /**
     * 对象转JSON字符串
     * @param obj           对象
     * @returns {string}    JSON字符串
     */
    var ObjectToJsonString = function (obj)
    {
        var S = [];
        var J = null;

        var type = Object.prototype.toString.apply(obj);

        if (type === '[object Array]')
        {
            for (var i = 0; i < obj.length; i++)
            {
                S.push(ObjectToJsonString(obj[i]));
            }
            J = '[' + S.join(',') + ']';
        }
        else if (type === '[object Date]')
        {
            J = "new Date(" + obj.getTime() + ")";
        }
        else if (type === '[object RegExp]'
                 || type === '[object Function]')
        {
            J = obj.toString();
        }
        else if (type === '[object Object]')
        {
            for (var key in obj)
            {
                var value = ObjectToJsonString(obj[key]);
                if (value != null)
                {
                    S.push('"' + key + '":' + value);
                }
            }
            J = '{' + S.join(',') + '}';
        }
        else if (type === '[object String]')
        {
            J = '"' + obj.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '') + '"';
                                                         }
                                                         else if (type === '[object Number]')
                                                         {
                                                         J = obj;
                                                         }
                                                         else if (type === '[object Boolean]')
                                                         {
                                                         J = obj;
                                                         }

                                                         return J;
                                                         };

                                                         };

var $mobpush = new MobPush();
