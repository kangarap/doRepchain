package com.kgr.rechain.chain.config;

import com.google.common.collect.Maps;
import com.kgr.rechain.chain.core.ChainCodeIdManager;
import com.kgr.rechain.chain.core.ChainNetManager;
import com.kgr.rechain.chain.core.ChainUserManager;
import com.kgr.rechain.chain.entity.ChainCode;
import com.kgr.rechain.chain.entity.ChainNet;
import com.kgr.rechain.chain.entity.ChainUser;
import com.rcjava.protos.Peer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.kgr.rechain.chain.utils.Constants.*;

/**
 * @author kgr
 * @create 2022-07-01 17:44
 */
@Configuration
@EnableConfigurationProperties(ChainProperties.class)
@Slf4j
public class ChainAutoConfiguration {

    @Resource
    private ChainProperties chainProperties;

    @Bean
//    @ConditionalOnMissingBean
    public ChainUserManager chainUserManager() throws IOException {

        Optional<ChainUser> chainUserOptional =  chainProperties.getUser().stream().filter(user -> ADMIN.equals(user.getType())).findFirst();

        if(!chainUserOptional.isPresent()) {
            throw new RuntimeException("请在配置文件中添加Admin项");
        }

        ChainUser user_ = chainUserOptional.get();
        ChainUser superUsers = new ChainUser(
                user_.getType(),
                user_.getMobile(),
                user_.getUsername(),
                user_.getCreditCode(),
                user_.getJks().getPath(),
                user_.getJks().getPassword(),
                user_.getJks().getAlias());

        Map<String, ChainUser> normalUserMap = new HashMap<>();

        List<ChainUser> chainUsers = chainProperties.getUser().stream().filter(user -> !ADMIN.equals(user.getType())).collect(Collectors.toList());

        chainUsers.forEach(user -> {
            try {

                ChainUser normalUser = new ChainUser(
                        user.getType(),
                        user.getMobile(),
                        user.getUsername(),
                        user.getCreditCode(),
                        user.getJks().getPath(),
                        user.getJks().getPassword(),
                        user.getJks().getAlias());

                normalUserMap.put(normalUser.getUsername(), normalUser);

            } catch (IOException e) {
                throw new RuntimeException("Normal项加载失败，请检查配置文件！");
            }
        });

        if(normalUserMap.size() == 0 ) {
            throw new RuntimeException("请在配置文件中添加Normal项");
        }
        log.info("==========> 普通用户注入成功，共{}个", normalUserMap.size());

        return new ChainUserManager(superUsers, normalUserMap);

    }

    public ChainNet parseNet(String type) {

        Optional<ChainNet> chainNetOptional =  chainProperties.getNet().stream().filter(net -> type.equals(net.getType())).findFirst();

        if(!chainNetOptional.isPresent()) {
            throw new RuntimeException("请在配置文件中添加网络项");
        }

        ChainNet net = chainNetOptional.get();
        return new ChainNet(net.getType(), net.getHost(), net.getPrefix());
    }

    @Bean
//    @ConditionalOnMissingBean
    public ChainNetManager chainNetManager() {

        Map<String, ChainNet> map = Maps.newHashMap();
        map.put(IDENTITY, parseNet(IDENTITY));
        map.put(BIZ, parseNet(BIZ));

        return new ChainNetManager(map);
    }


    @Bean
//    @ConditionalOnMissingBean
    public ChainCodeIdManager chainCodeIdManager() {
        Map<String, ChainCode> chainCodeMap = new HashMap<>();

        List<ChainCode> chainCodeList = chainProperties.getChaincode();

        if(chainCodeList.size() == 0) {
            throw new RuntimeException("请在配置文件中添加合约信息");
        }

        chainCodeList.forEach(chainCode -> {
            chainCode.setChaincodeId(Peer.ChaincodeId.newBuilder().setChaincodeName(chainCode.getName()).setVersion(chainCode.getVersion()).build());
            chainCodeMap.put(chainCode.getName(), chainCode);
        });

        log.info("==========> 合约注入成功，共{}个", chainCodeList.size());
        return new ChainCodeIdManager(chainCodeMap);
    }

}




