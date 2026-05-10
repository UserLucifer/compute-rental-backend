package com.compute.rental.modules.commission.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommissionSourceAmountRow {

    private Long sourceUserId;

    private BigDecimal amount;
}
