package com.kgr.rechain.chain.entity.User;

import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.security.PrivateKey;

/**
 * @author kgr
 * @create 2022-07-01 21:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class SuperUser extends BaseChainUser {
    public SuperUser(String username, String creditCode, String jksPath, String password, String alias) {

        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File(jksPath),
                password,
                alias
        ).getPrivateKey();

        Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode(creditCode).setCertName(username).build();

        TranCreator tranCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm(this.getSignAlgorithm()).build();

        this.setTranCreator(tranCreator);
        this.setCertId(certId);
        this.setPrivateKey(privateKey);
        log.info("==========> SuperUser，成功！");
    }
}
