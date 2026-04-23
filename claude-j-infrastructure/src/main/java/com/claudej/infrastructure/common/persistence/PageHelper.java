package com.claudej.infrastructure.common.persistence;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.claudej.domain.common.model.valobj.PageRequest;

import java.util.function.Function;

/**
 * 分页工具类 - MyBatis-Plus IPage 与 Domain Page 转换
 */
public final class PageHelper {

    private PageHelper() {
    }

    /**
     * 创建 MyBatis-Plus 分页对象
     */
    public static <T> Page<T> createMybatisPlusPage(PageRequest pageRequest) {
        if (pageRequest == null) {
            pageRequest = PageRequest.defaultPage();
        }
        return new Page<T>(pageRequest.getPage() + 1, pageRequest.getSize());
    }

    /**
     * IPage 转 Domain Page（直接映射，不转换元素）
     */
    public static <T> com.claudej.domain.common.model.valobj.Page<T> toDomainPage(IPage<T> iPage) {
        if (iPage == null) {
            return com.claudej.domain.common.model.valobj.Page.empty(0, 20);
        }
        return new com.claudej.domain.common.model.valobj.Page<T>(
                iPage.getRecords(),
                iPage.getTotal(),
                (int) iPage.getCurrent() - 1, // MyBatis-Plus 页码从 1 开始，Domain 从 0 开始
                (int) iPage.getSize()
        );
    }

    /**
     * IPage 转 Domain Page 并转换元素类型
     */
    public static <T, R> com.claudej.domain.common.model.valobj.Page<R> toDomainPage(IPage<T> iPage, Function<T, R> converter) {
        if (iPage == null) {
            return com.claudej.domain.common.model.valobj.Page.empty(0, 20);
        }
        java.util.List<R> convertedRecords = iPage.getRecords().stream()
                .map(converter)
                .collect(java.util.stream.Collectors.toList());
        return new com.claudej.domain.common.model.valobj.Page<R>(
                convertedRecords,
                iPage.getTotal(),
                (int) iPage.getCurrent() - 1,
                (int) iPage.getSize()
        );
    }
}