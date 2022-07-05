package com.kgr.rechain.chain.config;

import com.kgr.rechain.chain.entity.Net.BizNet;
import com.kgr.rechain.chain.entity.Net.IdentityNet;
import com.kgr.rechain.chain.entity.User.NormalUser;
import com.kgr.rechain.chain.entity.User.SuperUser;
import com.rcjava.protos.Peer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-01 17:44
 */
@Configuration
@EnableConfigurationProperties(ChainProperties.class)
@Slf4j
public class ChainConfiguration {

    @Resource
    private ChainProperties chainProperties;

    @Bean
    public SuperUser superUser() {
        for (ChainUser user : chainProperties.getUser()) {
            if(!"admin".equals(user.getType())) {
                continue;
            }
            return new SuperUser(user.getUsername(), user.getCreditCode(), user.getJks().getPath(), user.getJks().getPassword(), user.getJks().getAlias());
        }
        throw new RuntimeException("请在配置文件中添加Admin项");
    }

    @Bean
    @ConditionalOnBean(SuperUser.class)
    public NormalUser normalUser() throws IOException {

        for (ChainUser user : chainProperties.getUser()) {
            if("admin".equals(user.getType())) {
                continue;
            }
            return new NormalUser(user.getUsername(), user.getMobile(), user.getCreditCode(), user.getJks().getPath(), user.getJks().getPassword(), user.getJks().getAlias());
        }
        throw new RuntimeException("请在配置文件中添加Normal项");
    }




    @Bean
    public IdentityNet identityNet() {
        for (ChainNet net : chainProperties.getNet()) {
            if(!"identity".equals(net.getType())) {
                continue;
            }
            return new IdentityNet(net.getType(), net.getPrefix(), net.getHost());
        }
        throw new RuntimeException("请在配置文件中添加网络项");
    }

    @Bean
    public BizNet bizNet() {
        for (ChainNet net : chainProperties.getNet()) {
            if("identity".equals(net.getType())) {
                continue;
            }
            return new BizNet(net.getType(), net.getPrefix(), net.getHost());
        }
        throw new RuntimeException("请在配置文件中添加网络项");
    }


    @Bean
    public Map<String, ChainCode> injectChainCodeId() {

        Map<String, ChainCode> chainCodeMap = new HashMap<>();

        List<ChainCode> chainCodeList = chainProperties.getChaincode();

        if(chainCodeList.size() == 0) {
            throw new RuntimeException("请在配置文件中添加合约信息");
        }

        chainCodeList.forEach(chainCode -> {
            chainCode.setChaincodeId(Peer.ChaincodeId.newBuilder().setChaincodeName(chainCode.getName()).setVersion(chainCode.getVersion()).build());
            chainCodeMap.put(chainCode.getName(), chainCode);
        });

        log.info("==========> 合约注入，共{}个，成功！", chainCodeList.size());
        return chainCodeMap;
    }
}




