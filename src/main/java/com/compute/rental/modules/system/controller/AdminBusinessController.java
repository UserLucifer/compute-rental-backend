package com.compute.rental.modules.system.controller;

import com.compute.rental.common.api.ApiResponse;
import com.compute.rental.common.page.PageResult;
import com.compute.rental.modules.system.dto.AdminApiCredentialResponse;
import com.compute.rental.modules.system.dto.AdminApiDeployOrderResponse;
import com.compute.rental.modules.system.dto.AdminCommissionRecordResponse;
import com.compute.rental.modules.system.dto.AdminLogResponse;
import com.compute.rental.modules.system.dto.AdminProfitRecordResponse;
import com.compute.rental.modules.system.dto.AdminRentalOrderDetailResponse;
import com.compute.rental.modules.system.dto.AdminRentalOrderResponse;
import com.compute.rental.modules.system.dto.AdminSettlementOrderResponse;
import com.compute.rental.modules.system.dto.AdminTeamLeaderboardRow;
import com.compute.rental.modules.system.dto.AdminTeamListRow;
import com.compute.rental.modules.system.dto.AdminTeamMemberRow;
import com.compute.rental.modules.system.dto.AdminTeamOverviewResponse;
import com.compute.rental.modules.system.dto.AdminTeamRelationResponse;
import com.compute.rental.modules.system.dto.AdminTeamTreeNode;
import com.compute.rental.modules.system.dto.AdminTeamUserSummaryResponse;
import com.compute.rental.modules.system.dto.AdminUserResponse;
import com.compute.rental.modules.system.dto.AdminUserTeamResponse;
import com.compute.rental.modules.system.dto.AdminWalletResponse;
import com.compute.rental.modules.system.dto.AdminWalletTransactionResponse;
import com.compute.rental.modules.system.service.AdminBusinessQueryService;
import com.compute.rental.modules.system.service.AdminLogService;
import com.compute.rental.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Business")
@RestController
@RequestMapping("/api/admin")
public class AdminBusinessController {

    private final AdminBusinessQueryService adminBusinessQueryService;
    private final AdminLogService adminLogService;

    public AdminBusinessController(
            AdminBusinessQueryService adminBusinessQueryService,
            AdminLogService adminLogService
    ) {
        this.adminBusinessQueryService = adminBusinessQueryService;
        this.adminLogService = adminLogService;
    }

    @Operation(summary = "Admin users")
    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserResponse>> users(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, name = "user_id") String userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageUsers(pageNo, pageSize, keyword, email, userId, status,
                startTime, endTime));
    }

    @Operation(summary = "Admin user detail")
    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> user(@PathVariable Long userId) {
        return ApiResponse.success(adminBusinessQueryService.getUser(userId));
    }

    @Operation(summary = "Disable user")
    @PostMapping("/users/{userId}/disable")
    public ApiResponse<AdminUserResponse> disableUser(@PathVariable Long userId, HttpServletRequest request) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminBusinessQueryService.disableUser(userId, admin.id(), adminLogService.clientIp(request)));
    }

    @Operation(summary = "Enable user")
    @PostMapping("/users/{userId}/enable")
    public ApiResponse<AdminUserResponse> enableUser(@PathVariable Long userId, HttpServletRequest request) {
        var admin = CurrentUser.requiredAdmin();
        return ApiResponse.success(adminBusinessQueryService.enableUser(userId, admin.id(), adminLogService.clientIp(request)));
    }

    @Operation(summary = "Admin wallets")
    @GetMapping("/wallets")
    public ApiResponse<PageResult<AdminWalletResponse>> wallets(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "用户搜索关键词，精确匹配用户编号/邮箱，前缀匹配用户名称/邮箱")
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "wallet_no") String walletNo
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageWallets(pageNo, pageSize, keyword, userId, walletNo));
    }

    @Operation(summary = "Admin wallet by user")
    @GetMapping("/wallets/{userId}")
    public ApiResponse<AdminWalletResponse> wallet(@PathVariable Long userId) {
        return ApiResponse.success(adminBusinessQueryService.getWalletByUser(userId));
    }

    @Operation(summary = "Admin wallet transactions")
    @GetMapping("/wallet-transactions")
    public ApiResponse<PageResult<AdminWalletTransactionResponse>> walletTransactions(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "用户搜索关键词，精确匹配用户编号/邮箱，前缀匹配用户名称/邮箱")
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "wallet_no") String walletNo,
            @RequestParam(required = false, name = "tx_type") String txType,
            @RequestParam(required = false, name = "biz_type") String bizType,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageWalletTransactions(pageNo, pageSize, keyword,
                userId, walletNo, txType, bizType, startTime, endTime));
    }

    @Operation(summary = "Admin rental orders")
    @GetMapping("/rental/orders")
    public ApiResponse<PageResult<AdminRentalOrderResponse>> rentalOrders(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "order_no") String orderNo,
            @RequestParam(required = false, name = "order_status") String orderStatus,
            @RequestParam(required = false, name = "profit_status") String profitStatus,
            @RequestParam(required = false, name = "settlement_status") String settlementStatus,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageRentalOrders(pageNo, pageSize, userId, orderNo,
                orderStatus, profitStatus, settlementStatus, startTime, endTime));
    }

    @Operation(summary = "Admin rental order detail")
    @GetMapping("/rental/orders/{orderNo}")
    public ApiResponse<AdminRentalOrderDetailResponse> rentalOrder(@PathVariable String orderNo) {
        return ApiResponse.success(adminBusinessQueryService.getRentalOrder(orderNo));
    }

    @Operation(summary = "Admin API credentials")
    @GetMapping("/api-credentials")
    public ApiResponse<PageResult<AdminApiCredentialResponse>> apiCredentials(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "credential_no") String credentialNo,
            @RequestParam(required = false, name = "token_status") String tokenStatus,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageApiCredentials(pageNo, pageSize, userId,
                credentialNo, tokenStatus, startTime, endTime));
    }

    @Operation(summary = "Admin API credential detail")
    @GetMapping("/api-credentials/{credentialNo}")
    public ApiResponse<AdminApiCredentialResponse> apiCredential(@PathVariable String credentialNo) {
        return ApiResponse.success(adminBusinessQueryService.getApiCredential(credentialNo));
    }

    @Operation(summary = "Admin API deploy orders")
    @GetMapping("/api-deploy-orders")
    public ApiResponse<PageResult<AdminApiDeployOrderResponse>> apiDeployOrders(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "deploy_no") String deployNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageApiDeployOrders(pageNo, pageSize, userId,
                deployNo, status, startTime, endTime));
    }

    @Operation(summary = "Admin API deploy order detail")
    @GetMapping("/api-deploy-orders/{deployNo}")
    public ApiResponse<AdminApiDeployOrderResponse> apiDeployOrder(@PathVariable String deployNo) {
        return ApiResponse.success(adminBusinessQueryService.getApiDeployOrder(deployNo));
    }

    @Operation(summary = "Admin profit records")
    @GetMapping("/profit/records")
    public ApiResponse<PageResult<AdminProfitRecordResponse>> profitRecords(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "order_no") String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "profit_date")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate profitDate,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageProfitRecords(pageNo, pageSize, keyword, userId, orderNo,
                status, profitDate, startTime, endTime));
    }

    @Operation(summary = "Admin profit record detail")
    @GetMapping("/profit/records/{profitNo}")
    public ApiResponse<AdminProfitRecordResponse> profitRecord(@PathVariable String profitNo) {
        return ApiResponse.success(adminBusinessQueryService.getProfitRecord(profitNo));
    }

    @Operation(summary = "Admin settlement orders")
    @GetMapping("/settlement/orders")
    public ApiResponse<PageResult<AdminSettlementOrderResponse>> settlementOrders(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "order_no") String orderNo,
            @RequestParam(required = false, name = "settlement_type") String settlementType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageSettlementOrders(pageNo, pageSize, userId,
                orderNo, settlementType, status, startTime, endTime));
    }

    @Operation(summary = "Admin settlement order detail")
    @GetMapping("/settlement/orders/{settlementNo}")
    public ApiResponse<AdminSettlementOrderResponse> settlementOrder(@PathVariable String settlementNo) {
        return ApiResponse.success(adminBusinessQueryService.getSettlementOrder(settlementNo));
    }

    @Operation(summary = "Admin commission records")
    @GetMapping("/commission/records")
    public ApiResponse<PageResult<AdminCommissionRecordResponse>> commissionRecords(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false, name = "order_no") String orderNo,
            @RequestParam(required = false, name = "level_no") Integer levelNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageCommissionRecords(pageNo, pageSize, userId,
                orderNo, levelNo, status, startTime, endTime));
    }

    @Operation(summary = "Admin commission record detail")
    @GetMapping("/commission/records/{commissionNo}")
    public ApiResponse<AdminCommissionRecordResponse> commissionRecord(@PathVariable String commissionNo) {
        return ApiResponse.success(adminBusinessQueryService.getCommissionRecord(commissionNo));
    }

    @Operation(summary = "Admin team overview")
    @GetMapping("/team/overview")
    public ApiResponse<AdminTeamOverviewResponse> teamOverview(
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.adminTeamOverview(startTime, endTime));
    }

    @Operation(summary = "Admin team list")
    @GetMapping("/team/list")
    public ApiResponse<PageResult<AdminTeamListRow>> teamList(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sortBy
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageAdminTeamList(pageNo, pageSize, keyword, status,
                sortBy));
    }

    @Operation(summary = "Admin team leaderboard")
    @GetMapping("/team/leaderboard")
    public ApiResponse<PageResult<AdminTeamLeaderboardRow>> teamLeaderboard(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String sortBy
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageAdminTeamLeaderboard(pageNo, pageSize, sortBy));
    }

    @Operation(summary = "Admin team user summary")
    @GetMapping("/team/user-summary")
    public ApiResponse<AdminTeamUserSummaryResponse> teamUserSummary(
            @RequestParam(name = "user_id") Long userId
    ) {
        return ApiResponse.success(adminBusinessQueryService.adminTeamUserSummary(userId));
    }

    @Operation(summary = "Admin team children")
    @GetMapping("/team/children")
    public ApiResponse<PageResult<AdminTeamTreeNode>> teamChildren(
            @RequestParam(name = "root_user_id") Long rootUserId,
            @RequestParam(name = "parent_user_id") Long parentUserId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "50") long pageSize
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageAdminTeamChildren(rootUserId, parentUserId, pageNo,
                pageSize));
    }

    @Operation(summary = "Admin team members")
    @GetMapping("/team/members")
    public ApiResponse<PageResult<AdminTeamMemberRow>> teamMembers(
            @RequestParam(name = "ancestor_user_id") Long ancestorUserId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "level_depth") Integer levelDepth,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageAdminTeamMembers(ancestorUserId, pageNo, pageSize,
                levelDepth, keyword));
    }

    @Operation(summary = "Admin team relations")
    @GetMapping("/team/relations")
    public ApiResponse<PageResult<AdminTeamRelationResponse>> teamRelations(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "ancestor_user_id") Long ancestorUserId,
            @RequestParam(required = false, name = "descendant_user_id") Long descendantUserId,
            @RequestParam(required = false, name = "level_depth") Integer levelDepth
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageTeamRelations(pageNo, pageSize, ancestorUserId,
                descendantUserId, levelDepth));
    }

    @Operation(summary = "Admin user team")
    @GetMapping("/users/{userId}/team")
    public ApiResponse<AdminUserTeamResponse> userTeam(@PathVariable Long userId) {
        return ApiResponse.success(adminBusinessQueryService.userTeam(userId));
    }

    @Operation(summary = "Admin operation logs")
    @GetMapping("/logs")
    public ApiResponse<PageResult<AdminLogResponse>> logs(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, name = "admin_id") Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false, name = "biz_type") String bizType,
            @RequestParam(required = false, name = "start_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "end_time")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ApiResponse.success(adminBusinessQueryService.pageLogs(pageNo, pageSize, adminId, action,
                bizType, startTime, endTime));
    }

    @Operation(summary = "Admin operation log detail")
    @GetMapping("/logs/{id}")
    public ApiResponse<AdminLogResponse> log(@PathVariable Long id) {
        return ApiResponse.success(adminBusinessQueryService.getLog(id));
    }
}
