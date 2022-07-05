package com.kgr.rechain.chain.entity.User;

import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import com.rcjava.util.PemUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Collections;

/**
 * @author kgr
 * @create 2022-07-01 17:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Slf4j
public class NormalUser extends BaseChainUser {

    private Peer.Signer signer;

    public NormalUser(String username, String mobile, String cerditCode, String jksPath, String password, String alias ) throws IOException {


        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File(jksPath),
                password,
                alias
        ).getPrivateKey();

        Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode(cerditCode).setCertName(username).build();

        TranCreator tranCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm(this.getSignAlgorithm()).build();

        // 读取cer证书字符串
        String certPem = PemUtil.toPemString(
                CertUtil.generateX509Cert(new File(jksPath), password,alias)
        );

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
                // 账号标识用 业务链:用户名
                .setCreditCode(cerditCode)
                .setMobile(mobile)
                .addAllAuthenticationCerts(Collections.singletonList(certProto))
                .setSignerValid(true)
                .build();


        this.setTranCreator(tranCreator);
        this.setCertId(certId);
        this.setPrivateKey(privateKey);
        this.setUserName(username);
        this.setMobile(mobile);
        this.setSigner(userSigner);

        log.info("==========> NormalUser，成功！");

    }

}
