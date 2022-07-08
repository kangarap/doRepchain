package com.kgr.repChain.ioc;

import com.kgr.repChain.entity.ChainUser;
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
        try {
            ChainUser superUser = new ChainUser(
                    "admin",
                    "1888888869",
                    "super_admin",
                    "identity-net:951002007l78123233",
                    "/Users/lihao/Documents/newJks/newJks/951002007l78123233.super_admin.jks",
                    "mPUo6^pD38z0@H$0%^On",
                    "951002007l78123233.super_admin"
            );


            this.chainUserMap = new HashMap<>();
            this.chainUserMap.put(superUser.getUsername(), superUser);
        }catch (Exception e){
            throw new RuntimeException("112");
        }
    }

    @Override
    public void init() {}


    @Override
    public ChainUser getSuperUser(){
        return chainUserMap.get("super_admin");
    }
    @Override
    public void addChainUser(ChainUser chainUser){
        chainUserMap.put(chainUser.getUsername(), chainUser);
    }

    @Override
    public ChainUser getChainUser(String username){

        return chainUserMap.get(username);
    }

}
