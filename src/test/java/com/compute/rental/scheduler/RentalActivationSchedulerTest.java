package com.compute.rental.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.mapper.RentalOrderMapper;
import com.compute.rental.modules.system.entity.SchedulerLog;
import com.compute.rental.modules.system.service.SchedulerLogService;
import com.compute.rental.modules.system.service.SysConfigDefaults;
import com.compute.rental.modules.system.service.SysConfigService;
import java.time.Duration;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalActivationSchedulerTest {

    @Mock
    private SchedulerLockTemplate schedulerLockTemplate;

    @Mock
    private SchedulerLogService schedulerLogService;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private RentalOrderMapper rentalOrderMapper;

    @Mock
    private RentalActivationSchedulerProcessor processor;

    @InjectMocks
    private RentalActivationScheduler scheduler;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        var configuration = new Configuration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), RentalOrder.class);
    }

    @Test
    void singleOrderFailureShouldNotStopBatch() {
        when(schedulerLockTemplate.runWithLock(any(), any(Duration.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        });
        var log = new SchedulerLog();
        log.setId(1L);
        when(schedulerLogService.start(SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL)).thenReturn(log);
        when(sysConfigService.getInteger(eq(SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES), eq(15)))
                .thenReturn(15);
        var first = order(1L, "RO001");
        var second = order(2L, "RO002");
        when(rentalOrderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(first, second));
        doThrow(new RuntimeException("failed")).when(processor).cancelDeployFeeTimeout(eq(1L), any());

        var result = scheduler.runDeployFeeTimeoutCancel();

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failCount()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("PARTIAL_FAIL");
    }

    @Test
    void scheduledNoopDeployFeeTimeoutShouldNotWriteSchedulerLog() {
        when(schedulerLockTemplate.runWithLock(any(), any(Duration.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        });
        when(sysConfigService.getInteger(eq(SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES), eq(15)))
                .thenReturn(15);
        when(rentalOrderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        scheduler.scheduledDeployFeeTimeoutCancel();

        verify(schedulerLogService, never()).start(SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL);
    }

    @Test
    void manualNoopDeployFeeTimeoutShouldWriteSchedulerLog() {
        when(schedulerLockTemplate.runWithLock(any(), any(Duration.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        });
        var log = new SchedulerLog();
        log.setId(1L);
        when(schedulerLogService.start(SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL)).thenReturn(log);
        when(sysConfigService.getInteger(eq(SysConfigDefaults.ORDER_PENDING_ACTIVATION_TIMEOUT_MINUTES), eq(15)))
                .thenReturn(15);
        when(rentalOrderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        var result = scheduler.runDeployFeeTimeoutCancel();

        assertThat(result.totalCount()).isZero();
        verify(schedulerLogService).start(SchedulerTaskNames.DEPLOY_FEE_TIMEOUT_CANCEL);
        verify(schedulerLogService).finish(log, 0, 0, 0, null);
    }

    @Test
    void scheduledNoopAutoPauseShouldNotWriteSchedulerLog() {
        when(schedulerLockTemplate.runWithLock(any(), any(Duration.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        });
        when(rentalOrderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        scheduler.scheduledAutoPause();

        verify(schedulerLogService, never()).start(SchedulerTaskNames.AUTO_PAUSE);
    }

    @Test
    void manualNoopAutoPauseShouldWriteSchedulerLog() {
        when(schedulerLockTemplate.runWithLock(any(), any(Duration.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        });
        var log = new SchedulerLog();
        log.setId(1L);
        when(schedulerLogService.start(SchedulerTaskNames.AUTO_PAUSE)).thenReturn(log);
        when(rentalOrderMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        var result = scheduler.runAutoPause();

        assertThat(result.totalCount()).isZero();
        verify(schedulerLogService).start(SchedulerTaskNames.AUTO_PAUSE);
        verify(schedulerLogService).finish(log, 0, 0, 0, null);
    }

    private RentalOrder order(Long id, String orderNo) {
        var order = new RentalOrder();
        order.setId(id);
        order.setOrderNo(orderNo);
        return order;
    }
}
