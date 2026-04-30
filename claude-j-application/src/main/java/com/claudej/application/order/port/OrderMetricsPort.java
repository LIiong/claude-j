package com.claudej.application.order.port;

/**
 * 订单指标采集端口
 */
public interface OrderMetricsPort {

    void recordCreateOrderSuccess(String source);

    void recordCreateOrderFailure(String source, String reasonType);

    void recordCreateOrderDuration(String source, String outcome, long nanos);
}
