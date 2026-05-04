package com.compute.rental.modules.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.wallet.entity.UserWallet;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {

    @Select("SELECT COALESCE(SUM(available_balance), 0) FROM user_wallet")
    BigDecimal sumAvailableBalance();

    @Select("SELECT COALESCE(SUM(frozen_balance), 0) FROM user_wallet")
    BigDecimal sumFrozenBalance();
}
