package com.kgr.repChain.core;

import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.kgr.repChain.entity.ChainCode;
import com.kgr.repChain.entity.ChainUser;
import com.rcjava.protos.Peer;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.tran.impl.InvokeTran;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.kgr.repChain.utils.Constants.*;

/**
 * @author kgr
 * @create 2022-07-04 09:48
 */
//@Component
@AllArgsConstructor
public class ChainService {

    private final ChainUserManager chainUserManager;
    private final ChainNetManager chainNetManager;
    private final ChainCodeIdManager chainCodeIdManager;


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
     * @param username yml中定义的用户名
     */
    public void signUpSigner(String username) throws Exception {

        genInvokeTran(
                JavaType.STEP_REGISTER_USER.getCode(),
                chainUserManager.admin(),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL"),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL").getFunc().get("signUpSigner"),
                JsonFormat.printer().print(chainUserManager.normalUser(username).getSigner())
        );
    }

    /**
     * 向身份链注册一下 部署合约的权限 ，如 credence-net:xxxTPL.deploy
     * @param chainCode  合约
     * @param description  描述。如：部署合约TestTPL的操作
     * @throws Exception   exception
     */
    public void signUpDeploy(ChainCode chainCode, String description) throws Exception {


        String fullName = chainNetManager.net(BIZ) + ":%s." + "deploy";

        Peer.Operate operate = genOperate(
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName()),
                description,
                chainUserManager.admin().getCertId().getCreditCode(),
                false
        );

        genInvokeTran(
                JavaType.STEP_REGISTER_OPERATE.getCode(),
                chainUserManager.admin(),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL"),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL").getFunc().get("signUpOperate"),
                JsonFormat.printer().print(operate));
    }


    /**
     * superAdmin授权给 业务链:用户名 可部署 xxxTPL 的权限"
     * @param username     用户名。yml中的用户名
     * @param chainCode  合约
     * @throws Exception   exception
     */
    public void grantOperate(String username, ChainCode chainCode) throws Exception {

        String fullName = chainNetManager.net(BIZ).getPrefix() + ":%s." + "deploy";

        // step4: superAdmin授权给usr0部署合约("credence-net:CredenceTPL.deploy")的权限，向<身份链>提交
        Peer.Authorize authorize = genAuthorize(
                chainNetManager.net(BIZ).getPrefix() + ":" + UUID.randomUUID(),
                chainUserManager.admin().getCertId().getCreditCode(),
                chainUserManager.normalUser(username).getCertId().getCreditCode(),
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName())
        );

        genInvokeTran(
                JavaType.STEP_GRANT_AUTH.getCode(),
                chainUserManager.admin(),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL"),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL").getFunc().get("grantOperate"),
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize)))
        );
    }


    /**
     * 有了向业务链部署TPL的权限，因此向业务链部署合约
     * @param username     用户名。yml中的用户名
     * @param tplPath      合约路径。如：/User/lihao/test/TestTPL.scala
     * @param chainCode  合约
     */
    public void deployContract(String username, String tplPath, ChainCode chainCode) throws Exception {
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
                .setCertId(chainUserManager.normalUser(username).getCertId())
                .setChaincodeId(chainCode.getChaincodeId())
                .setChaincodeDeploy(chaincodeDeploy)
                .build();

        Peer.Transaction transaction = chainUserManager.normalUser(username).getTranCreator().createDeployTran(deployTran);

        chainNetManager.net(BIZ).getTranPostClient().postSignedTran(transaction);

        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult transactionResult = chainNetManager.net(BIZ).getChainInfoClient().getTranResultByTranId(transaction.getId());

        Peer.ActionResult actionResult = transactionResult.getErr();

        if(actionResult.getCode() != 0) {
            throw new Exception(actionResult.getReason());
        }

    }

    /**
     * 有了向业务链部署TPL的权限，因此向业务链部署合约
     * @param username     用户名。yml中的用户名
     * @param chainCode      合约定义类
     */
    public void deployContract(String username, ChainCode chainCode) throws Exception {

        this.deployContract(username, chainCode.getPath(), chainCode);

    }

    /**
     * 合约部署者注册合约的某个方法
     * @param username    用户名。yml中的用户名
     * @param chainCode  合约
     * @param funcName     合约中的方法名。
     * @param description  描述。 如：TestTPL.createValue
     * @throws Exception   exception
     */
    public void signUpOperate(String username, ChainCode chainCode, String funcName, String description) throws Exception {

        String fullName = chainNetManager.net(BIZ).getPrefix() + ":%s.%s";

        // usr0注册合约A的某个方法的Operate成功，向身份链提交
        Peer.Operate operate = genOperate(
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName(), funcName),
                description,
                chainUserManager.normalUser(username).getCertId().getCreditCode(),
                true
        );

        genInvokeTran(
                JavaType.STEP_USER_REGISTER_FUNCTION.getCode(),
                chainUserManager.normalUser(username),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL"),
                chainCodeIdManager.chainCode("RdidOperateAuthorizeTPL").getFunc().get("signUpOperate"),
                JsonFormat.printer().print(operate)
        );
    }

    /**
     * 调用合约
     * @param username  用户名。yml中的用户名
     * @param chainCode  合约
     * @param funcName     合约中的方法名。
     * @param params       合约方法参数。 json字符串
     * @throws Exception   excception
     */
    public void userInvoke(String username, ChainCode chainCode, String funcName, String params) throws Exception {

        genInvokeTran(
                JavaType.STEP_USER_INVOKE.getCode(),
                chainUserManager.normalUser(username),
                chainCode,
                funcName,
                params
        );
    }


    /**
     *
     * @param code       操作类型
     * @param chainUser  操作用户
     * @param chainCode 操作合约
     * @param functionName 合约方法名称
     * @param params     操作合约方法参数 json格式
     */

    public void genInvokeTran(int code, ChainUser chainUser, ChainCode chainCode, String functionName, String params) throws Exception {
        String tranId =  UUID.randomUUID().toString();

        Peer.TransactionResult transactionResult;


        Peer.CertId certId = chainUser.getCertId();
        String username = chainUser.getUsername();

        Peer.ChaincodeInput chaincodeInput = Peer.ChaincodeInput.newBuilder()
                .setFunction(functionName)
                .addArgs(params)
                .build();
        InvokeTran invokeTran = InvokeTran.newBuilder()
                .setTxid(tranId)
                .setCertId(certId)
                .setChaincodeId(chainCode.getChaincodeId())
                .setChaincodeInput(chaincodeInput)
                .setGasLimit(0)
                .setOid("")
                .build();


        if(ADMIN.equals(chainUser.getType()) || code == JavaType.STEP_USER_REGISTER_FUNCTION.getCode()) {
            Peer.Transaction tran =
                    ADMIN.equals(chainUser.getType())
                            ? chainUserManager.admin().getTranCreator().createInvokeTran(invokeTran) : chainUserManager.normalUser(username).getTranCreator().createInvokeTran(invokeTran);
            chainNetManager.net(IDENTITY).getTranPostClient().postSignedTran(tran);
            TimeUnit.SECONDS.sleep(5);
            transactionResult = chainNetManager.net(IDENTITY).getChainInfoClient().getTranResultByTranId(tranId);

        } else
        {
            Peer.Transaction tran = chainUserManager.normalUser(username).getTranCreator().createInvokeTran(invokeTran);
            if(code == JavaType.STEP_USER_INVOKE.getCode()) {
                String tranHex = Hex.encodeHexString(tran.toByteArray());
                chainNetManager.net(BIZ).getTranPostClient().postSignedTran(tranHex);
            }
            else
            {
                chainNetManager.net(BIZ).getTranPostClient().postSignedTran(tran);
            }

            TimeUnit.SECONDS.sleep(5);
            transactionResult = chainNetManager.net(BIZ).getChainInfoClient().getTranResultByTranId(tranId);
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
