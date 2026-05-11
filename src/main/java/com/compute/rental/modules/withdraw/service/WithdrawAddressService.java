package com.compute.rental.modules.withdraw.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.compute.rental.common.enums.CommonStatus;
import com.compute.rental.common.enums.ErrorCode;
import com.compute.rental.common.exception.BusinessException;
import com.compute.rental.common.util.DateTimeUtils;
import com.compute.rental.modules.wallet.entity.UserWithdrawAddress;
import com.compute.rental.modules.wallet.mapper.UserWithdrawAddressMapper;
import com.compute.rental.modules.withdraw.dto.CreateWithdrawAddressRequest;
import com.compute.rental.modules.withdraw.dto.UpdateWithdrawAddressRequest;
import com.compute.rental.modules.withdraw.dto.WithdrawAddressResponse;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WithdrawAddressService {

    private final UserWithdrawAddressMapper withdrawAddressMapper;
    private final WithdrawAddressValidator addressValidator;

    public WithdrawAddressService(
            UserWithdrawAddressMapper withdrawAddressMapper,
            WithdrawAddressValidator addressValidator
    ) {
        this.withdrawAddressMapper = withdrawAddressMapper;
        this.addressValidator = addressValidator;
    }

    public List<WithdrawAddressResponse> listAddresses(Long userId) {
        return withdrawAddressMapper.selectList(new LambdaQueryWrapper<UserWithdrawAddress>()
                        .eq(UserWithdrawAddress::getUserId, userId)
                        .eq(UserWithdrawAddress::getStatus, CommonStatus.ENABLED.value())
                        .orderByDesc(UserWithdrawAddress::getIsDefault)
                        .orderByDesc(UserWithdrawAddress::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WithdrawAddressResponse createAddress(Long userId, CreateWithdrawAddressRequest request) {
        var address = new UserWithdrawAddress();
        applyAddress(address, userId, request.network(), request.accountName(), request.accountNo(), request.label());
        var now = DateTimeUtils.now();
        address.setIsDefault(shouldSetDefault(userId, request.defaultAddress()) ? 1 : 0);
        address.setStatus(CommonStatus.ENABLED.value());
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefault(userId);
        }
        try {
            withdrawAddressMapper.insert(address);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_DUPLICATE);
        }
        return toResponse(address);
    }

    @Transactional
    public WithdrawAddressResponse updateAddress(Long userId, Long addressId, UpdateWithdrawAddressRequest request) {
        var existing = requireAddress(userId, addressId);
        var address = new UserWithdrawAddress();
        applyAddress(address, userId, request.network(), request.accountName(), request.accountNo(), request.label());
        var defaultValue = request.defaultAddress() == null
                ? existing.getIsDefault()
                : Boolean.TRUE.equals(request.defaultAddress()) ? 1 : 0;
        if (defaultValue == 1) {
            clearDefault(userId);
        }
        try {
            var updated = withdrawAddressMapper.update(null, new LambdaUpdateWrapper<UserWithdrawAddress>()
                    .eq(UserWithdrawAddress::getId, addressId)
                    .eq(UserWithdrawAddress::getUserId, userId)
                    .eq(UserWithdrawAddress::getStatus, CommonStatus.ENABLED.value())
                    .set(UserWithdrawAddress::getNetwork, address.getNetwork())
                    .set(UserWithdrawAddress::getAccountName, address.getAccountName())
                    .set(UserWithdrawAddress::getAccountNo, address.getAccountNo())
                    .set(UserWithdrawAddress::getLabel, address.getLabel())
                    .set(UserWithdrawAddress::getIsDefault, defaultValue)
                    .set(UserWithdrawAddress::getUpdatedAt, DateTimeUtils.now()));
            if (updated == 0) {
                throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_NOT_FOUND);
            }
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_DUPLICATE);
        }
        return toResponse(requireAddress(userId, addressId));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        var deleted = withdrawAddressMapper.delete(new LambdaQueryWrapper<UserWithdrawAddress>()
                .eq(UserWithdrawAddress::getId, addressId)
                .eq(UserWithdrawAddress::getUserId, userId));
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_NOT_FOUND);
        }
    }

    @Transactional
    public WithdrawAddressResponse setDefault(Long userId, Long addressId) {
        requireAddress(userId, addressId);
        clearDefault(userId);
        var updated = withdrawAddressMapper.update(null, new LambdaUpdateWrapper<UserWithdrawAddress>()
                .eq(UserWithdrawAddress::getId, addressId)
                .eq(UserWithdrawAddress::getUserId, userId)
                .eq(UserWithdrawAddress::getStatus, CommonStatus.ENABLED.value())
                .set(UserWithdrawAddress::getIsDefault, 1)
                .set(UserWithdrawAddress::getUpdatedAt, DateTimeUtils.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_NOT_FOUND);
        }
        return toResponse(requireAddress(userId, addressId));
    }

    public UserWithdrawAddress requireAddress(Long userId, Long addressId) {
        var address = withdrawAddressMapper.selectOne(new LambdaQueryWrapper<UserWithdrawAddress>()
                .eq(UserWithdrawAddress::getId, addressId)
                .eq(UserWithdrawAddress::getUserId, userId)
                .eq(UserWithdrawAddress::getStatus, CommonStatus.ENABLED.value())
                .last("LIMIT 1"));
        if (address == null) {
            throw new BusinessException(ErrorCode.WITHDRAW_ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void applyAddress(UserWithdrawAddress address, Long userId, String network, String accountName,
                              String accountNo, String label) {
        var normalizedNetwork = addressValidator.requireValid(network, accountNo);
        address.setUserId(userId);
        address.setNetwork(normalizedNetwork);
        address.setAccountName(trimToNull(accountName));
        address.setAccountNo(accountNo.trim());
        address.setLabel(trimToNull(label));
    }

    private boolean shouldSetDefault(Long userId, Boolean defaultAddress) {
        if (Boolean.TRUE.equals(defaultAddress)) {
            return true;
        }
        return withdrawAddressMapper.selectCount(new LambdaQueryWrapper<UserWithdrawAddress>()
                .eq(UserWithdrawAddress::getUserId, userId)
                .eq(UserWithdrawAddress::getStatus, CommonStatus.ENABLED.value())) == 0;
    }

    private void clearDefault(Long userId) {
        withdrawAddressMapper.update(null, new LambdaUpdateWrapper<UserWithdrawAddress>()
                .eq(UserWithdrawAddress::getUserId, userId)
                .eq(UserWithdrawAddress::getIsDefault, 1)
                .set(UserWithdrawAddress::getIsDefault, 0)
                .set(UserWithdrawAddress::getUpdatedAt, DateTimeUtils.now()));
    }

    private WithdrawAddressResponse toResponse(UserWithdrawAddress address) {
        return new WithdrawAddressResponse(
                address.getId(),
                address.getNetwork(),
                address.getAccountName(),
                address.getAccountNo(),
                address.getLabel(),
                Integer.valueOf(1).equals(address.getIsDefault()),
                address.getStatus(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
