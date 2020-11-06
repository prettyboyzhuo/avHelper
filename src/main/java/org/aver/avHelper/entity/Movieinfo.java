package org.aver.avHelper.entity;

import lombok.Data;

/**
* @author zhm
* @date 2020/11/6 
*
*/   
@Data
public class Movieinfo {
    private Integer id;

    /**
    * 原文件名
    */
    private String originalName;

    /**
    * 新文件名(番号)
    */
    private String newName;

    /**
    * 影片在avbus的网址
    */
    private String webSite;

    /**
    * 标题
    */
    private String title;

    /**
    * 短标题
    */
    private String sorttitle;

    /**
    * 导演
    */
    private String director;

    /**
    * 年份
    */
    private String year;

    /**
    * 发行日期
    */
    private String releasedate;

    /**
    * 本地路径
    */
    private String localPath;
}