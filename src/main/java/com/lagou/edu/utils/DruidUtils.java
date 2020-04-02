package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql:///springtest?useSSL=false&amp;useUnicode=true&amp;characterEncoding=UTF-8");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("1003");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
