package com.kgr.rechain.chain.core;

import com.kgr.rechain.chain.entity.ChainCode;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-06 13:55
 */
@AllArgsConstructor
public class ChainCodeIdManager {
    private final Map<String, ChainCode> chainCodeMap;

    public ChainCode chainCode(String name) {

        if(!chainCodeMap.containsKey(name)) {

            throw new RuntimeException(String.format("配置中找不到名字为 %s 的合约！", name));
        }

        return chainCodeMap.get(name);

    }
}
