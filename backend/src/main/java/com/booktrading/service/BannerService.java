package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Banner;

import java.util.List;

public interface BannerService extends IService<Banner> {

    IPage<Banner> listAdmin(int page, int size);

    void createBanner(Banner banner);

    void updateBanner(Banner banner);

    void deleteBanner(Long id);

    void toggleStatus(Long id, Integer status);

    List<Banner> listEnabled();
}
