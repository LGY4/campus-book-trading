package com.booktrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    void createAddress(Address address);

    void updateAddress(Address address);

    void deleteAddress(Long id, Long userId);

    List<Address> getMyAddresses(Long userId);

    void setDefault(Long addressId, Long userId);

    Address getDefault(Long userId);
}
