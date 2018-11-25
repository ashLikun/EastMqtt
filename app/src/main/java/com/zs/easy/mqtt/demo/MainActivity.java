package com.zs.easy.mqtt.demo;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;

import com.zs.easy.mqtt.EasyMqttManage;
import com.zs.easy.mqtt.IEasyMqttCallBack;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author zhangshun
 */
public class MainActivity extends Activity {
    private StringBuilder messageSb = new StringBuilder();

    private EasyMqttManage mqttManage;
    /**
     * 回调时使用
     */
    private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    TextView textView;
    TextView textViewTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textViewTopic = findViewById(R.id.textViewTopic);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }
        buildEasyMqttService();

        connect();

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();

            }
        });

    }

    private void sendMessage() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mqttManage.publish("我是李坤2,这是第几条" + count, "likun", 2);
                count++;
                if (count >= 10000) {
                    return;
                }
               // sendMessage();
            }
        }, 50);
    }

    int count = 1;
    Handler handler = new Handler();


    /**
     * 断开连接
     */
    private void disconnect() {
        mqttManage.disconnect();
    }

    /**
     * 关闭连接
     */
    private void close() {
        mqttManage.close();
    }

    /**
     * 订阅主题 这里订阅三个主题分别是"a", "b", "c"
     */
    private void subscribe() {
        mqttManage.subscribe("zhaoyang", 2);
    }

    /**
     * 连接Mqtt服务器
     */
    private void connect() {
        mqttManage.connect();
    }

    /**
     * 构建EasyMqttService对象
     */
    private void buildEasyMqttService() {
        mqttManage = new EasyMqttManage.Builder()
                //设置自动重连
                .reConnect(true)
                //设置不清除回话session 可收到服务器之前发出的推送消息
                .cleanSession(false)
                //唯一标示 保证每个设备都唯一就可以 建议 imei
                .clientId("android")
                .dynamicClientId(false)
                //mqtt服务器地址 格式例如：tcp://10.0.261.159:1883
                .serverUrl("tcp://47.101.58.148:61613")
                //心跳包默认的发送间隔
                .keepAliveInterval(20)
                .userName("admin")
                .passWord("password")
                //构建出EasyMqttService 建议用application的context
                .bulid(this.getApplicationContext());

        mqttManage.addCallback(new IEasyMqttCallBack() {
            @Override
            public void messageArrived(String topic, String message, int qos, MqttMessage orginMessage) {
                //推送消息到达
                messageSb.append(message);
                messageSb.append("\n");
                textView.setText(messageSb);
                textViewTopic.setText("topic 为 ：" + topic);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                //连接断开
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }

            @Override
            public void connectSuccess(IMqttToken token) {
                //连接成功
                subscribe();
            }

            @Override
            public void connectFailed(IMqttToken token, Throwable throwable) {
                //连接失败
            }
        });
    }
}
