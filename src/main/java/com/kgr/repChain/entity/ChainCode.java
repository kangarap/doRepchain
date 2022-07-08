package com.kgr.repChain.entity;

import com.rcjava.protos.Peer;
import lombok.Data;

/**
 * @author kgr
 * @create 2022-07-05 13:55
 */
@Data
public class ChainCode {
    String name;
    String path;
    Integer version;
    String[] func;
    Peer.ChaincodeId chaincodeId;

    public ChainCode(String name, String path, Integer version, String[] func) {
        this.name = name;
        this.path = path;
        this.version = version;
        this.func = func;
        this.chaincodeId = Peer.ChaincodeId.newBuilder().setChaincodeName(name).setVersion(version).build();

    }
}