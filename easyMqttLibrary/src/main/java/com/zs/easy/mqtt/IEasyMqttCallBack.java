package com.zs.easy.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author　　: 李坤
 * 创建时间: 2018/11/24 17:09
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：mqtt的整个回调
 */
public interface IEasyMqttCallBack {

    /**
     * 收到消息
     *
     * @param topic        主题
     * @param message      消息内容
     * @param qos          消息策略
     * @param orginMessage 整个消息体 messageId用来保证该条消息的唯一性，可做去重判断
     */
    void messageArrived(String topic, String message, int qos, MqttMessage orginMessage);

    /**
     * 连接断开
     * 这里如果没有设置自动重连（默认没有），那么要在这里重新连接
     *
     * @param throwable 抛出的异常信息
     */
    void connectionLost(Throwable throwable);

    /**
     * 传送完成
     *
     * @param token
     */
    void deliveryComplete(IMqttDeliveryToken token);

    /**
     * 连接成功
     *
     * @param token
     */
    void connectSuccess(IMqttToken token);

    /**
     * 连接失败
     *
     * @param token
     */
    void connectFailed(IMqttToken token, Throwable throwable);

}
