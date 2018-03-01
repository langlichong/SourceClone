package com.huhu;


import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

/**
 * Created by hasee on 2018/2/28.
 */
public class JGitTest {

    public static void main(String[] args) throws IOException, GitAPIException {

//        gitClone("https://github.com/zdnet/shop.git",new File("D:/shop"));

     //   testPull();

        gitCheckout(new File("D:/shop"),"master");

    }


    public static void gitClone(String remoteUrl, File repoDir) {
        try {
            Git git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(repoDir)
                    .call();

            System.out.println("Cloning from " + remoteUrl + " to " + git.getRepository());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 拉取远程仓库内容到本地
     */
    public static void testPull() throws IOException, GitAPIException {

        UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider =new UsernamePasswordCredentialsProvider("username","password");
        //git仓库地址
        Git git = new Git(new FileRepository("D:/shop"+"/.git"));
        git.pull().setRemoteBranchName("master").
                setCredentialsProvider(usernamePasswordCredentialsProvider).call();
    }

    public static void gitCheckout(File repoDir, String version) {
        File RepoGitDir = new File(repoDir.getAbsolutePath() + "/.git");
        if (!RepoGitDir.exists()) {
            System.out.println("Error! Not Exists : " + RepoGitDir.getAbsolutePath());
        } else {
            Repository repo = null;
            try {
                repo = new FileRepository(RepoGitDir.getAbsolutePath());
                Git git = new Git(repo);
                CheckoutCommand checkout = git.checkout();
                checkout.setName(version);
                checkout.call();
                System.out.println("Checkout to " + version);

                PullCommand pullCmd = git.pull();
                pullCmd.call();

                System.out.println("Pulled from remote repository to local repository at " + repo.getDirectory());

            } catch (Exception e) {
                System.out.println(e.getMessage() + " : " + RepoGitDir.getAbsolutePath());
            } finally {
                if (repo != null) {
                    repo.close();
                }
            }
        }
    }
}
