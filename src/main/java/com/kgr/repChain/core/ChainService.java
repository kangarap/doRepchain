package com.kgr.repChain.core;

import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.kgr.repChain.entity.ChainCode;
import com.kgr.repChain.entity.ChainUser;
import com.rcjava.protos.Peer;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.tran.impl.InvokeTran;
import com.rcjava.util.CertUtil;
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
@AllArgsConstructor
public class ChainService {

    private final ChainNetManager chainNetManager;

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
     * @param superUser
     * @param normalUser
     * @param didChain
     * @throws Exception
     */
    public void signUpSigner(ChainUser superUser, ChainUser normalUser, ChainCode didChain) throws Exception {

        genInvokeTran(
                JavaType.STEP_REGISTER_USER.getCode(),
                superUser,
                didChain,
                "signUpSigner",
                JsonFormat.printer().print(normalUser.getSigner()
                )
        );
    }

    /**
     * 向身份链注册一下 部署合约的权限 ，如 credence-net:xxxTPL.deploy
     * @param superUser 管理员
     * @param chainCode 合约
     * @param didChain  系统合约
     * @param description
     * @throws Exception
     */
    public void signUpDeploy(ChainUser superUser, ChainCode chainCode, ChainCode didChain, String description) throws Exception {

        String fullName = chainNetManager.net(BIZ).getPrefix() + ":%s." + "deploy";

        Peer.Operate operate = genOperate(
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName()),
                description,
                superUser.getCertId().getCreditCode(),
                false
        );

        genInvokeTran(
                JavaType.STEP_REGISTER_OPERATE.getCode(),
                superUser,
                didChain,
                "signUpOperate",
                JsonFormat.printer().print(operate));
    }


    /**
     * superAdmin授权给 业务链:用户名 可部署 xxxTPL 的权限"
     * @param superUser  管理员
     * @param normalUser 用户
     * @param chainCode  合约
     * @param didChain   系统合约
     * @throws Exception
     */
    public void grantOperate(ChainUser superUser, ChainUser normalUser, ChainCode chainCode, ChainCode didChain) throws Exception {

        String fullName = chainNetManager.net(BIZ).getPrefix() + ":%s." + "deploy";

        // step4: superAdmin授权给usr0部署合约("credence-net:CredenceTPL.deploy")的权限，向<身份链>提交
        Peer.Authorize authorize = genAuthorize(
                chainNetManager.net(BIZ).getPrefix() + ":" + UUID.randomUUID(),
                superUser.getCertId().getCreditCode(),
                normalUser.getCertId().getCreditCode(),
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName())
        );

        genInvokeTran(
                JavaType.STEP_GRANT_AUTH.getCode(),
                superUser,
                didChain,
                "grantOperate",
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize)))
        );
    }


    /**
     * 有了向业务链部署TPL的权限，因此向业务链部署合约
     * @param normalUser
     * @param chainCode
     * @throws Exception
     */
    public void deployContract(ChainUser normalUser, ChainCode chainCode) throws Exception {
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

        chainNetManager.net(BIZ).getTranPostClient().postSignedTran(transaction);

        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult transactionResult = chainNetManager.net(BIZ).getChainInfoClient().getTranResultByTranId(transaction.getId());

        Peer.ActionResult actionResult = transactionResult.getErr();

        if(actionResult.getCode() != 0) {
            throw new Exception(actionResult.getReason());
        }

    }


    /**
     * 合约部署者注册合约的某个方法
     * @param normalUser
     * @param chainCode
     * @param funcName
     * @param description
     * @param didCode
     * @throws Exception
     */
    public void signUpOperate(ChainUser normalUser, ChainCode chainCode, String funcName, String description, ChainCode didCode) throws Exception {

        String fullName = chainNetManager.net(BIZ).getPrefix() + ":%s.%s";

        // usr0注册合约A的某个方法的Operate成功，向身份链提交
        Peer.Operate operate = genOperate(
                String.format(fullName, chainCode.getChaincodeId().getChaincodeName(), funcName),
                description,
                normalUser.getCertId().getCreditCode(),
                true
        );

        genInvokeTran(
                JavaType.STEP_USER_REGISTER_FUNCTION.getCode(),
                normalUser,
                didCode,
                "signUpOperate",
                JsonFormat.printer().print(operate)
        );
    }

    /**
     * 调用合约
     * @param normalUser 用户
     * @param chainCode  合约
     * @param funcName   合约方法
     * @param params json字符串
     * @throws Exception
     * @return 区块链交易id
     */
    public String userInvoke(ChainUser normalUser, ChainCode chainCode, String funcName, String params) throws Exception {

        return genInvokeTran(
                JavaType.STEP_USER_INVOKE.getCode(),
                normalUser,
                chainCode,
                funcName,
                params
        );
    }


    /**
     * 根据 alias，password 创建jks文件
     * @param saveFile 保存文件名 如 /Users/lihao/Desktop/RCJava-core/test.jks
     * @param alias    别名 相当于jks账户
     * @param password 密码
     * @throws Exception 异常
     */
    public void createJks(String saveFile, String alias, String password) throws Exception {

        CertUtil.genJksFile(new File(saveFile), alias, password);
    }


    /**
     *
     * @param code       操作类型
     * @param chainUser  操作用户
     * @param chainCode 操作合约
     * @param functionName 合约方法名称
     * @param params     操作合约方法参数 json格式
     * @return String    区块链交易id
     */
    private String genInvokeTran(int code, ChainUser chainUser, ChainCode chainCode, String functionName, String params) throws Exception {
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
            Peer.Transaction tran = chainUser.getTranCreator().createInvokeTran(invokeTran);
            chainNetManager.net(IDENTITY).getTranPostClient().postSignedTran(tran);
            TimeUnit.SECONDS.sleep(5);
            transactionResult = chainNetManager.net(IDENTITY).getChainInfoClient().getTranResultByTranId(tranId);

        } else
        {
            Peer.Transaction tran = chainUser.getTranCreator().createInvokeTran(invokeTran);
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

        return tranId;
    }



    private Peer.Operate genOperate (String authFullName, String description, String register, boolean isPublish) {
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
    private Peer.Authorize genAuthorize(String id, String grantBy, String granted, String opId) {

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
