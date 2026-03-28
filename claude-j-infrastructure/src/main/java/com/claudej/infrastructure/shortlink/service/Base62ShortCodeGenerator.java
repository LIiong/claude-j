package com.claudej.infrastructure.shortlink.service;

import com.claudej.domain.shortlink.model.valobj.ShortCode;
import com.claudej.domain.shortlink.service.ShortCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    /**
     * 乘法逆元位混淆常量（模 2^32）
     * PRIME * INVERSE ≡ 1 (mod 2^32)
     */
    private static final long PRIME = 2654435761L;
    private static final long MAX = 1L << 32;

    @Override
    public ShortCode generate(Long id) {
        long obfuscated = obfuscate(id);
        String encoded = encodeBase62(obfuscated);
        return new ShortCode(encoded);
    }

    /**
     * 乘法逆元位混淆：将顺序 ID 映射为伪随机数，零碰撞
     */
    long obfuscate(long id) {
        return (id * PRIME) & (MAX - 1);
    }

    /**
     * Base62 编码，左填充到 6 位
     */
    String encodeBase62(long value) {
        if (value == 0) {
            return padLeft("0");
        }

        StringBuilder sb = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            sb.append(BASE62.charAt((int) (remaining % 62)));
            remaining /= 62;
        }
        return padLeft(sb.reverse().toString());
    }

    private String padLeft(String s) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < CODE_LENGTH) {
            sb.insert(0, '0');
        }
        return sb.toString();
    }
}
