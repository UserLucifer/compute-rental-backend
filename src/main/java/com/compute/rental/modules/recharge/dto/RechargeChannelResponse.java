package com.compute.rental.modules.recharge.dto;

public record RechargeChannelResponse(
        Long channelId,
        String channelCode,
        String channelName,
        String network,
        String displayUrl,
        String accountName,
        String accountNo,
        Integer sortNo,
        String locale,
        String requestedLocale,
        Boolean localeFallback
) {
}
