package com.kgr.repChain.core;

import com.kgr.repChain.entity.ChainNet;
import lombok.AllArgsConstructor;

import java.util.Map;

import static com.kgr.repChain.utils.Constants.*;

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

    public ChainNet connector() {
        return this.net(CONNECTOR);
    }


    public ChainNet bizNet() {
        return this.net(BIZ);
    }

    public ChainNet identityNet() {
        return this.net(IDENTITY);
    }
}
