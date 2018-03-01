package com.huhu;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import sun.misc.BASE64Decoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hasee on 2018/2/27.
 */
public class SourceCloneUtil {

    private static final String CLONE_REPO_DIR = "D:/repo" ;
    private static final String GITHUB_ARCHIVE_URL_PART = "/zipball/master";


    public static void main(String[] args) throws IOException, SVNException, ZipException {

         //produceZipFileFromGitHub("https://github.com/langlichong/ShiroTest.git","");
         //produceZipFileFromGitHub("https://github.com/langlichong/NettyWebSocketDemo.git","branch010");

        // cloneGitLabProjectByZip("5596775","brach001");

         checkOutFromSvn("https://huhu:8443/svn/taotao-2017/trunk","huhu","huhu");

        // downloadSourceByGitTreeAPIRecursive("https://github.com/langlichong/ShiroTest.git");

       /* ZipFile zf = new ZipFile("");
        zf.extractAll();*/

    }


    /**
     * 根据GitHub archive API 获取仓库master分支源码
     * @param gitUrl  e.g. : https://github.com/ReactiveX/RxJava.git
     */
    public static void produceZipFileFromGitHub(String gitUrl,String branchName) throws IOException {

        String repoName = "" ;
        String fixUrl = "" ;
        String urlBranchParam = "/zipball/master" ; // 默认拉取master分支

        if(StringUtils.isNotBlank(gitUrl) && gitUrl.endsWith(".git")){

            repoName = gitUrl.substring(gitUrl.lastIndexOf('/')+1,gitUrl.length()-4).concat(".zip");
            System.out.println("repoName : " + repoName);
            if(StringUtils.isNotBlank(branchName)){
                urlBranchParam = "/zipball/".concat(branchName);
            }
            fixUrl = gitUrl.replace(".git",urlBranchParam);
            System.out.println("github archive url : " + fixUrl);
        }
        if(StringUtils.isNotBlank(fixUrl)){

            Response resp = exeURLRequest(fixUrl);

            InputStream is = resp.body().byteStream();
            File repo = new File(CLONE_REPO_DIR);
            if(!repo.exists()){
                repo.mkdirs();
            }

            File zipFile = new File(CLONE_REPO_DIR + File.separator + repoName);
            System.out.println("download source  :" + zipFile.getCanonicalPath());

            FileOutputStream fos = new FileOutputStream(zipFile);
            fos.write(resp.body().bytes());
//            int len = -1 ;
//            while((len = is.read()) != -1){
//                fos.write(len);
//            }
            fos.close();
            is.close();
        }
    }


    /**
     *
     * 利用github 递归 api 获取文件内容
     *
     */
    @Deprecated
    public static void downloadSourceByGitTreeAPIRecursive(String gitUrl) throws IOException {

        String repoName = gitUrl.substring(gitUrl.lastIndexOf("/")).replace(".git","");
        System.out.println("repository name is :" + repoName);

        //  https://api.github.com/repos/langlichong/ShiroTest/contents/
        String url = gitUrl.replace(".git","/contents/");
        System.out.println("content url :" + url);
        url = "https://api.github.com/repos/langlichong/ShiroTest/contents/";
        Response resp = exeURLRequest(url);

        JSONArray contentArr = JSONArray.parseArray(resp.body().string());
        for(int i=0;i<contentArr.size();i++){
            JSONObject jo = contentArr.getJSONObject(i);
            String type = jo.getString("type");
            String name = jo.getString("name");

            if("file".equalsIgnoreCase(type)){
                String fContentUrl = jo.getString("url");
                Response fileContent = exeURLRequest(fContentUrl);
                File f = new File(CLONE_REPO_DIR.concat("/").concat(repoName).concat("/").concat(name));
                if(!f.exists()){
                   f.createNewFile();
                }
                String fileRawContent = new String(new BASE64Decoder().decodeBuffer(fileContent.body().string()),"UTF-8");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(fileRawContent.getBytes(Charset.forName("UTF-8")));
                fos.close();

            }else if("dir".equalsIgnoreCase(type)){
                String sha = jo.getString("sha"); // 准备递归获取其中文件列表信息
                // GET /repos/:owner/:repo/git/trees/:sha?recursive=1
                String urlCom = "/git/trees/"+sha+ "?recursive=20";
                String recursiveUrl = gitUrl.replace(".git",urlCom);
                System.out.println("recursiveUrl :" + recursiveUrl);

                Response resp1 = exeURLRequest(recursiveUrl);

                JSONObject treeJo = JSONObject.parseObject(resp1.body().string());
                JSONArray treeArr = treeJo.getJSONArray("tree");
                for(int j=0;j<treeArr.size();j++){

                    JSONObject item = treeArr.getJSONObject(j);
                    String type1 = item.getString("type");
                    if("blob".equalsIgnoreCase(type1)){
                        String path1 = item.getString("path");
                        String contentUrl = item.getString("url");

                        //获取文件具体内容
                        Response fileContent = exeURLRequest(contentUrl);
                        String fileRawContent = new String(new BASE64Decoder().decodeBuffer(fileContent.body().string()),"UTF-8");
                        File f = new File(CLONE_REPO_DIR.concat("/").concat(repoName).concat("/").concat(path1));
                        if(!f.exists()){
                            f.createNewFile();
                        }
                        System.out.println("will create file :" + f.getCanonicalPath());
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(fileRawContent.getBytes(Charset.forName("UTF-8")));
                        fos.close();
                    }
                }
            }
        }

    }

    /**
     * 检出gitlab中项目的某个分支，检出格式为zip
     * e.g.   https://gitlab.com/api/v4/projects/5596775/repository/archive.zip
     * @param projectId  项目id  (必须)
     * @param branchName  分支名称或者sha值(可选) 默认值为master
     * @throws IOException
     */
    public static void cloneGitLabProjectByZip(String projectId,String branchName) throws IOException {

        String urlPart1 = "https://gitlab.com/api/v4/projects/" ;
        String urlPart2 = "/repository/archive.zip";
        if(StringUtils.isNotBlank(projectId)){

            if(StringUtils.isNotBlank(branchName)){
                urlPart2 = urlPart2.concat("?sha=").concat(branchName);
            }else{
                urlPart2 = urlPart2.concat("?sha=master");
            }

            Response res = exeURLRequest( urlPart1.concat(projectId).concat(urlPart2));

            String fileName = new SimpleDateFormat("yyyyHHmmSS").format(new Date()).concat(".zip");

            File f = new File(CLONE_REPO_DIR.concat(File.separator).concat("gitlabRepo"));
            if(!f.exists()){
               f.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(new File(f.getCanonicalPath()+File.separator+fileName));
            fos.write(res.body().bytes());
            fos.close();
        }

    }

    /**
     * 从svn检出源码
     */
    public static void checkOutFromSvn(String svnUrlStr,String uName,String pass) throws SVNException {


        SVNRepository repository = null ;

        SVNURL svnurl = SVNURL.parseURIEncoded(svnUrlStr);

        if(svnUrlStr.startsWith("svn")){ //svn://

            SVNRepositoryFactoryImpl.setup();
            repository = SVNRepositoryFactoryImpl.create(svnurl);

        }else if(svnUrlStr.startsWith("http")){ http://

            DAVRepositoryFactory.setup();
            repository = DAVRepositoryFactory.create(svnurl);

        }else if(svnUrlStr.startsWith("file")){ // file:///

            FSRepositoryFactory.setup();
            repository = FSRepositoryFactory.create(svnurl);
        }


        ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(uName,pass.toCharArray());

        repository.setAuthenticationManager(authenticationManager);

        DefaultSVNOptions defaultSVNOptions = SVNWCUtil.createDefaultOptions(true);

        SVNClientManager clientManager = SVNClientManager.newInstance(defaultSVNOptions,authenticationManager);

        SVNUpdateClient updateClient = clientManager.getUpdateClient();

        updateClient.setIgnoreExternals(false);

        File wcFile = new File("D:/repo");

        updateClient.doCheckout(svnurl,wcFile, SVNRevision.HEAD,SVNRevision.HEAD, SVNDepth.INFINITY,false);

    }

    private static Response exeURLRequest(String url) throws IOException {

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(url).build();
        Call call = client.newCall(req);
        Response resp = call.execute();

        return resp ;
    }


}
