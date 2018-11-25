package com.zs.easy.mqtt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/24 16:57
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：Mqtt管理器
 */

public class EasyMqttManage {
    private final String TAG = "EasyMqttService";

    private MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    Handler handler;

    /**
     * 回调集合
     */
    private List<IEasyMqttCallBack> starMQTTCallBacks = new ArrayList<>();
    private Builder builder;

    /**
     * builder设计模式
     *
     * @param builder
     */
    private EasyMqttManage(Builder builder) {
        this.builder = builder.clone();
        init();
    }

    /**
     * Builder 构造类
     */
    public static final class Builder implements Cloneable {


        private Context context;
        /**
         * 服务器地址
         */
        private String serverUrl;
        /**
         * 用户名
         */
        private String userName;
        /**
         * 密码
         */
        private String passWord;
        /**
         * 客户端id
         */
        private String clientId;
        /**
         * 是否根据clientId，后面加上随机字符串，实现动态id
         * <p>
         * 如果true那么重连时候会重新生成MqttAndroidClient
         */
        private boolean isDynamicClientId = false;
        /**
         * 超时时间
         */
        private int timeOut = 10;
        /**
         * 心跳间隔
         */
        private int keepAliveInterval = 20;
        /**
         * 要保留最后的断开连接信息
         */
        private boolean retained = true;
        /**
         * 清空连接Session缓存
         * 如果需要保持某个会话长时间保存,这里为false
         * 默认开启，每断一次，就清除这个链接，方便后台管理。
         */
        private boolean cleanSession = true;
        /**
         * 自动重连(mqtt内部的),默认关闭
         * 自动重连是默认关闭的，设置开启后，会在掉线的情况下每隔1秒请求一次；
         */
        private boolean autoReconnect = false;
        /**
         * 自动重连(本工具的),不要和autoReconnect同时设置
         * 开启后会在回调失败的地方进行重新连接
         */
        public boolean isReConnect = true;
        /**
         * 重连的时间间隔(对应isReConnect)
         */
        private int reConnectTime = 5000;

        @Override
        public Builder clone() {
            try {
                Builder builder = (Builder) super.clone();
                builder.context = context;
                builder.serverUrl = serverUrl;
                builder.userName = userName;
                builder.passWord = passWord;
                builder.clientId = clientId;
                builder.timeOut = timeOut;
                builder.keepAliveInterval = keepAliveInterval;
                builder.retained = retained;
                builder.cleanSession = cleanSession;
                builder.autoReconnect = autoReconnect;
                return builder;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder passWord(String passWord) {
            this.passWord = passWord;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder dynamicClientId(boolean dynamicClientId) {
            this.isDynamicClientId = dynamicClientId;
            return this;
        }

        public Builder timeOut(int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public Builder keepAliveInterval(int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder retained(boolean retained) {
            this.retained = retained;
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public Builder reConnect(boolean isReConnect) {
            this.isReConnect = isReConnect;
            return this;
        }

        public Builder reConnectTime(int reConnectTime) {
            this.reConnectTime = reConnectTime;
            return this;
        }

        public EasyMqttManage bulid(Context context) {
            this.context = context;
            return new EasyMqttManage(this);
        }


    }

    /**
     * 发布消息
     *
     * @param msg
     * @param topic
     * @param qos   // 0 表示只会发送一次推送消息 收到不收到都不关心
     *              // 1 保证能收到消息，但不一定只收到一条
     *              // 2 保证收到切只能收到一条消息
     */
    public void publish(String msg, String topic, int qos) {
        try {
            client.publish(topic, msg.getBytes(), qos, builder.retained);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        setClient();
        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(builder.cleanSession);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(builder.timeOut);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(builder.keepAliveInterval);
        // 用户名
        conOpt.setUserName(builder.userName);
        // 密码
        conOpt.setPassword(builder.passWord.toCharArray());
        conOpt.setAutomaticReconnect(builder.autoReconnect);
        //定时器
        handler = new Handler(Looper.myLooper());
    }

    private void setClient() {
        // 服务器地址（协议+地址+端口号）
        client = new MqttAndroidClient(builder.context, builder.serverUrl, getClientId());
        // 设置MQTT监听并且接受消息
        client.setCallback(mqttCallback);
    }

    /**
     * 获取一个clientId
     *
     * @return
     */
    private String getClientId() {
        String result = builder.clientId;
        if (result == null) {
            result = "";
        }
        if (builder.isDynamicClientId) {
            result += System.currentTimeMillis();
        }
        //最大长度22
        if (result.length() > 20) {
            result = result.substring(0, 20);
        }
        return result;
    }

    /**
     * 关闭客户端
     */
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 连接MQTT服务器
     */
    public void connect() {
        if (!client.isConnected()) {
            try {
                client.connect(conOpt, null, iMqttActionListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加一个监听
     *
     * @param starMQTTCallBack
     */
    public void addCallback(IEasyMqttCallBack starMQTTCallBack) {
        if (starMQTTCallBack != null && !starMQTTCallBacks.contains(starMQTTCallBack)) {
            starMQTTCallBacks.add(starMQTTCallBack);
        }
    }

    /**
     * 订阅主题
     *
     * @param topic 主题
     * @param qos   策略
     */
    public void subscribe(String topic, int qos) {
        try {
            // 订阅topic话题
            client.subscribe(topic, qos);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 订阅主题
     *
     * @param topics 主题
     * @param qos    策略
     */
    public void subscribe(String[] topics, int[] qos) {
        try {
            // 订阅topic话题
            client.subscribe(topics, qos);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            client.disconnect();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * 判断连接是否断开
     */
    public boolean isConnected() {
        try {
            return client.isConnected();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return false;
    }

    /**
     * MQTT是否连接成功
     */
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "mqtt connect success ");
            if (starMQTTCallBacks != null) {
                for (IEasyMqttCallBack c : starMQTTCallBacks) {
                    c.connectSuccess(arg0);
                }
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            Log.e(TAG, "mqtt connect failed " + arg1.toString());
            if (starMQTTCallBacks != null) {
                for (IEasyMqttCallBack c : starMQTTCallBacks) {
                    c.connectFailed(arg0, arg1);
                }
            }
            reConnect();
        }
    };

    /**
     * MQTT监听并且接受消息
     */
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {

            String msgContent = new String(message.getPayload());
            String detailLog = topic + ";qos:" + message.getQos() + ";retained:" + message.isRetained();
            Log.i(TAG, "messageArrived:" + msgContent);
            Log.i(TAG, detailLog);
            if (starMQTTCallBacks != null) {
                for (IEasyMqttCallBack c : starMQTTCallBacks) {
                    c.messageArrived(topic, msgContent, message.getQos(), message);
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            Log.i(TAG, "deliveryComplete");
            if (starMQTTCallBacks != null) {
                for (IEasyMqttCallBack c : starMQTTCallBacks) {
                    c.deliveryComplete(arg0);
                }
            }
        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.e(TAG, "connectionLost");
            // 失去连接，重连,这里最好记录日志
            reConnect();
            if (starMQTTCallBacks != null) {
                for (IEasyMqttCallBack c : starMQTTCallBacks) {
                    c.connectionLost(arg0);
                }
            }
        }
    };

    /**
     * 失去连接，重连
     * 5秒重连
     *
     * @return
     */
    private void reConnect() {
        if (builder.isReConnect) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (builder.isDynamicClientId) {
                            setClient();
                        }
                        client.connect(conOpt, null, iMqttActionListener);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, builder.reConnectTime);
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) builder.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "MQTT current network name：" + name);
            return true;
        } else {
            Log.i(TAG, "MQTT no network");
            return false;
        }
    }
}