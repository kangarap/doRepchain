package com.kgr.repChain.ioc;

import com.kgr.repChain.entity.ChainCode;
import org.springframework.stereotype.Component;

/**
 * @author kgr
 * @create 2022-07-08 10:56
 */
@Component
public interface ChainCodeIoc {
    void init();

    ChainCode getDidChain();
    void addChainCode(ChainCode chainCode);

    ChainCode getChainCode(String name);
}
