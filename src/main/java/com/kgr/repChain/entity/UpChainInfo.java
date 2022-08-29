package com.kgr.repChain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author kgr
 * @create 2022-08-29 11:35
 */
@Data
@AllArgsConstructor
public class UpChainInfo {
    private String url;
    private String bid;
    private String networkingName;
    private String contractName;
    private String contractVersion;
    private String contractInstanceId;
    private String contractMethodName;
    private String contractParameters;

}
