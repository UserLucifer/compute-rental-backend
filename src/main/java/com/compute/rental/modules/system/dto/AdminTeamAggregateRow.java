package com.compute.rental.modules.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTeamAggregateRow {

    private Long userId;

    private String userName;

    private String email;

    private String avatarKey;

    private Integer userStatus;

    private Long directCount;

    private Long indirectCount;

    private Long totalTeamCount;

    private BigDecimal yesterdayCommission;

    private BigDecimal totalCommission;

    private Long activeOrderCount;

    private Long runningOrderCount;

    private LocalDateTime lastCommissionAt;
}
