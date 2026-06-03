package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Banner;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.BannerMapper;
import com.booktrading.service.BannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Override
    public IPage<Banner> listAdmin(int page, int size) {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Banner::getSortOrder, Banner::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional
    public void createBanner(Banner banner) {
        if (banner.getBookId() == null) {
            throw new BusinessException("请选择书籍");
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        save(banner);
    }

    @Override
    @Transactional
    public void updateBanner(Banner banner) {
        if (banner.getId() == null) {
            throw new BusinessException("缺少ID");
        }
        if (banner.getBookId() == null) {
            throw new BusinessException("请选择书籍");
        }
        updateById(banner);
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        removeById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        Banner banner = getById(id);
        if (banner == null) {
            throw new BusinessException("轮播不存在");
        }
        banner.setStatus(status);
        updateById(banner);
    }

    @Override
    public List<Banner> listEnabled() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
                .orderByDesc(Banner::getSortOrder, Banner::getCreateTime);
        return list(wrapper);
    }
}
