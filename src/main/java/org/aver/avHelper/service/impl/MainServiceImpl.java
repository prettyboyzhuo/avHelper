package org.aver.avHelper.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.core.io.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.aver.avHelper.service.MainService;
import org.aver.avHelper.utils.HtmlDownload;
import org.aver.avHelper.utils.XmlHandler;
import org.aver.avHelper.vo.Config;
import org.aver.avHelper.vo.ConfigStatic;
import org.aver.avHelper.vo.Movie;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import static org.aver.avHelper.vo.ConfigStatic.javBusSite;

@Component
public class MainServiceImpl implements MainService {
	
	@Override
	public void addActorToNfo(File rootPath, String actorName) throws Exception {
		//1.拿取解析器Sample Api Xml
		SAXReader sax = new SAXReader();
		for (File file : rootPath.listFiles()) {
			if (file.isDirectory()) {
				addActorToNfo(file, actorName);
			}else if (StringUtils.endsWithIgnoreCase(file.getName(), ".nfo")) {
				//2.读取指定 的xml文档。将其封装在document对象中
				org.dom4j.Document document = sax.read(file);
				//3.拿取根元素<movie>
				org.dom4j.Element rootEle = document.getRootElement();
				//4.创建子元素
				org.dom4j.Element actEle = rootEle.addElement("actor");
				org.dom4j.Element actName = actEle.addElement("name");
				actName.setText(actorName);
				org.dom4j.Element actType = actEle.addElement("type");
				actType.setText("Actor");
				//6.设置输出流来生成一个xml文件
				OutputStream os = new FileOutputStream(file);
				//Format格式输出格式刷
				OutputFormat format = OutputFormat.createPrettyPrint();
				//设置xml编码
				format.setEncoding("utf-8");
		 
				//写：传递两个参数一个为输出流表示生成xml文件在哪里
				//另一个参数表示设置xml的格式
				XMLWriter xw = new XMLWriter(os,format);
				//将组合好的xml封装到已经创建好的document对象中，写出真实存在的xml文件中
				xw.write(document);
				//清空缓存关闭资源
				xw.flush();
				xw.close();
			}
		}
	}
	
	@Override
	public List<Movie> generateConfig(String usedSite) throws IOException {
		//所有文件名
		String[] names = new File(ConfigStatic.config.getTempMediaDir()).list();
		//用来保存可识别影片信息
		List<Movie> movieList = new ArrayList<Movie>();
		Movie movie = null;
		for (String name : names) {
			movie = new Movie();
			movie.setOriginalName(name);
			movie.setNewName(name);
			movie.setSorttitle(name.substring(0, name.lastIndexOf(".")));
			setWebSite(movie, usedSite, name.substring(0, name.lastIndexOf(".")));
			movieList.add(movie);
		}
		XmlHandler.generateMovies(movieList, ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName, true);
		return movieList;
	}

//	public static void main(String[] args) throws IOException {
//		File file = new File("Y:\\home\\Data");
//		String localDir = "D:\\moviebak";
////		new File(localDir +"\\"+ "data").mkdir();
////		System.out.println(file.getParent());
//		copyMirror(file, localDir);
//	}

	public static void print(File file, int level){
		for(int i = 0; i < level; ++i) {
			System.out.print("\t");//按层次输出\t
		}
		if(file.isDirectory()){
//			file.mkdir();
			System.out.println(file.getName() + "/");
		}else if(file.isFile()) {
			System.out.println(file.getName());
		}
		File[] fList = file.listFiles();//获取路径下所有目录和文件
		if(fList != null){
			for (File f:fList) {
				print(f, level+1);//递归
			}
		}
	}
	
	@Override
	public List<Movie> generateRenameConfig(String usedRule, String usedSite) throws IOException {
		//所有文件名
		String[] names = new File(ConfigStatic.config.getTempMediaDir()).list();
		//用来保存可识别影片信息
		List<Movie> movieList = new ArrayList<Movie>();
		//用来保存不可识别影片信息
		List<Movie> errMovieList = new ArrayList<Movie>();
		//key为番号，值为影片，用于判断一个电影是否拆分多个文件
		Map<String, Movie> map = new HashMap<String, Movie>();

		// 番号正则
		String nameRegEx = null;
		if ("rule1".equals(usedRule)) {
			nameRegEx = "[A-Za-z]+-[0-9]+";
		}else if ("rule2".equals(usedRule)) {
			nameRegEx = "[0-9]+[a-z]+[0-9]+";
		}else {
			throw new RuntimeException("重命名规则前后台对不上");
		}

		// 忽略大小写的写法
		Pattern namePattern = Pattern.compile(nameRegEx, Pattern.CASE_INSENSITIVE);
		
		//循环使用的临时变量
		Matcher nameMat = null;
		Movie movie = null;
		String movieExtName = null;
		// 根据正则生成新文件名
		for (String name : names) {
			movie = new Movie();
			movie.setOriginalName(name);

			// 影片扩展名
			movieExtName = name.substring(name.lastIndexOf(".") + 1);
			nameMat = namePattern.matcher(name.replaceAll("-", ""));
			// 影片名可以识别
			if (nameMat.find()) {
				String matString = nameMat.group();
				// 排除掉番号数字前面的0
				String[] nums = matString.split("[a-zA-Z]+");
				String lastnum = nums[1];
				// 没有0或数字个数不为5的，不做特殊处理
				while (lastnum.length() > 3 && lastnum.indexOf("0") == 0) {
					lastnum = lastnum.substring(1);
				}
				String shortName = nums[0] + (matString.replaceAll("[0-9]*", "") + "-" + lastnum).toUpperCase();
				
				//处理一个电影多个文件的情况
				Movie parentMovie = map.get(shortName);
				if(parentMovie == null){
					map.put(shortName, movie);
					movie.setSorttitle(shortName);
					setWebSite(movie, usedSite, shortName);
					movie.setNewName(shortName + "." + movieExtName);
					movieList.add(movie);
				}else{
					String parentName = parentMovie.getNewName();
					if(parentMovie.getChildrenMovies() == null){
						parentMovie.setChildrenMovies(new ArrayList<Movie>());
						parentMovie.setNewName(parentName.substring(0, parentName.lastIndexOf("."))
								+ ConfigStatic.rule[0] + parentName.substring(parentName.lastIndexOf(".")));
					}
					movie.setNewName(
							shortName + ConfigStatic.rule[parentMovie.getChildrenMovies().size() + 1]
									+ "." + movieExtName);
					parentMovie.getChildrenMovies().add(movie);
				}
			}
			//影片不能识别
			else{
				errMovieList.add(movie);
			}
		}

		movieList.addAll(errMovieList);
		XmlHandler.generateMovies(movieList, ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName, true);
		return movieList;
	}

	private void setWebSite(Movie movie, String usedSite, String shortName) {
		if ("javbus".equals(usedSite)) {
			movie.setWebSite(javBusSite + shortName);
		}else if ("mgstage".equals(usedSite)) {
			movie.setWebSite(ConfigStatic.mgstageSite + shortName);
		}else if ("fanza".equals(usedSite)) {
			String searchSite = ConfigStatic.fanzaSearchSite + shortName.replaceAll("-", "");
			Document document = HtmlDownload.getDocBySite(searchSite, null);
			Element listEle = document.selectFirst("#list");
			//查询到影片
			if (listEle != null) {
				List<String> sites = null;
				try {
					sites = FileUtils.readLines(new File("./fanza优先选择.txt"), "utf-8");
				} catch (IOException e) {
					e.printStackTrace();
				}
				//没有优先地址时直接使用列表中的第一个
				String link = listEle.selectFirst("li").selectFirst("a").attr("href");
				if (sites != null && sites.size() > 0) {
					Iterator<Element> liIt = listEle.select(">li").iterator();
					boolean flag = true;
					while (liIt.hasNext() && flag) {
						String curLink = liIt.next().selectFirst("a").attr("href");
						for (String site : sites) {
							if (StringUtils.startsWithIgnoreCase(curLink, site)) {
								link = curLink;
								flag = false;
								break;
							}
						}
					}
				}
				movie.setWebSite(link.split("\\?")[0]);
			}
		}else {
			throw new RuntimeException("刮削选项前后台对不上");
		}
	}
	
	@Override
	public void synchronizeNewName(List<Movie> movies) throws Exception {
		List<Movie> movieList = XmlHandler.readMovies(ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName);
		for (int i = 0; i < movieList.size(); i++) {
			if(!StringUtils.equals(movieList.get(i).getOriginalName(), movies.get(i).getOriginalName())){
				throw new RuntimeException("数据对不上，请重新生成改名配置");
			}
			String newName = movies.get(i).getNewName();
			String shortName = newName.substring(0, newName.lastIndexOf(".")).toUpperCase();
			String extendName = newName.substring(newName.lastIndexOf(".") + 1).toLowerCase();
			movieList.get(i).setNewName(shortName + "." + extendName);
			movieList.get(i).setSorttitle(shortName);
			movieList.get(i).setWebSite(javBusSite + shortName);
		}
		XmlHandler.generateMovies(movieList, ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName, false);
	}

	@Override
	public List<Movie> getMovieMessage() throws Exception {
		List<Movie> movieList = XmlHandler.readMovies(ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName);
		if (movieList == null) {
			throw new RuntimeException("请先生成配置文件");
		}

		// 多线程代码
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				ConfigStatic.config.getThreadSize(), ConfigStatic.config.getThreadSize(), 60000,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		CopyOnWriteArrayList<Movie> movieListForSite = new CopyOnWriteArrayList<Movie>();
		CopyOnWriteArrayList<Movie> errorMovieListForSite = new CopyOnWriteArrayList<Movie>();
		Runnable getMovieMsgTask = null;
		for (Movie movie : movieList) {
			// 没有网址的不处理
			if (StringUtils.isBlank(movie.getWebSite())) {
				errorMovieListForSite.add(movie);
				continue;
			}else if (movie.getWebSite().contains(javBusSite)) {
				getMovieMsgTask = new GetJavBusMovieMsgTask(movie, movieListForSite, errorMovieListForSite);
			}else if (movie.getWebSite().contains(ConfigStatic.mgstageSite)) {
				getMovieMsgTask = new GetMgstageMovieMsgTask(movie, movieListForSite, errorMovieListForSite);
			}else if (movie.getWebSite().contains(ConfigStatic.fanzaSite)) {
				getMovieMsgTask = new GetFanzaMovieMsgTask(movie, movieListForSite, errorMovieListForSite);
			}
			executor.execute(getMovieMsgTask);
		}
		executor.shutdown();

		while (true) {
			if (executor.isTerminated()) {
				movieListForSite.addAll(errorMovieListForSite);
				XmlHandler.generateMovies(
						movieListForSite, ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName, true);
				return movieListForSite;
			}
			Thread.sleep(1000);
		}
	}

	@Override
	public void reName() throws Exception {
		List<Movie> movieList = XmlHandler.readMovies(ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName);
		if (movieList == null) {
			throw new RuntimeException("请先生成配置文件");
		}
		
		File originalFile = null;
		File newFile = null;
		String tempMediaDir = ConfigStatic.config.getTempMediaDir();
		for (Movie movie : movieList) {
			originalFile = new File(tempMediaDir + "/" + movie.getOriginalName());
			newFile = new File(tempMediaDir + "/" + movie.getNewName());
			if (originalFile.exists()) {
				originalFile.renameTo(newFile);
			}
			if (movie.getChildrenMovies() != null && movie.getChildrenMovies().size() > 0) {
				for (Movie childMovie : movie.getChildrenMovies()) {
					originalFile = new File(tempMediaDir + "/" + childMovie.getOriginalName());
					newFile = new File(tempMediaDir + "/" + childMovie.getNewName());
					if (originalFile.exists()) {
						originalFile.renameTo(newFile);
					}
				}
			}
		}
	}

	@Override
	public void downLoadImg(String desDir) throws Exception {
		List<Movie> movieList = XmlHandler.readMovies(ConfigStatic.tempRootPath, ConfigStatic.moviesXmlName);
		if (movieList == null) {
			throw new RuntimeException("请先生成配置文件");
		}

		// 多线程代码
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				ConfigStatic.config.getThreadSize(),ConfigStatic.config.getThreadSize(), 60000,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		DownLoadImgTask downLoadImgTask = null;
		for (Movie movie : movieList) {
			downLoadImgTask = new DownLoadImgTask(movie, ConfigStatic.config.getThreadSize(), desDir);
			executor.execute(downLoadImgTask);
		}
		executor.shutdown();

		while (true) {
			if (executor.isTerminated()) {
				break;
			}
			Thread.sleep(1000);
		}

	}

	@Override
	public void moveFiles(String sourceDir, String desDir) {
		File sourceDirFile = new File(sourceDir);
		movieFile(sourceDirFile, desDir + "/");		
	}
	/**
	 * 移动所有大于20M的文件到指定文件夹
	 * @param sourceDir
	 * @param desDir
	 */
	private static void movieFile(File sourceDir, String desDir){
		File desFile = null;
		if(sourceDir.isDirectory()){
			for (File childFile : sourceDir.listFiles()) {
				movieFile(childFile, desDir);
			}
		}else{
			if (sourceDir.length() / 1024 / 1024 > 20) {
				desFile = new File(desDir + sourceDir.getName());
				sourceDir.renameTo(desFile);
			}
		}
		
	}

	@Override
	public void compareDir(String retainDir, String deleteDir) {
		Map<String, File> actorNames = new HashMap<String, File>();
		File dirForWatch = new File(retainDir);
		for (File file : dirForWatch.listFiles()) {
			actorNames.put(file.getName(), file);
		}
		
		File dirForPreview = new File(deleteDir);
		File watActorDir = null;
		for (File preActorDir : dirForPreview.listFiles()) {
			watActorDir = actorNames.get(preActorDir.getName());
			if(watActorDir != null){
				for (File preShortNameDir : preActorDir.listFiles()) {
					for (File watShortNameDir : watActorDir.listFiles()) {
						if(preShortNameDir.getName().equals(watShortNameDir.getName())){
							delteDir(preShortNameDir);
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param dir
	 */
	private static void delteDir(File dir){
		for (File file : dir.listFiles()) {
			if(file.isDirectory()){
				delteDir(file);
			} else {
				if (file.length() / 1024 / 1024 < 20) {
					if(!file.delete()){
						throw new RuntimeException("删除失败：" + file.getAbsolutePath());
					}
				}else{
					throw new RuntimeException("存在电影文件：" + file.getAbsolutePath());
				}
			}
		}
		dir.delete();
	}

	@Override
	public void generatePseudoVideo(String urlName) throws IOException{
		Config config = ConfigStatic.config;
		
		//请求参数
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
		properties.put("Cookie", "existmag=all");
		int i = 1;
		while (true) {
			if (config.getMaxDownloadNum() !=null && i > config.getMaxDownloadNum()) break;
			Document document = HtmlDownload.getDocBySite(urlName + "/" + i, properties);
			Elements imgEles = document.select("#waterfall a.movie-box");
			if (imgEles.size() == 0) break;
			Iterator<Element> imgElesIt = imgEles.iterator();
			while (imgElesIt.hasNext()) {
				Element movieBoxEle = imgElesIt.next();
				String picName = movieBoxEle.selectFirst("date").text() + ".wmv";
				File picFile = new File(config.getTempMediaDir() + "/" + picName);
				picFile.createNewFile();
			}
			i++;
		}
	}

	@Override
	public void renameFile() {
		//重命名规则
		String nameRegEx = "[A-Za-z]+-[0-9]+";
		String filename = "";
		Pattern p =Pattern.compile(nameRegEx);
		p.split("(NATURAL HIGH)(NHDTB-090)宙に浮くほどイキ跳ねる「エビ反り薬漬けエステ」10 全員中出し再会SPECIAL");
		boolean matches = filename.matches(nameRegEx);
		String localDir = "D:\\data\\1.txt";
		File file = new File(localDir);
		File file2 = new File("D:\\data\\2.txt");
		file.renameTo(file2);


	}

	@Override
	public Movie findMovie(String fanhao) {
		Document document = HtmlDownload.getDocBySite(javBusSite + "/search/" + fanhao, null);
		Element waterfall = document.getElementById("waterfall");
		//完整的视频地址
		String movieLink = waterfall.getElementsByTag("a").get(0).attributes().get("href");
		Movie movie = new Movie();
		String newName = waterfall.getElementsByTag("date").get(0).text();
		movie.setNewName(newName);
		movie.setWebSite(movieLink);
		Document document1 = HtmlDownload.getDocBySite(movieLink, null);
		Elements containerEles = document1.select(".container");

		//解析影片信息
		//标题
		movie.setTitle(containerEles.select("h3").text());

		//演员
		Elements actorEles = containerEles.select(".info .star-name");
		if(actorEles.size() > 0){
			Iterator<Element> actorElesIt = actorEles.iterator();
			List<String> actors = new ArrayList<String>();
			while(actorElesIt.hasNext()){
				actors.add(actorElesIt.next().select("a").text());
			}
			movie.setActorName(actors);
		}

		Elements infoEles = containerEles.select(".info>p");
		Element currentEle;
		Iterator<Element> infoEleIt = infoEles.iterator();
		while(infoEleIt.hasNext()){
			currentEle = infoEleIt.next();
			if(currentEle.text().contains("導演:")){
				movie.setDirector(currentEle.select("a").text());
			}else if(currentEle.text().contains("發行日期:")){
				String date = currentEle.text().replace("發行日期:", "").replaceAll(" ", "");
				movie.setReleasedate(date);
				movie.setYear(date.substring(0, date.indexOf("-")));
			}else if(currentEle.text().contains("類別:")){
				Elements genreEles = currentEle.nextElementSibling().select("a");
				List<String> genreList = new ArrayList<String>();
				Iterator<Element> genreElesIt = genreEles.iterator();
				while(genreElesIt.hasNext()){
					genreList.add(genreElesIt.next().text());
				}
				movie.setGenre(genreList);
			}else if(currentEle.text().contains("製作商:")){
				movie.setStudio(currentEle.select("a").text());
			}else if(currentEle.text().contains("系列:")){
				movie.setSeries(currentEle.select("a").text());
			}
		}

		//海报
		String posterPicSite = containerEles.select(".screencap a").attr("href");
		movie.setPosterPicSite(posterPicSite);
		movie.setSmallPosterPicSite(posterPicSite.replace("cover", "thumb").replace("_b", ""));
		movie.setPosterNeedCut(true);

		//影片截图
		Elements samplePicsEles = containerEles.select("#sample-waterfall a");
		if(samplePicsEles.size() > 0){
			Iterator<Element> samplePicsElesIt = samplePicsEles.iterator();
			List<String> samplePicList = new ArrayList<String>();
			while(samplePicsElesIt.hasNext()){
				samplePicList.add(samplePicsElesIt.next().attr("href"));
			}
			movie.setFanartsPicSite(samplePicList);
		}


		return movie;
	}

	public static void main(String[] args) {
		String nameRegEx = "[A-Za-z]+[0-9]+";
		String filename = "(NATURAL HIGH)(NHDTB-090)宙に浮くほどイキ跳ねる「エビ反り薬漬けエステ」10 全員中出し再会SPECIAL";
		String s = filename.replaceAll("-", "");
		System.out.println(s);
		Pattern p =Pattern.compile(nameRegEx);
		Matcher matcher = p.matcher(s);
		while (matcher.find()){
			System.out.println(matcher.group());
		}

	}

	/**
	 * 将磁盘中的电影目录结构在本地创建一份
	 * @param file
	 * @param localDir
	 * @throws IOException
	 */
//	public static void main(String[] args) throws IOException {
//		File file = new File("Y:\\home\\Data");
//		String localDir = "D:\\moviebak";
////		new File(localDir +"\\"+ "data").mkdir();
////		System.out.println(file.getParent());
//		copyMirror(file, localDir);
//	}


	public static void copyMirror(File file, String localDir) throws IOException {
		if (file.isDirectory()){
			System.out.println("创建文件夹" + localDir +"\\"+ file.getName());
			new File(localDir + "\\" + file.getName()).mkdir();
			localDir = localDir + "\\"+file.getName();
		}else if (file.isFile()){
			System.out.println("创建文件" + localDir +"\\"+ file.getName());
			new File(localDir +"\\"+ file.getName()).createNewFile();
		}
		File[] fList = file.listFiles();//获取路径下所有目录和文件
		if(fList != null){
			for (File f:fList) {
				copyMirror(f, localDir);//递归
			}
		}
	}



}
