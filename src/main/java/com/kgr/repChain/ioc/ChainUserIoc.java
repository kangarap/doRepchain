package com.kgr.repChain.ioc;

import com.kgr.repChain.entity.ChainUser;
import org.springframework.stereotype.Component;

/**
 * @author kgr
 * @create 2022-07-08 10:31
 */
@Component
public interface ChainUserIoc {
    void init();

    ChainUser getSuperUser();
    void addChainUser(ChainUser chainUser);

    ChainUser getChainUser(String username);
}
