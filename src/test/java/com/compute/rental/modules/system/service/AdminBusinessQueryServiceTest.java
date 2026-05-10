package com.compute.rental.modules.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.compute.rental.common.enums.ApiTokenStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.enums.RentalOrderStatus;
import com.compute.rental.common.enums.RunSegmentCloseReason;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.modules.commission.mapper.CommissionRecordMapper;
import com.compute.rental.modules.order.entity.ApiCredential;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.ApiCredentialMapper;
import com.compute.rental.modules.order.mapper.ApiDeployOrderMapper;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.order.mapper.RentalProfitRecordMapper;
import com.compute.rental.modules.order.mapper.RentalSettlementOrderMapper;
import com.compute.rental.modules.order.service.RentalOrderRunSegmentService;
import com.compute.rental.modules.system.dto.AdminTeamAggregateRow;
import com.compute.rental.modules.system.mapper.SysAdminLogMapper;
import com.compute.rental.modules.user.entity.AppUser;
import com.compute.rental.modules.user.entity.UserTeamRelation;
import com.compute.rental.modules.user.mapper.AppUserMapper;
import com.compute.rental.modules.user.mapper.UserTeamRelationMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import com.compute.rental.modules.wallet.mapper.UserWalletMapper;
import com.compute.rental.modules.wallet.mapper.WalletTransactionMapper;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminBusinessQueryServiceTest {

    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private UserWalletMapper userWalletMapper;
    @Mock
    private WalletTransactionMapper walletTransactionMapper;
    @Mock
    private RentalOrderMapper rentalOrderMapper;
    @Mock
    private ApiCredentialMapper apiCredentialMapper;
    @Mock
    private ApiDeployOrderMapper apiDeployOrderMapper;
    @Mock
    private RentalProfitRecordMapper profitRecordMapper;
    @Mock
    private RentalSettlementOrderMapper settlementOrderMapper;
    @Mock
    private CommissionRecordMapper commissionRecordMapper;
    @Mock
    private UserTeamRelationMapper teamRelationMapper;
    @Mock
    private SysAdminLogMapper adminLogMapper;
    @Mock
    private AdminLogService adminLogService;
    @Mock
    private RentalOrderRunSegmentService runSegmentService;

    private AdminBusinessQueryService service;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), AppUser.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ApiCredential.class);
    }

    @BeforeEach
    void setUp() {
        service = new AdminBusinessQueryService(appUserMapper, userWalletMapper, walletTransactionMapper,
                rentalOrderMapper, apiCredentialMapper, apiDeployOrderMapper, profitRecordMapper,
                settlementOrderMapper, commissionRecordMapper, teamRelationMapper, adminLogMapper, adminLogService,
                runSegmentService);
    }

    @Test
    void disableUserPausesRunningOrdersAndCredentials() {
        var user = new AppUser();
        user.setId(10L);
        user.setStatus(1);
        var order = new RentalOrder();
        order.setId(20L);
        order.setUserId(10L);
        order.setOrderStatus(RentalOrderStatus.RUNNING.name());

        when(appUserMapper.selectById(10L)).thenReturn(user);
        when(rentalOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(appUserMapper.update(isNull(), any())).thenReturn(1);
        when(rentalOrderMapper.update(isNull(), any())).thenReturn(1);
        when(apiCredentialMapper.update(isNull(), any())).thenReturn(1);

        var result = service.disableUser(10L, 1L, "127.0.0.1");

        assertEquals(10L, result.id());
        verify(rentalOrderMapper).update(isNull(), any());
        verify(apiCredentialMapper).update(isNull(), any());
        verify(runSegmentService).closeOpenSegment(eq(20L), any(), eq(RunSegmentCloseReason.ADMIN_DISABLE));
        verify(adminLogService).log(eq(1L), eq("BAN_USER"), eq("app_user"), eq(10L),
                isNull(), eq("status=0"), any(), eq("127.0.0.1"));
    }

    @Test
    void disabledUserShouldNotBeEnabledAgain() {
        var user = new AppUser();
        user.setId(10L);
        user.setStatus(0);
        when(appUserMapper.selectById(10L)).thenReturn(user);

        assertThatThrownBy(() -> service.enableUser(10L, 1L, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_REENABLE_NOT_ALLOWED);
    }

    @Test
    void apiCredentialResponseDoesNotExposeCiphertext() {
        var credential = new ApiCredential();
        credential.setId(1L);
        credential.setCredentialNo("CR001");
        credential.setTokenCiphertext("secret");
        credential.setTokenMasked("tok***001");
        credential.setTokenStatus(ApiTokenStatus.GENERATED.name());
        when(apiCredentialMapper.selectOne(any())).thenReturn(credential);

        var result = service.getApiCredential("CR001");

        assertEquals("tok***001", result.tokenMasked());
    }

    @Test
    void rentalOrderDetailIncludesUserName() {
        var order = new RentalOrder();
        order.setId(20L);
        order.setUserId(10L);
        order.setOrderNo("RO001");
        var user = new AppUser();
        user.setId(10L);
        user.setUserName("Alice");

        when(rentalOrderMapper.selectOne(any())).thenReturn(order);
        when(apiCredentialMapper.selectOne(any())).thenReturn(null);
        when(appUserMapper.selectById(10L)).thenReturn(user);

        var result = service.getRentalOrder("RO001");

        assertEquals("RO001", result.orderNo());
        assertEquals("Alice", result.userName());
    }

    @Test
    void walletTransactionsReturnAdminDtoWithUserNameAndIdempotencyKey() {
        var transaction = new WalletTransaction();
        transaction.setId(30L);
        transaction.setTxNo("TX001");
        transaction.setIdempotencyKey("idem-001");
        transaction.setUserId(10L);

        var page = new Page<WalletTransaction>(1, 10);
        page.setRecords(List.of(transaction));
        page.setTotal(1);

        var user = new AppUser();
        user.setId(10L);
        user.setUserName("Alice");

        when(walletTransactionMapper.selectPage(any(), any())).thenReturn(page);
        when(appUserMapper.selectBatchIds(any())).thenReturn(List.of(user));

        var result = service.pageWalletTransactions(1, 10, null, null, null, null, null, null, null);
        var response = result.records().get(0);

        assertEquals(30L, response.id());
        assertEquals("idem-001", response.idempotencyKey());
        assertEquals("Alice", response.userName());
    }

    @Test
    void userTeamReturnsDtoRelations() {
        var user = new AppUser();
        user.setId(10L);
        when(appUserMapper.selectById(10L)).thenReturn(user);

        var relation = new UserTeamRelation();
        relation.setId(99L);
        relation.setAncestorUserId(10L);
        relation.setDescendantUserId(11L);
        relation.setLevelDepth(1);
        when(teamRelationMapper.selectList(any())).thenReturn(List.of(relation));

        var result = service.userTeam(10L);

        assertEquals(1, result.totalTeamCount());
        assertEquals(99L, result.relations().get(0).id());
        assertEquals(11L, result.relations().get(0).descendantUserId());
    }

    @Test
    void teamListUsesSqlAggregatePagination() {
        when(teamRelationMapper.countAdminTeamAggregates(eq(2), eq("alice"), isNull(), eq(1))).thenReturn(1L);
        when(teamRelationMapper.selectAdminTeamAggregatePage(
                eq(2),
                eq("alice"),
                isNull(),
                eq(1),
                any(),
                any(),
                eq("SETTLED"),
                eq("RUNNING"),
                eq("PAUSED"),
                eq("directCount"),
                eq(20L),
                eq(20L))).thenReturn(List.of(teamAggregateRow(10L)));

        var result = service.pageAdminTeamList(2, 20, " alice ", 1, "directCount");

        assertEquals(1L, result.total());
        assertEquals(2L, result.pageNo());
        assertEquals(20L, result.pageSize());
        assertEquals(10L, result.records().get(0).userId());
    }

    @Test
    void teamLeaderboardUsesSqlAggregatePaginationAndRankOffset() {
        when(teamRelationMapper.countAdminTeamAggregates(eq(2), isNull(), isNull(), isNull())).thenReturn(3L);
        when(teamRelationMapper.selectAdminTeamAggregatePage(
                eq(2),
                isNull(),
                isNull(),
                isNull(),
                any(),
                any(),
                eq("SETTLED"),
                eq("RUNNING"),
                eq("PAUSED"),
                eq("totalCommission"),
                eq(2L),
                eq(2L))).thenReturn(List.of(teamAggregateRow(30L)));

        var result = service.pageAdminTeamLeaderboard(2, 2, null);

        assertEquals(3L, result.total());
        assertEquals(3L, result.records().get(0).rankNo());
        assertEquals(30L, result.records().get(0).userId());
    }

    private AdminTeamAggregateRow teamAggregateRow(Long userId) {
        var row = new AdminTeamAggregateRow();
        row.setUserId(userId);
        row.setUserName("Alice");
        row.setEmail("alice@example.com");
        row.setAvatarKey("avatar");
        row.setUserStatus(1);
        row.setDirectCount(2L);
        row.setIndirectCount(1L);
        row.setTotalTeamCount(3L);
        row.setYesterdayCommission(BigDecimal.ONE);
        row.setTotalCommission(BigDecimal.TEN);
        row.setActiveOrderCount(1L);
        row.setRunningOrderCount(1L);
        return row;
    }
}
