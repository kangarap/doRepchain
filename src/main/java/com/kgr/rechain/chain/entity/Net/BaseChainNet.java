package com.kgr.rechain.chain.entity.Net;

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
public class BaseChainNet implements Serializable {
    String type;
    String host;
    String prefix;

    TranPostClient tranPostClient;
    ChainInfoClient chainInfoClient;

}
