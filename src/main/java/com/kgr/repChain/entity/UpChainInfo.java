package com.kgr.repChain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kgr
 * @create 2022-08-29 11:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
