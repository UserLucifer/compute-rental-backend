package com.compute.rental.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rental_order_run_segment")
public class RentalOrderRunSegment {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("rental_order_id")
    private Long rentalOrderId;

    @TableField("user_id")
    private Long userId;

    @TableField("segment_start_at")
    private LocalDateTime segmentStartAt;

    @TableField("segment_end_at")
    private LocalDateTime segmentEndAt;

    @TableField("close_reason")
    private String closeReason;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
