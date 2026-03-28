package com.claudej.domain.shortlink.service;

import com.claudej.domain.shortlink.model.valobj.ShortCode;

public interface ShortCodeGenerator {

    ShortCode generate(Long id);
}
