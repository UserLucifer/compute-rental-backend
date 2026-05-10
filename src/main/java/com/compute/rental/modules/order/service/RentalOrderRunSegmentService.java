package com.compute.rental.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.RunSegmentCloseReason;
import com.compute.rental.modules.order.entity.RentalOrder;
import com.compute.rental.modules.order.entity.RentalOrderRunSegment;
import com.compute.rental.modules.order.mapper.RentalOrderRunSegmentMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalOrderRunSegmentService {

    private final RentalOrderRunSegmentMapper runSegmentMapper;

    @Transactional
    public void openSegment(RentalOrder order, LocalDateTime startAt) {
        if (order == null || order.getId() == null || order.getUserId() == null || startAt == null) {
            return;
        }
        var openSegment = runSegmentMapper.selectOne(new LambdaQueryWrapper<RentalOrderRunSegment>()
                .eq(RentalOrderRunSegment::getRentalOrderId, order.getId())
                .isNull(RentalOrderRunSegment::getSegmentEndAt)
                .last("LIMIT 1"));
        if (openSegment != null) {
            return;
        }

        var segment = new RentalOrderRunSegment();
        segment.setRentalOrderId(order.getId());
        segment.setUserId(order.getUserId());
        segment.setSegmentStartAt(startAt);
        segment.setCreatedAt(startAt);
        segment.setUpdatedAt(startAt);
        runSegmentMapper.insert(segment);
    }

    @Transactional
    public void closeOpenSegment(Long rentalOrderId, LocalDateTime endAt, RunSegmentCloseReason reason) {
        if (rentalOrderId == null || endAt == null || reason == null) {
            return;
        }
        var segment = runSegmentMapper.selectOne(new LambdaQueryWrapper<RentalOrderRunSegment>()
                .eq(RentalOrderRunSegment::getRentalOrderId, rentalOrderId)
                .isNull(RentalOrderRunSegment::getSegmentEndAt)
                .last("LIMIT 1"));
        if (segment == null) {
            return;
        }

        var closeAt = endAt.isBefore(segment.getSegmentStartAt()) ? segment.getSegmentStartAt() : endAt;
        runSegmentMapper.update(null, new LambdaUpdateWrapper<RentalOrderRunSegment>()
                .eq(RentalOrderRunSegment::getId, segment.getId())
                .isNull(RentalOrderRunSegment::getSegmentEndAt)
                .set(RentalOrderRunSegment::getSegmentEndAt, closeAt)
                .set(RentalOrderRunSegment::getCloseReason, reason.name())
                .set(RentalOrderRunSegment::getUpdatedAt, closeAt));
    }
}
