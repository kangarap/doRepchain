package com.kgr.repChain.config;

import com.google.common.collect.Maps;
import com.kgr.repChain.core.ChainNetManager;
import com.kgr.repChain.core.ChainService;
import com.kgr.repChain.entity.ChainNet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;

import static com.kgr.repChain.utils.Constants.BIZ;
import static com.kgr.repChain.utils.Constants.IDENTITY;

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


    public ChainNet parseNet(String type) {

        Optional<ChainNet> chainNetOptional =  chainProperties.getNet().stream().filter(net -> type.equals(net.getType())).findFirst();

        if(!chainNetOptional.isPresent()) {
            throw new RuntimeException("请在配置文件中添加网络项");
        }

        ChainNet net = chainNetOptional.get();
        return new ChainNet(net.getType(), net.getHost(), net.getPrefix());
    }

    @Bean
    public ChainNetManager chainNetManager() {

        Map<String, ChainNet> map = Maps.newHashMap();
        map.put(IDENTITY, parseNet(IDENTITY));
        map.put(BIZ, parseNet(BIZ));

        return new ChainNetManager(map);
    }


    @Bean
    public ChainService chainService(ChainNetManager chainNetManager) {
        return new ChainService(chainNetManager);
    }
}




