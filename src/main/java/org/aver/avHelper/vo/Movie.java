package org.aver.avHelper.vo;

import lombok.Data;

import java.util.List;

/**
 * 需要组织的电影信息
 */
@Data
public class Movie {
	/** 原文件名 */
	String originalName;

	/** 新文件名（番号） */
	String newName;

	/** 影片在avbus的网址 */
	String webSite;

	/** 标题 */
	String title;

	/** 短标题 */
	String sorttitle;

	/** 导演 */
	String director;

	/** 年份 */
	String year;

	/** 发行日期 */
	String releasedate;

	/** 风格 */
	List<String> genre;

	/** 演员名 */
	List<String> actorName;

	/** 工作室 */
	String studio;

	/** 系列 */
	String series;

	/** 海报图片地址 */
	String posterPicSite;

	/** 下载的海报是否需要裁剪 */
	Boolean posterNeedCut;
	
	/** 小海报图片地址 */
	String smallPosterPicSite;

	/** 影片截图地址 */
	List<String> fanartsPicSite;

	List<Movie> childrenMovies;


	
}
