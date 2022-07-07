package com.kgr.repChain.entity;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author kgr
 * @create 2022-07-05 13:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainNet implements Serializable {
    String type;
    String host;
    String prefix;

    TranPostClient tranPostClient;
    ChainInfoClient chainInfoClient;


    public ChainNet(String type, String host, String prefix) {
        this.type = type;
        this.prefix = prefix;
        this.host = host;
        this.tranPostClient = new TranPostClient(host);
        this.chainInfoClient = new ChainInfoClient(host);
    }

}
