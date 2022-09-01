package com.kgr.repChain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author kgr
 * @create 2022-07-06 15:57
 */
@Data
@AllArgsConstructor
public class Jks {
    private String path;
    private String password;
    private String alias;
}
