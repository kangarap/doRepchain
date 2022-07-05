package com.kgr.rechain.chain.config;

import com.rcjava.protos.Peer;
import lombok.Data;

import java.util.Map;

/**
 * @author kgr
 * @create 2022-07-05 13:55
 */
@Data
public class ChainCode {
    String name;
    String path;
    Integer version;
    Map<String, String > func;
    Peer.ChaincodeId chaincodeId;
}