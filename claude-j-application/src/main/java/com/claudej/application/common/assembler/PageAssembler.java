package com.claudej.application.common.assembler;

import com.claudej.application.common.dto.PageDTO;
import com.claudej.domain.common.model.valobj.Page;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.function.Function;

/**
 * 分页结果转换器 - Domain Page 转 Application PageDTO
 */
@Mapper(componentModel = "spring")
public interface PageAssembler {

    /**
     * Domain Page 转 PageDTO（不转换内部元素）
     * 注意：此方法仅转换分页元信息，content 列表需要在外部单独转换
     */
    @Mapping(target = "content", ignore = true)
    PageDTO<Object> toPageDTO(Page<?> page);

    /**
     * 转换分页结果并转换内部元素
     *
     * @param page         Domain 分页结果
     * @param contentMapper 内容转换函数
     * @return PageDTO
     */
    default <T, R> PageDTO<R> toPageDTO(Page<T> page, Function<T, R> contentMapper) {
        if (page == null) {
            return PageDTO.empty(0, 20);
        }

        PageDTO<R> pageDTO = new PageDTO<R>();
        pageDTO.setContent(page.getContent().stream()
                .map(contentMapper)
                .collect(java.util.stream.Collectors.toList()));
        pageDTO.setTotalElements(page.getTotalElements());
        pageDTO.setTotalPages(page.getTotalPages());
        pageDTO.setPage(page.getPage());
        pageDTO.setSize(page.getSize());
        pageDTO.setFirst(page.isFirst());
        pageDTO.setLast(page.isLast());
        pageDTO.setEmpty(page.isEmpty());
        return pageDTO;
    }
}