package com.kgr.repChain.ioc;

import com.kgr.repChain.entity.ChainUser;
import com.kgr.repChain.utils.Constants;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-08 10:34
 */
@Component
public abstract class AbstractChainUserIoc implements ChainUserIoc {

    public final Map<String, ChainUser> chainUserMap;

    protected AbstractChainUserIoc() {
        this.chainUserMap = new HashMap<>();
    }

    @Override
    public void init() {}

    @Override
    public ChainUser getSuperUser(){

        if (!chainUserMap.containsKey(Constants.ADMIN)) {
            throw new RuntimeException("管理员配置缺失！");
        }

        return chainUserMap.get(Constants.ADMIN);
    }
    @Override
    public void addChainUser(ChainUser chainUser){
        chainUserMap.put(chainUser.getUsername(), chainUser);
    }

    @Override
    public ChainUser getChainUser(String username){

        if (!chainUserMap.containsKey(username)) {
            throw new RuntimeException(String.format("用户 %s 不存在！", username));
        }
        return chainUserMap.get(username);
    }

}
