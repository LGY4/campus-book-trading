package com.booktrading.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booktrading.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
