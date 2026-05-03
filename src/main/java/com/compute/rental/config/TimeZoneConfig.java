package com.compute.rental.config;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    private final ZoneId zoneId;

    public TimeZoneConfig(@Value("${app.time-zone:Asia/Shanghai}") String zoneId) {
        this.zoneId = ZoneId.of(zoneId);
    }

    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }
}
