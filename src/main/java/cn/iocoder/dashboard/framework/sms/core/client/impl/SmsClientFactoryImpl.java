package cn.iocoder.dashboard.framework.sms.core.client.impl;

import cn.iocoder.dashboard.framework.sms.core.client.SmsClient;
import cn.iocoder.dashboard.framework.sms.core.client.SmsClientFactory;
import cn.iocoder.dashboard.framework.sms.core.client.impl.aliyun.AliyunSmsClient;
import cn.iocoder.dashboard.framework.sms.core.client.impl.yunpian.YunpianSmsClient;
import cn.iocoder.dashboard.framework.sms.core.enums.SmsChannelEnum;
import cn.iocoder.dashboard.framework.sms.core.property.SmsChannelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 短信客户端工厂接口
 *
 * @author zzf
 */
@Validated
@Slf4j
public class SmsClientFactoryImpl implements SmsClientFactory {

    /**
     * 短信客户端 Map
     * key：渠道编号，使用 {@link SmsChannelProperties#getId()}
     */
    private final ConcurrentMap<Long, AbstractSmsClient> channelIdClients = new ConcurrentHashMap<>();

    /**
     * 短信客户端 Map
     * key：渠道编码，使用 {@link SmsChannelProperties#getCode()} ()}
     *
     * 注意，一些场景下，需要获得某个渠道类型的客户端，所以需要使用它。
     * 例如说，解析短信接收结果，是相对通用的，不需要使用某个渠道编号的 {@link #channelIdClients}
     */
    private final ConcurrentMap<String, AbstractSmsClient> channelCodeClients = new ConcurrentHashMap<>();

    public SmsClientFactoryImpl() {
        // 初始化 channelCodeClients 集合
        Arrays.stream(SmsChannelEnum.values()).forEach(channel -> {
            // 创建一个空的 SmsChannelProperties 对象
            SmsChannelProperties properties = new SmsChannelProperties().setCode(channel.getCode())
                    .setApiKey("default").setApiSecret("default");
            // 创建 Sms 客户端
            AbstractSmsClient smsClient = createSmsClient(properties);
            channelCodeClients.put(channel.getCode(), smsClient);
        });
    }

    @Override
    public SmsClient getSmsClient(Long channelId) {
        return channelIdClients.get(channelId);
    }

    @Override
    public SmsClient getSmsClient(String channelCode) {
        return channelCodeClients.get(channelCode);
    }

    @Override
    public void createOrUpdateSmsClient(SmsChannelProperties properties) {
        AbstractSmsClient client = channelIdClients.get(properties.getId());
        if (client == null) {
            client = this.createSmsClient(properties);
            client.init();
            channelIdClients.put(client.getId(), client);
        } else {
            client.refresh(properties);
        }
    }

    private AbstractSmsClient createSmsClient(SmsChannelProperties properties) {
        SmsChannelEnum channelEnum = SmsChannelEnum.getByCode(properties.getCode());
        Assert.notNull(channelEnum, String.format("渠道类型(%s) 为空", channelEnum));
        // 创建客户端
        switch (channelEnum) {
            case ALIYUN:
                return new AliyunSmsClient(properties);
            case YUN_PIAN:
                return new YunpianSmsClient(properties);
        }
        // 创建失败，错误日志 + 抛出异常
        log.error("[createSmsClient][配置({}) 找不到合适的客户端实现]", properties);
        throw new IllegalArgumentException(String.format("配置(%s) 找不到合适的客户端实现", properties));
    }

//    /**
//     * 从短信发送回调函数请求中获取用于唯一确定一条send_lod的apiId
//     *
//     * @param callbackRequest 短信发送回调函数请求
//     * @return 第三方平台短信唯一标识
//     */
//    public SmsResultDetail getSmsResultDetailFromCallbackQuery(ServletRequest callbackRequest) {
//        for (Long channelId : clients.keySet()) {
//            AbstractSmsClient smsClient = clients.get(channelId);
//            try {
//                SmsResultDetail smsSendResult = smsClient.smsSendCallbackHandle(callbackRequest);
//                if (smsSendResult != null) {
//                    return smsSendResult;
//                }
//            } catch (Exception ignored) {
//            }
//        }
//        throw new IllegalArgumentException("getSmsResultDetailFromCallbackQuery fail! don't match SmsClient by RequestParam: "
//                + JsonUtils.toJsonString(callbackRequest.getParameterMap()));
//    }

}
