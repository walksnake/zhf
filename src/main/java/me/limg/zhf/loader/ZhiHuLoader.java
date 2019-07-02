package me.limg.zhf.loader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class ZhiHuLoader {

    private static final Logger logger = LogManager.getLogger(ZhiHuLoader.class);
    private static final String UTF_8 = "UTF-8";
    private final static int TIMEOUT_CONNECTION = 200000;
    private final static int TIMEOUT_SOCKET = 200000;
    private static final HttpClient httpClient = getHttpClient();
    private static final String COOKIE_FILE         = "cookie.txt";

    private String rootPath;

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void loadFromUrl(String url){
        int i = url.indexOf("/question/");
        String qid = null, aid = null;
        if(i > 0){
            String sb = url.substring(i + 10);
            if(sb.indexOf("/") > 0) {
                sb = sb.substring(0, sb.indexOf("/"));
            }
            qid = sb;
        }

        i = 0;
        i = url.indexOf("/answer/");
        if(i > 0){
            String sb = url.substring(i + 8);
            aid = sb;
        }

        if(qid != null && aid != null){
            loadAnswer(qid, aid);
        }
        if(qid != null && aid == null){
            loadThread(qid);
        }
    }

    /**
     * 载入问题所有答案
     * @param questionId
     */
    public void loadThread(String questionId){
        logger.info("处理问题：" + questionId);

        List<String> list = loadAnswerIds(questionId);

        logger.info("共" + list.size() + "个回复");
        if(!list.isEmpty()){
            for(String aid : list){
                loadAnswer(questionId, aid);
            }
        }
    }

    private List<String> loadAnswerIds(String qid){
        int offset = 0;
        int limit = 20;
        List<String> list = new ArrayList<>();
        while(true){
            String url = "https://www.zhihu.com/api/v4/questions/" + qid + "/answers?offset=" + offset + "&limit=" + limit;
            String html = loadHtml(url);
            if(html == null){
                continue;
            }

            JSONObject jsonObject = JSONObject.parseObject(html);
            JSONArray jsonArray = (JSONArray) jsonObject.get("data");
            if(jsonArray == null || jsonArray.isEmpty()){
                break;
            }

            for(Object item : jsonArray){
                JSONObject j = (JSONObject)item;
                list.add(j.getString("id"));
            }

            offset += limit;
        }

        return list;
    }

    private JSONObject loadQuestionInfo(String qid){
        String url = "https://www.zhihu.com/api/v4/questions/" + qid;
        return (JSONObject) loadJson(url);
    }

    private String findQuestionDir(String questionId){
        File file = new File(rootPath);
        if(!ensurePathExist(file)){
            return null;
        }

        String[] children = file.list((dir, name) -> name.startsWith(questionId + "-"));

        if(children != null && children.length > 0){
            return rootPath + File.separator + children[0];
        }else{
            return null;
        }
    }

    private String ensureQuestionDirExist(String qid){
        String path = findQuestionDir(qid);
        if(StringUtils.isEmpty(path)){
            return mkQuestionDir(qid);
        }else{
            return path;
        }
    }

    private String mkQuestionDir(String qid) {
        JSONObject jsonObject = loadQuestionInfo(qid);
        if(jsonObject == null){
            return null;
        }

        String path = rootPath + File.separator + qid + "-" + jsonObject.getString("title");
        File file = new File(path);
        file.mkdirs();
        return path;
    }

    private boolean ensurePathExist(File file){
        return file.exists() && file.isDirectory();
    }

    private String loadHtml(String url){
        try {
            GetMethod getMethod = new GetMethod(url);
            String cookie = loadCookie();
            if(!StringUtils.isEmpty(cookie)) {
                getMethod.setRequestHeader("cookie", cookie);
            }

            int r = httpClient.executeMethod(getMethod);
            if(HttpStatus.SC_OK == r){
                return getMethod.getResponseBodyAsString();
            }else{
                return null;
            }
        }catch(Exception e){
            return null;
        }
    }

    private static String CK = null;
    private String loadCookie() {
        if(StringUtils.isEmpty(CK)){
            String ckpath = System.getProperty("user.dir") + File.separator + COOKIE_FILE;
            File file = new File(ckpath);
            if(!file.exists() || !file.isFile()){
                return null;
            }

            try {
                List<String> list = FileUtils.readLines(file, "UTF-8");
                if(list != null && !list.isEmpty()){
                    CK = list.get(0);
                }
            }catch(Exception e){

            }
        }

        return CK;
    }

    private JSON loadJson(String url){
        String html = loadHtml(url);
        if(StringUtils.isEmpty(html)){
            return null;
        }

        return (JSON) JSON.parse(html);
    }

    private String decodeUrl(String url){
        try{
            return URLDecoder.decode(url, "UTF-8");
        }catch (Exception e){
            logger.error(e.getMessage(), e);
            return url;
        }
    }

    public void loadAnswer(String questionId, String aid) {
        String url = "https://www.zhihu.com/question/" + questionId + "/answer/" + aid;
        try{
            logger.info("处理帖子：" + aid);
            String html = loadHtml(url);

            Elements answerElements = Jsoup.parse(html).select("body .Question-main .AnswerCard .QuestionAnswer-content");
            if(answerElements == null || answerElements.isEmpty()){
                return;
            }

            String qPath = ensureQuestionDirExist(questionId);
            if(StringUtils.isEmpty(qPath)){
                return;
            }

            loadAnswer(answerElements.get(0), aid, qPath);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }
    }

    private void loadAnswer(Element e, String aid, String path) {
        loadAnswerImages(e, aid, path);
        loadAnswerVedios(e, aid, path);
    }

    private void loadAnswerVedios(Element e, String aid, String path) {
        Elements iframes = e.select(".video-box");

        if(iframes == null || iframes.isEmpty()){
            return;
        }

        for(Element ifms : iframes){
            String url = ifms.attr("href");
            if(StringUtils.isEmpty(url)){
                continue;
            }

            if(url.startsWith("https://link.zhihu.com/?target=")){
                url = url.substring("https://link.zhihu.com/?target=".length());
                url = decodeUrl(url);
            }

            String videoId = getVideoId(url);

            loadAnswerVedio(videoId, aid, path);
        }
    }

    private String getVideoId(String url) {
        int i = url.lastIndexOf("/");
        return url.substring(i + 1);
    }

    private void loadAnswerVedio(String vid, String aid, String path) {
        String url = "https://lens.zhihu.com/api/v4/videos/" + vid;
        String json = loadHtml(url);
        JSONObject jsonObject = JSONObject.parseObject(json);

        String vUrl = jsonObject.getJSONObject("playlist").getJSONObject("LD").getString("play_url");
        String ft = jsonObject.getJSONObject("playlist").getJSONObject("LD").getString("format");

        String fn = DigestUtils.md5Hex(vUrl) + "." + ft;

        saveFile(vUrl, path, fn, aid);
    }

    private void loadAnswerImages(Element e, String aid, String path) {
        Elements figures = e.select("figure");
        if(figures == null || figures.isEmpty()){
            return;
        }

        for(Element f : figures){
            loadAnswerImage(f, aid, path);
        }
    }

    private void loadAnswerImage(Element f, String aid, String path) {
        String imageUrl = f.select("img").attr("data-original");
        if(StringUtils.isEmpty(imageUrl)){
            return;
        }

        saveFile(imageUrl, path, null, aid);
    }



    private void saveFile(String url, String path, String fn, String pre){
        makeDirExists(path);

        try{
            logger.info("下载文件：" + url);

            URL u = new URL(url);
            InputStream inputStream = u.openStream();

            String filename;
            if(StringUtils.isEmpty(fn)){
                filename = getFileName(url);
            }else{
                filename = fn;
            }

            if(!StringUtils.isEmpty(pre)) {
                filename = pre + "-" + filename;
            }

            String fp = path + File.separator + filename;
            File f = new File(fp);
            if(f.exists()){
                return;
            }

            OutputStream outputStream = new FileOutputStream(fp);
            IOUtils.copy(inputStream, outputStream);
            //inputStream.close();
            //outputStream.close();
            logger.info("载入文件完成：" + fp);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
        }
    }

    private String getFileName(String imageUrl) {
        int i = imageUrl.lastIndexOf("/");
        return imageUrl.substring(i + 1);
    }

    private void makeDirExists(String path) {
        File dir = new File(path);
        if(!dir.exists() || dir.isFile()){
            dir.mkdirs();
        }
    }

    private static HttpClient getHttpClient() {
        HttpClient httpClient = new HttpClient();
        // 设置 HttpClient 接收 Cookie,用与浏览器一样的策略
        httpClient.getParams().setCookiePolicy(
                CookiePolicy.BROWSER_COMPATIBILITY);
        // 设置 默认的超时重试处理策略
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler());
        // 设置 连接超时时间
        httpClient.getHttpConnectionManager().getParams()
                .setConnectionTimeout(TIMEOUT_CONNECTION);
        // 设置 读数据超时时间
        httpClient.getHttpConnectionManager().getParams()
                .setSoTimeout(TIMEOUT_SOCKET);
        // 设置 字符集
        httpClient.getParams().setContentCharset(UTF_8);
        return httpClient;
    }
}
