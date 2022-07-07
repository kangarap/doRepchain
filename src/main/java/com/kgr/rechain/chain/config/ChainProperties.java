package com.kgr.rechain.chain.config;

import com.kgr.rechain.chain.entity.ChainCode;
import com.kgr.rechain.chain.entity.ChainNet;
import com.kgr.rechain.chain.entity.ChainUser;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author kgr
 * @create 2022-07-05 10:38
 */
@Data
@ConfigurationProperties(prefix = "chain")
public class ChainProperties {
    private List<ChainNet> net;
    private List<ChainUser> user;
    private List<ChainCode> chaincode;
}
