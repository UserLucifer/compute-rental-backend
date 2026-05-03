package com.compute.rental.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.system.dto.AdminLoginRequest;
import com.compute.rental.modules.system.dto.AdminLoginResponse;
import com.compute.rental.modules.system.dto.AdminListResponse;
import com.compute.rental.modules.system.dto.AdminMeResponse;
import com.compute.rental.modules.system.dto.AdminRegisterRequest;
import com.compute.rental.common.page.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.modules.system.entity.SysAdmin;
import com.compute.rental.modules.system.mapper.SysAdminMapper;
import com.compute.rental.security.IdentityType;
import com.compute.rental.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final SysAdminMapper sysAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminLogService adminLogService;

    public AdminAuthService(
            SysAdminMapper sysAdminMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AdminLogService adminLogService
    ) {
        this.sysAdminMapper = sysAdminMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminLogService = adminLogService;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, String ip) {
        var userName = request.userName().trim();
        var admin = sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUserName, userName)
                .last("LIMIT 1"));
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_BAD_CREDENTIALS);
        }
        if (!Integer.valueOf(CommonStatus.ENABLED.value()).equals(admin.getStatus())) {
            adminLogService.log(admin.getId(), AdminLogService.ADMIN_LOGIN_FAIL, "sys_admin", admin.getId(),
                    null, null, "Admin disabled", ip);
            throw new BusinessException(ErrorCode.ADMIN_DISABLED);
        }
        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            adminLogService.log(admin.getId(), AdminLogService.ADMIN_LOGIN_FAIL, "sys_admin", admin.getId(),
                    null, null, "Bad credentials", ip);
            throw new BusinessException(ErrorCode.ADMIN_BAD_CREDENTIALS);
        }

        var now = DateTimeUtils.now();
        sysAdminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .eq(SysAdmin::getId, admin.getId())
                .set(SysAdmin::getLastLoginAt, now)
                .set(SysAdmin::getUpdatedAt, now));
        admin.setLastLoginAt(now);
        adminLogService.log(admin.getId(), AdminLogService.ADMIN_LOGIN_SUCCESS, "sys_admin", admin.getId(),
                null, null, "Admin login success", ip);
        var token = jwtTokenProvider.createAccessToken(
                admin.getId(),
                admin.getUserName(),
                admin.getRole(),
                IdentityType.ADMIN
        );
        return new AdminLoginResponse(token, toMe(admin));
    }

    public AdminMeResponse me(Long adminId) {
        var admin = requireEnabledAdmin(adminId);
        return toMe(admin);
    }

    public void logout(Long adminId, String ip) {
        adminLogService.log(adminId, AdminLogService.ADMIN_LOGOUT, "sys_admin", adminId,
                null, null, "TODO token blacklist is not implemented", ip);
    }

    public PageResult<AdminListResponse> pageAdmins(long pageNo, long pageSize, String userName, String role, Integer status) {
        var page = new Page<SysAdmin>(pageNo, pageSize);
        var wrapper = new LambdaQueryWrapper<SysAdmin>()
                .like(userName != null && !userName.isBlank(), SysAdmin::getUserName, userName)
                .eq(role != null && !role.isBlank(), SysAdmin::getRole, role)
                .eq(status != null, SysAdmin::getStatus, status)
                .orderByDesc(SysAdmin::getId);
        var result = sysAdminMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toListResponse).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void register(AdminRegisterRequest request, Long currentAdminId, String ip) {
        // Check if exists
        var existing = sysAdminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUserName, request.userName().trim())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(ErrorCode.ADMIN_USERNAME_EXISTS);
        }

        var admin = new SysAdmin();
        admin.setUserName(request.userName().trim());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(request.role());
        admin.setStatus(1); // Enabled by default
        admin.setCreatedBy(currentAdminId);
        admin.setCreatedAt(DateTimeUtils.now());
        admin.setUpdatedAt(DateTimeUtils.now());
        sysAdminMapper.insert(admin);

        adminLogService.log(currentAdminId, "CREATE_ADMIN", "sys_admin", admin.getId(),
                null, "role=" + request.role(), "Created new administrator: " + request.userName(), ip);
    }

    private SysAdmin requireEnabledAdmin(Long adminId) {
        var admin = sysAdminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonStatus.ENABLED.value()).equals(admin.getStatus())) {
            throw new BusinessException(ErrorCode.ADMIN_DISABLED);
        }
        return admin;
    }

    private AdminMeResponse toMe(SysAdmin admin) {
        return new AdminMeResponse(
                admin.getId(),
                admin.getUserName(),
                admin.getStatus(),
                admin.getRole()
        );
    }

    private AdminListResponse toListResponse(SysAdmin admin) {
        return new AdminListResponse(
                admin.getId(),
                admin.getUserName(),
                admin.getStatus(),
                admin.getRole(),
                admin.getLastLoginAt(),
                admin.getCreatedAt()
        );
    }
}
