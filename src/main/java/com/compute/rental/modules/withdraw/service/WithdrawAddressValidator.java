package com.compute.rental.modules.withdraw.service;

import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class WithdrawAddressValidator {

    private static final Pattern TRC20 = Pattern.compile("^T[1-9A-HJ-NP-Za-km-z]{33}$");
    private static final Pattern EVM = Pattern.compile("^0x[0-9a-fA-F]{40}$");

    public String requireValid(String network, String accountNo) {
        var normalizedNetwork = network == null ? "" : network.trim().toUpperCase(Locale.ROOT);
        var normalizedAccountNo = accountNo == null ? "" : accountNo.trim();
        var valid = switch (normalizedNetwork) {
            case "TRC20" -> TRC20.matcher(normalizedAccountNo).matches();
            case "ERC20", "BEP20" -> EVM.matcher(normalizedAccountNo).matches();
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_INVALID);
        }
        return normalizedNetwork;
    }
}
