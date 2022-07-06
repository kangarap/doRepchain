package com.kgr.rechain.chain.entity;

import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import com.rcjava.util.PemUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Collections;

/**
 * @author kgr
 * @create 2022-07-06 13:57
 */
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class ChainUser {
    private String mobile;
    private String type;
    private String username;
    private String creditCode;
    private Jks jks;

    private final String signAlgorithm = "sha256withecdsa";
    private TranCreator tranCreator;
    private Peer.CertId certId;
    private Peer.Signer signer;


    public ChainUser(String type, String mobile,  String username, String creditCode, String jksPath, String password, String alias) throws IOException {

        PrivateKey privateKey = genPrivateKey(jksPath, password, alias);
        Peer.CertId certId = genCertId(creditCode, username);
        TranCreator tranCreator = genTranCreator(privateKey);

        this.setType(type);
        this.setTranCreator(tranCreator);
        this.setCertId(certId);
        this.setUsername(username);

        if(!"admin".equals(type)) {
            // 读取cer证书字符串
            String certPem = PemUtil.toPemString(CertUtil.generateX509Cert(new File(jksPath), password,alias));

            // 构造账户证书标示
            Peer.Certificate certProto = Peer.Certificate.newBuilder()
                    .setCertificate(certPem)
                    .setAlgType(this.getSignAlgorithm())
                    .setCertValid(true)
                    .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                    .setId(certId)
                    .setCertHash(DigestUtils.sha256Hex(certPem.replaceAll("\r\n|\r|\n|\\s", "")))
                    .build();

            // 构造账户
            Peer.Signer userSigner = Peer.Signer.newBuilder()
                    .setName(username)
                    .setCreditCode(creditCode)
                    .setMobile(mobile)
                    .addAllAuthenticationCerts(Collections.singletonList(certProto))
                    .setSignerValid(true)
                    .build();

            this.setSigner(userSigner);
            this.setMobile(mobile);

        }
    }
    private PrivateKey genPrivateKey(String jksPath, String password, String alias) {
        return CertUtil.genX509CertPrivateKey(
                new File(jksPath),
                password,
                alias
        ).getPrivateKey();
    }
    private Peer.CertId genCertId(String creditCode, String username) {
        return Peer.CertId.newBuilder().setCreditCode(creditCode).setCertName(username).build();
    }

    private TranCreator genTranCreator(PrivateKey privateKey) {
        return TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm(this.getSignAlgorithm()).build();
    }
}