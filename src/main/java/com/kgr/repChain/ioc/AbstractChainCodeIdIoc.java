package com.kgr.repChain.ioc;

import com.kgr.repChain.entity.ChainCode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-08 10:56
 */
@Component
public abstract class AbstractChainCodeIdIoc implements ChainCodeIoc {

    public final Map<String, ChainCode> chainCodeMap;

    public AbstractChainCodeIdIoc() {
        String [] func = new String []{ "signUpOperate", "signUpSigner","grantOperate"};
        ChainCode codes = new ChainCode("RdidOperateAuthorizeTPL","",1, func);
        this.chainCodeMap = new HashMap<>();
        this.chainCodeMap.put(codes.getName(), codes);
    }

    @Override
    public void init() {
    }

    @Override
    public ChainCode getDidChain(){
        return chainCodeMap.get("RdidOperateAuthorizeTPL");
    }

    @Override
    public void addChainCode(ChainCode chainCode) {

        chainCodeMap.put(chainCode.getName(), chainCode);
    }

    @Override
    public ChainCode getChainCode(String name) {

        return chainCodeMap.get(name);
    }
}
