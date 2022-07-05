package com.kgr.rechain.chain;

import com.alibaba.fastjson2.JSONObject;
import com.kgr.rechain.chain.config.ChainCode;
import com.kgr.rechain.chain.entity.Net.BizNet;
import com.kgr.rechain.chain.entity.Net.IdentityNet;
import com.kgr.rechain.chain.entity.User.NormalUser;
import com.kgr.rechain.chain.entity.User.SuperUser;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.protos.Peer;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.tran.impl.InvokeTran;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author kgr
 * @create 2022-07-04 09:48
 */
@Component
public class ChainUtils {

    @Resource
    private NormalUser normalUser;

    @Resource
    private SuperUser superUser;

    @Resource
    BizNet bizNet;
    @Resource
    IdentityNet identityNet;

    @Resource
    Map<String, ChainCode> chaincodeMap;


    /**
     * 1～6 就是新用户从注册到最后调用合约代码的过程
     * 这个枚举其实用处不大，只是为了区分5，6和其他步骤，5，6需要特殊处理
     */
    @AllArgsConstructor
    @Getter
    @SuppressWarnings("all")
    enum JavaType {
        STEP_REGISTER_USER(1, "SuperAdmin向身份链去注册用户"),
        STEP_REGISTER_OPERATE(2, "superAdmin注册可以操作合约权限"),
        STEP_GRANT_AUTH(3, "superAdmin授权给业务链用户授予权限"),
        STEP_USER_DEPLOY(4, "业务链用户部署合约"),
        STEP_USER_REGISTER_FUNCTION(5, "业务链用户注册合约中方法"),
        STEP_USER_INVOKE(6, "调用业务链中的合约");

        final int code;
        final String intro;

    }


    /**
     * 向身份链注册新用户
     */
    public void signUpSigner() throws Exception {

        genInvokeTran(
                JavaType.STEP_REGISTER_USER.getCode(),
                superUser.getCertId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getChaincodeId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getFunc().get("signUpSigner"),
                JsonFormat.printer().print(normalUser.getSigner())
        );
    }

    /**
     * 向身份链注册一下 部署合约的权限 ，如 credence-net:xxxTPL.deploy
     * @param chaincodeId  合约。在配置文件中提前定义好
     * @param description  描述。如：部署合约TestTPL的操作
     * @throws Exception   exception
     */
    public void signUpDeploy(Peer.ChaincodeId chaincodeId, String description) throws Exception {


        String fullName = bizNet.getPrefix() + ":%s." + "deploy";

        Peer.Operate operate = genOperate(
                String.format(fullName, chaincodeId.getChaincodeName()),
                description,
                superUser.getCertId().getCreditCode(),
                false
        );

        genInvokeTran(
                JavaType.STEP_REGISTER_OPERATE.getCode(),
                superUser.getCertId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getChaincodeId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getFunc().get("signUpOperate"),
                JsonFormat.printer().print(operate));
    }


    /**
     * superAdmin授权给 业务链:用户名 可部署 xxxTPL 的权限"
     * @param chaincodeId  合约。在配置文件中提前定义好
     * @throws Exception   exception
     */
    public void grantOperate(Peer.ChaincodeId chaincodeId) throws Exception {

        String fullName = bizNet.getPrefix() + ":%s." + "deploy";

        // step4: superAdmin授权给usr0部署合约("credence-net:CredenceTPL.deploy")的权限，向<身份链>提交
        Peer.Authorize authorize = genAuthorize(
                bizNet.getPrefix() + ":" + UUID.randomUUID(),
                superUser.getCertId().getCreditCode(),
                normalUser.getCertId().getCreditCode(),
                String.format(fullName, chaincodeId.getChaincodeName())
        );

        genInvokeTran(
                JavaType.STEP_GRANT_AUTH.getCode(),
                superUser.getCertId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getChaincodeId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getFunc().get("grantOperate"),
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize)))
        );
    }


    /**
     * 有了向业务链部署TPL的权限，因此向业务链部署合约
     * @param tplPath      合约路径。如：/User/lihao/test/TestTPL.scala
     * @param chaincodeId  合约。在配置文件中提前定义好
     */
    public void deployContract(String tplPath, Peer.ChaincodeId chaincodeId) throws Exception {
        // 向业务链提交，部署合约
        String tplString = FileUtils.readFileToString(new File(tplPath), StandardCharsets.UTF_8);

        Peer.ChaincodeDeploy chaincodeDeploy = Peer.ChaincodeDeploy.newBuilder()
                .setTimeout(5000)
                .setCodePackage(tplString)
                .setLegalProse("")
                .setCType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
                .setRType(Peer.ChaincodeDeploy.RunType.RUN_SERIAL)
                .setSType(Peer.ChaincodeDeploy.StateType.STATE_BLOCK)
                .setInitParameter("")
                .setCclassification(Peer.ChaincodeDeploy.ContractClassification.CONTRACT_CUSTOM)
                .build();
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(DigestUtils.sha256Hex(tplString))
                .setCertId(normalUser.getCertId())
                .setChaincodeId(chaincodeId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();

        Peer.Transaction transaction = normalUser.getTranCreator().createDeployTran(deployTran);

        bizNet.getTranPostClient().postSignedTran(transaction);

        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult transactionResult = bizNet.getChainInfoClient().getTranResultByTranId(transaction.getId());

        Peer.ActionResult actionResult = transactionResult.getErr();

        if(actionResult.getCode() != 0) {
            throw new Exception(actionResult.getReason());
        }

    }

    /**
     * 有了向业务链部署TPL的权限，因此向业务链部署合约
     * @param chainCode      合约定义类
     */
    public void deployContract(ChainCode chainCode) throws Exception {
        // 向业务链提交，部署合约
        String tplString = FileUtils.readFileToString(new File(chainCode.getPath()), StandardCharsets.UTF_8);

        Peer.ChaincodeDeploy chaincodeDeploy = Peer.ChaincodeDeploy.newBuilder()
                .setTimeout(5000)
                .setCodePackage(tplString)
                .setLegalProse("")
                .setCType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
                .setRType(Peer.ChaincodeDeploy.RunType.RUN_SERIAL)
                .setSType(Peer.ChaincodeDeploy.StateType.STATE_BLOCK)
                .setInitParameter("")
                .setCclassification(Peer.ChaincodeDeploy.ContractClassification.CONTRACT_CUSTOM)
                .build();
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(DigestUtils.sha256Hex(tplString))
                .setCertId(normalUser.getCertId())
                .setChaincodeId(chainCode.getChaincodeId())
                .setChaincodeDeploy(chaincodeDeploy)
                .build();

        Peer.Transaction transaction = normalUser.getTranCreator().createDeployTran(deployTran);

        bizNet.getTranPostClient().postSignedTran(transaction);

        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult transactionResult = bizNet.getChainInfoClient().getTranResultByTranId(transaction.getId());

        Peer.ActionResult actionResult = transactionResult.getErr();

        if(actionResult.getCode() != 0) {
            throw new Exception(actionResult.getReason());
        }

    }

    /**
     * 合约部署者注册合约的某个方法
     * @param chainCodeId  合约。在配置文件中提前定义好
     * @param funcName     合约中的方法名。
     * @param description  描述。 如：TestTPL.createValue
     * @throws Exception   exception
     */
    public void signUpOperate(Peer.ChaincodeId chainCodeId, String funcName, String description) throws Exception {

        String fullName = bizNet.getPrefix() + ":%s.%s";

        // usr0注册合约A的某个方法的Operate成功，向身份链提交
        Peer.Operate operate = genOperate(
                String.format(fullName, chainCodeId.getChaincodeName(), funcName),
                description,
                normalUser.getCertId().getCreditCode(),
                true
        );

        genInvokeTran(
                JavaType.STEP_USER_REGISTER_FUNCTION.getCode(),
                normalUser.getCertId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getChaincodeId(),
                chaincodeMap.get("RdidOperateAuthorizeTPL").getFunc().get("signUpOperate"),
                JsonFormat.printer().print(operate)
        );
    }

    /**
     * 调用合约
     * @param chaincodeId  合约。在配置文件中提前定义好
     * @param funcName     合约中的方法名。
     * @param params       合约方法参数。 json字符串
     * @throws Exception   excception
     */
    public void userInvoke(Peer.ChaincodeId chaincodeId, String funcName, String params) throws Exception {

        genInvokeTran(
                JavaType.STEP_USER_INVOKE.getCode(),
                normalUser.getCertId(),
                chaincodeId,
                funcName,
                params
        );
    }


    /**
     *
     * @param code       操作类型
     * @param certId     操作人证书
     * @param chaincodeId 操作合约
     * @param functionName 合约方法名称
     * @param params     操作合约方法参数 json格式
     */

    public void genInvokeTran(int code, Peer.CertId certId, Peer.ChaincodeId chaincodeId, String functionName, String params) throws Exception {
        String tranId =  UUID.randomUUID().toString();

        Peer.TransactionResult transactionResult;

        Peer.ChaincodeInput chaincodeInput = Peer.ChaincodeInput.newBuilder()
                .setFunction(functionName)
                .addArgs(params)
                .build();
        InvokeTran invokeTran = InvokeTran.newBuilder()
                .setTxid(tranId)
                .setCertId(certId)
                .setChaincodeId(chaincodeId)
                .setChaincodeInput(chaincodeInput)
                .setGasLimit(0)
                .setOid("")
                .build();

        if("super_admin".equals(certId.getCertName()) || code == JavaType.STEP_USER_REGISTER_FUNCTION.getCode()) {
            Peer.Transaction tran =
                    "super_admin".equals(certId.getCertName())
                            ? superUser.getTranCreator().createInvokeTran(invokeTran) : normalUser.getTranCreator().createInvokeTran(invokeTran);
            identityNet.getTranPostClient().postSignedTran(tran);
            TimeUnit.SECONDS.sleep(5);
            transactionResult = identityNet.getChainInfoClient().getTranResultByTranId(tranId);

        } else
        {
            Peer.Transaction tran = normalUser.getTranCreator().createInvokeTran(invokeTran);
            if(code == JavaType.STEP_USER_INVOKE.getCode()) {
                String tranHex = Hex.encodeHexString(tran.toByteArray());
                bizNet.getTranPostClient().postSignedTran(tranHex);
            }
            else
            {
                bizNet.getTranPostClient().postSignedTran(tran);
            }

            TimeUnit.SECONDS.sleep(5);
            transactionResult = bizNet.getChainInfoClient().getTranResultByTranId(tranId);
        }

        Peer.ActionResult actionResult = transactionResult.getErr();

        if(actionResult.getCode() != 0) {
            throw new Exception("出错了：" + actionResult.getReason());
        }

    }


    public Peer.Operate genOperate (String authFullName, String description, String register, boolean isPublish) {
        long millis = System.currentTimeMillis();
        return Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex(authFullName))
                .setDescription(description)
                .setRegister(register)
                .setIsPublish(isPublish)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName(authFullName)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
    }


    /**
     *
     * @param id    "credence-net:" + UUID.randomUUID()
     * @param grantBy identity-net:951002007l78123233
     * @param granted credence-net:usr-0
     * @param opId  credence-net:CredenceTPL.deploy
     */
    public Peer.Authorize genAuthorize(String id, String grantBy, String granted, String opId) {

        long millis = System.currentTimeMillis();

        return Peer.Authorize.newBuilder()
                .setId(id)
                .setGrant(grantBy)  // 授权者，这里是superAdmin
                .addGranted(granted)   // 被授权者
                .addOpId(DigestUtils.sha256Hex(opId))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY) // 代表可以继续授权给别人
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
    }
}
