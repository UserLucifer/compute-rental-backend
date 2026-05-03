package com.compute.rental.modules.system.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemEnumServiceTest {

    @Test
    void frontendEnumsExposeBusinessWhitelistAndHideReservedOrderStatuses() {
        var enums = new SystemEnumService().frontendEnums();

        assertThat(enums).containsKeys("commonStatus", "rentalOrderStatus", "walletTransactionType");
        assertThat(enums).doesNotContainKeys("errorCode", "adminRole", "schedulerLogStatus");

        assertThat(enums.get("commonStatus"))
                .anySatisfy(option -> {
                    assertThat(option.name()).isEqualTo("ENABLED");
                    assertThat(option.value()).isEqualTo(1);
                    assertThat(option.label()).isEqualTo("启用");
                });

        assertThat(enums.get("rentalOrderStatus"))
                .extracting(option -> option.name())
                .contains("PENDING_PAY", "RUNNING", "CANCELED")
                .doesNotContain("PAID", "SETTLING", "SETTLED");
    }
}
