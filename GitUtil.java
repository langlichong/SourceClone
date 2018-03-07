package com.huhu.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import sun.misc.BASE64Decoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  GitHub RestAPI
 *  JGit java lib
 *  Gitlab
 */
public class GitUtil {


    public static void main(String[] args) throws IOException, GitAPIException {

        // cloneRemoteGitRepo("https://github.com/langlichong/ShiroTest.git","F:/anjing/projects/shiroTest1");

       // updateLocalRepoFromGit("F:/anjing/projects/shiroTest1","master");

        //test gitlab  https://gitlab.com/lianhuayu420/gitlab-first-prj.git


        //cloneRemoteGitRepo("https://gitlab.com/lianhuayu420/gitlab-first-prj.git","F:/anjing/projects/gitlab-2","");
        //updateLocalRepoFromGit("F:/anjing/projects/gitlab-1","master");
        //Git.open(new File("F:/anjing/projects/gitlab-1")).pull().setRemoteBranchName("brach001").call();
        updateLocalRepoFromGit("F:/anjing/projects/gitlab-2");
    }


    /**
     * Git 克隆远程仓库到本地
     * @param remoteGitUrl 远程仓库地址 ： https://github.com/langlichong/ShiroTest.git
     * @param localRepoPath  本地仓库目录——目录必须不存在
     * @param branchName  被克隆的分支名称 默认为master
     * @throws IOException
     * @throws GitAPIException
     */
    public static void cloneRemoteGitRepo(String remoteGitUrl,String localRepoPath,String branchName) throws IOException, GitAPIException {

        String branch = "master" ;
        if(StringUtils.isNotBlank(branch)){
            branch = branchName ;
        }
        File localDir = new File(localRepoPath);

        if(localDir.exists()){
            System.out.println("the local repo directory already exist :" + localRepoPath + "clone failed !!");
            return ;
        }

        // then clone
        System.out.println("will  clone branch" + branch + " from " + remoteGitUrl + " to " + localRepoPath);

        Git result = Git.cloneRepository()
                        .setURI(remoteGitUrl)
                        .setDirectory(localDir)
                        .setBranch(branch)
                        .call();

        System.out.println("Cloning Done : " + result.getRepository().getDirectory());

        result.close();

    }

    /**
     * 更新本地git仓库代码
     * @param localRepoPath 本地git仓库地址(必须是git仓库) ：F:/anjing/projects/shiroTest/.git
     */
    public static void updateLocalRepoFromGit(String localRepoPath) throws IOException, GitAPIException {

        System.out.println("start update :" + localRepoPath + " ^_^ ing ");
        Git.open(new File(localRepoPath)).pull().call();
        System.out.println("updating Done !! ");

    }



    /**
     * 根据GitHub archive API 获取仓库master分支源码 存储为zip格式
     * @param gitUrl  e.g. : https://github.com/ReactiveX/RxJava.git
     * @param branchName 要获取的远程分支的名称 默认拉取master分支
     * @param localDir zip文件在本地存储路径
     */
    public static void produceZipFileFromGitHub(String gitUrl,String branchName,String localDir) throws IOException {

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
            File repo = new File(localDir);
            if(!repo.exists()){
                repo.mkdirs();
            }

            File zipFile = new File(localDir + File.separator + repoName);
            System.out.println("download source  :" + zipFile.getCanonicalPath());

            FileOutputStream fos = new FileOutputStream(zipFile);
            fos.write(resp.body().bytes());
            fos.close();
            is.close();

            System.out.println(" Generating Done .... ");
        }
    }


    /**
     * 检出gitlab中项目的某个分支，检出格式为zip
     * e.g.   https://gitlab.com/api/v4/projects/5596775/repository/archive.zip
     * @param projectId  项目id  (必须)
     * @param branchName  分支名称或者sha值(可选) 默认值为master
     * @throws IOException
     */
    public static void cloneGitLabProjectByZip(String projectId,String branchName,String localDir) throws IOException {

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

            File f = new File(localDir.concat(File.separator).concat("gitlabRepo"));
            if(!f.exists()){
                f.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(new File(f.getCanonicalPath()+File.separator+fileName));
            fos.write(res.body().bytes());
            fos.close();
        }

    }

    private static Response exeURLRequest(String url) throws IOException {

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(url).build();
        Call call = client.newCall(req);
        Response resp = call.execute();

        return resp ;
    }


    /**
     *  该方法有待测试调整 暂时不可用
     * 利用GitHub RestAPI中tree部分递归api获取文件内容
     *
     */
    @Deprecated
    public static void downloadSourceByGitTreeAPIRecursive(String gitUrl,String localDir) throws IOException {

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
                File f = new File(localDir.concat("/").concat(repoName).concat("/").concat(name));
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
                        File f = new File(localDir.concat("/").concat(repoName).concat("/").concat(path1));
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

}
