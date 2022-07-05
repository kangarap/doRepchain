package com.kgr.rechain.chain.entity.Net;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kgr
 * @create 2022-07-05 13:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class IdentityNet extends BaseChainNet{

    @Override
    public void setType(String type) {
        this.type = "identity-net";
    }

    public IdentityNet(String type, String prefix, String host) {
        this.setType(type);
        this.setPrefix(prefix);
        this.setHost(host);
        this.tranPostClient = new TranPostClient(host);
        this.chainInfoClient = new ChainInfoClient(host);

        log.info("==========> 身份链，成功！");

    }
}
