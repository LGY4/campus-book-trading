package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Address;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.AddressMapper;
import com.booktrading.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    @Transactional
    public void createAddress(Address address) {
        address.setIsDefault(address.getIsDefault() == null ? 0 : address.getIsDefault());
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefault(address.getUserId());
        }
        save(address);
    }

    @Override
    @Transactional
    public void updateAddress(Address address) {
        if (getById(address.getId()) == null) {
            throw new BusinessException("地址不存在");
        }
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            clearDefault(address.getUserId());
        }
        updateById(address);
    }

    @Override
    public void deleteAddress(Long id, Long userId) {
        Address address = getById(id);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此地址");
        }
        removeById(id);
    }

    @Override
    public List<Address> getMyAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId);
        wrapper.orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreateTime);
        return list(wrapper);
    }

    @Override
    @Transactional
    public void setDefault(Long addressId, Long userId) {
        Address address = getById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此地址");
        }
        clearDefault(userId);
        address.setIsDefault(1);
        updateById(address);
    }

    @Override
    public Address getDefault(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1);
        return getOne(wrapper);
    }

    private void clearDefault(Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .set(Address::getIsDefault, 0);
        update(wrapper);
    }
}
