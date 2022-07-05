package com.kgr.rechain.chain.entity.User;

import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.security.PrivateKey;

/**
 * @author kgr
 * @create 2022-07-01 15:05
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseChainUser implements Serializable {

    private final String signAlgorithm = "sha256withecdsa";
    private TranCreator tranCreator;
    private Peer.CertId certId;
    private PrivateKey privateKey;
    private String userName;
    private String mobile;

}
