package com.kgr.rechain.chain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author kgr
 * @create 2022-07-05 10:38
 */
@Data
@Component
@ConfigurationProperties(prefix = "chain")
public class ChainProperties {
    private List<ChainNet> net;
    private List<ChainUser> user;
    private List<ChainCode> chaincode;
}

@Data
class ChainNet {
    String type;
    String host;
    String prefix;
}



@Data
class ChainUser {
    private String mobile;
    private String type;
    private String username;
    private String creditCode;
    private Jks jks;
}

@Data
class Jks{
    private String path;
    private String password;
    private String alias;
}