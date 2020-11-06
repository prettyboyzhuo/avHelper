package org.aver.avHelper.mapper;

import org.aver.avHelper.entity.Movieinfo;

/**
* @author zhm
* @date 2020/11/6 
*
*/   
public interface MovieinfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Movieinfo record);

    int insertSelective(Movieinfo record);

    Movieinfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Movieinfo record);

    int updateByPrimaryKey(Movieinfo record);
}