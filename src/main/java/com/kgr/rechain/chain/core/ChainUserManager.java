package com.kgr.rechain.chain.core;

import com.kgr.rechain.chain.entity.ChainUser;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-06 13:54
 */
@AllArgsConstructor
public class ChainUserManager {
    private final ChainUser superUser;
    private final Map<String, ChainUser> normalUser;

    public ChainUser admin() {
        return this.superUser;
    }

    public ChainUser normalUser(String username) {

        boolean exist = normalUser.containsKey(username);
        if(!exist) {
            throw new RuntimeException(String.format("用户 %s 不存在！", username));
        }

        return normalUser.get(username);
    }
}
