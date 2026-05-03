package com.compute.rental.modules.system.service;

import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.system.entity.SysAdminLog;
import com.compute.rental.modules.system.mapper.SysAdminLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminLogService {

    public static final String ADMIN_LOGIN_SUCCESS = "ADMIN_LOGIN_SUCCESS";
    public static final String ADMIN_LOGIN_FAIL = "ADMIN_LOGIN_FAIL";
    public static final String ADMIN_LOGOUT = "ADMIN_LOGOUT";
    public static final String UPDATE_SYS_CONFIG = "UPDATE_SYS_CONFIG";
    public static final String RUN_SCHEDULER = "RUN_SCHEDULER";
    public static final String CLEAR_REDIS_CACHE = "CLEAR_REDIS_CACHE";

    public static String actionName(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case ADMIN_LOGIN_SUCCESS -> "\u7ba1\u7406\u5458\u767b\u5f55\u6210\u529f";
            case ADMIN_LOGIN_FAIL -> "\u7ba1\u7406\u5458\u767b\u5f55\u5931\u8d25";
            case ADMIN_LOGOUT -> "\u7ba1\u7406\u5458\u9000\u51fa\u767b\u5f55";
            case UPDATE_SYS_CONFIG -> "\u66f4\u65b0\u7cfb\u7edf\u914d\u7f6e";
            case RUN_SCHEDULER -> "\u6267\u884c\u5b9a\u65f6\u4efb\u52a1";
            case CLEAR_REDIS_CACHE -> "\u6e05\u7406 Redis \u7f13\u5b58";
            default -> action;
        };
    }

    private final SysAdminLogMapper sysAdminLogMapper;

    public AdminLogService(SysAdminLogMapper sysAdminLogMapper) {
        this.sysAdminLogMapper = sysAdminLogMapper;
    }

    public void log(Long adminId, String action, String targetTable, Long targetId,
                    String beforeValue, String afterValue, String remark, String ip) {
        if (adminId == null) {
            return;
        }
        var log = new SysAdminLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetTable(targetTable);
        log.setTargetId(targetId);
        log.setBeforeValue(beforeValue);
        log.setAfterValue(afterValue);
        log.setRemark(trim(remark, 255));
        log.setIp(trim(ip, 64));
        log.setCreatedAt(DateTimeUtils.now());
        sysAdminLogMapper.insert(log);
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        var realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
