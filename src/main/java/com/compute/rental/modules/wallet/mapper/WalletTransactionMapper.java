package com.compute.rental.modules.wallet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.compute.rental.modules.wallet.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {
}
