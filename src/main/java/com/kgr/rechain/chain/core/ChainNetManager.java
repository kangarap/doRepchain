package com.kgr.rechain.chain.core;

import com.kgr.rechain.chain.entity.ChainNet;
import lombok.AllArgsConstructor;

import java.util.Map;

import static com.kgr.rechain.chain.utils.Constants.BIZ;
import static com.kgr.rechain.chain.utils.Constants.IDENTITY;

/**
 * @author kgr
 * @create 2022-07-06 13:55
 */
@AllArgsConstructor
public class ChainNetManager {

    private final Map<String, ChainNet> map;

    public ChainNet net(String type) {

        if(!map.containsKey(type)) {
            throw new RuntimeException(String.format("网络类型 %s 不存在！", type));
        }

        return map.get(type);
    }


    public ChainNet bizNet() {
        return this.net(BIZ);
    }

    public ChainNet identityNet() {
        return this.net(IDENTITY);
    }
}
