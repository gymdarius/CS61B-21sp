package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    static final File STAGING_DIR = join(GITLET_DIR, "staging");
    static final File INDEX_FILE = join(STAGING_DIR, "index");
    public static final File COMMITS_DIR = new File(GITLET_DIR, "commits");
    static final File REMOVAL_FILE = join(STAGING_DIR, "removal");
    static final File REMOVAL_DIR = join(GITLET_DIR, "removal");
    static final File BRANCHES_DIR = join(GITLET_DIR,"branches");

    /* TODO: fill in the rest of this class. */
    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        COMMITS_DIR.mkdir();  // 创建 commits 目录
        BRANCHES_DIR.mkdir();
        STAGING_DIR.mkdir();


        Commit initial = new Commit("initial commit", null, new HashMap<>());
        String id = Utils.sha1(Utils.serialize(initial));
        File commitFile = join(COMMITS_DIR, id);
        Utils.writeObject(commitFile, initial);

        // 创建 master 分支
        File master = new File(BRANCHES_DIR, "master");
        Utils.writeContents(master, id);

        // HEAD 指向 master
        Utils.writeContents(HEAD_FILE, "master");
        System.out.println("Initialized an empty Gitlet repository.");
    }


    public static void add(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            // 若文件不存在，但在 staging 里，可能需要清理？
            // 实际上 git add 不存在的文件通常报错，或者视为 rm。
            // CS61B 要求：如果文件不存在，直接退出
            System.out.println("File does not exist: " + filename);
            System.exit(0);
        }

        byte[] content = Utils.readContents(file);
        String blobId = Utils.sha1(content); // 使用内容哈希

        // 1. 处理 Removal (撤销删除)
        if (REMOVAL_FILE.exists()) {
            Set<String> removal = Utils.readObject(REMOVAL_FILE, HashSet.class);
            if (removal.remove(filename)) {
                Utils.writeObject(REMOVAL_FILE, (Serializable) removal);
            }
        }

        // 2. 检查当前 Commit 是否已有相同版本
        String headCommitId = getHeadCommitId();
        Commit headCommit = Utils.readObject(new File(COMMITS_DIR, headCommitId), Commit.class);
        String trackedBlobId = headCommit.getBlobs().get(filename);

        Map<String, String> index = new HashMap<>();
        if (INDEX_FILE.exists()) {
            index = Utils.readObject(INDEX_FILE, HashMap.class);
        }

        // 如果当前添加的内容和 Head Commit 里的一样
        if (blobId.equals(trackedBlobId)) {
            // 如果它在暂存区里，把它移除（恢复原状）
            if (index.containsKey(filename)) {
                index.remove(filename);
                Utils.writeObject(INDEX_FILE, (Serializable) index);
            }
        } else {
            // 内容不一样，或者是新文件 -> 保存 Blob 并更新 Index
            Blob blob = new Blob(content);
            File blobFile = new File(OBJECTS_DIR, blobId);
            if (!blobFile.exists()) {
                Utils.writeObject(blobFile, blob);
            }
            index.put(filename, blobId);
            Utils.writeObject(INDEX_FILE, (Serializable) index);
        }
    }
    private static String getHeadCommitId() {
        String headContent = Utils.readContentsAsString(HEAD_FILE);
        // 判断 headContent 是分支名还是 commitID
        // 简单的判断方法：如果 branches 目录下有这个名字的文件，它就是分支名
        File branchFile = new File(BRANCHES_DIR, headContent);
        if (branchFile.exists()) {
            return Utils.readContentsAsString(branchFile);
        }
        // 否则它就是 commitID (Detached HEAD 情况，虽然本次实验前期不涉及，但为了健壮性)
        return headContent;
    }
    public static void commit(String message) {
        if (message.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }

        // 1. 准备数据
        Map<String, String> index = new HashMap<>();
        if (INDEX_FILE.exists()) {
            index = Utils.readObject(INDEX_FILE, HashMap.class);
        }
        Set<String> removal = new HashSet<>();
        if (REMOVAL_FILE.exists()) {
            removal = Utils.readObject(REMOVAL_FILE, HashSet.class);
        }
        if (index.isEmpty() && removal.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        // 2. 获取 Parent ID (使用 getHeadCommitId)
        String headCommitId = getHeadCommitId();

        // 3. 构建 Blob Map
        Commit parentCommit = null;
        Map<String, String> parentBlobs = new HashMap<>();
        if (!headCommitId.isEmpty()) {
            File parentFile = new File(COMMITS_DIR, headCommitId);
            // 这里为了防止空指针，加个判断，虽然后期一定存在
            if (parentFile.exists()) {
                parentCommit = Utils.readObject(parentFile, Commit.class);
                parentBlobs.putAll(parentCommit.getBlobs());
            }
        }

        // 应用变更
        parentBlobs.putAll(index);
        for (String file : removal) {
            parentBlobs.remove(file);
        }
        REMOVAL_FILE.delete(); // 清理 removal 文件

        // 4. 创建并保存 Commit
        // 注意：这里构造函数参数要和你 Commit.java 里定义的一致
        // 如果你按照之前的建议改了构造函数，这里第二个参数是 null (secondParent)
        Commit newCommit = new Commit(message, headCommitId.isEmpty() ? null : headCommitId, parentBlobs);

        String commitId = Utils.sha1(Utils.serialize(newCommit));
        File commitFile = new File(COMMITS_DIR, commitId);
        Utils.writeObject(commitFile, newCommit);

        // =======================================================
        // 5. 【关键修复】更新指针
        // =======================================================

        // 读取 HEAD 文件原本的内容（比如 "master"）
        String headContent = Utils.readContentsAsString(HEAD_FILE);

        // 看看 branches 目录下有没有叫 "master" 的文件
        File branchFile = new File(BRANCHES_DIR, headContent);

        if (branchFile.exists()) {
            // 如果 HEAD 指向的是一个分支（例如 master），那么更新那个分支文件！
            // 也就是说：master 指向了新的 commit，但 HEAD 依然指向 master
            Utils.writeContents(branchFile, commitId);
        } else {
            // 如果 HEAD 是游离状态（即 HEAD 文件里存的就是 Hash），才直接更新 HEAD
            Utils.writeContents(HEAD_FILE, commitId);
        }

        // =======================================================

        // 6. 清空暂存区
        if (INDEX_FILE.exists()) {
            INDEX_FILE.delete();
        }

        // 只有在测试或者为了调试时保留这行，平时提交不需要这行输出，看 Autograder 要求
        System.out.println("Committed with id " + commitId);
    }
    public static void log(){
        String commitId = getHeadCommitId();

        while(commitId !=null){
            File commitFile = new File(COMMITS_DIR,commitId);
            if(!commitFile.exists()){
                System.out.println("Commit not found: " + commitId);
                System.exit(0);
            }
            Commit commit = Utils.readObject(commitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + commitId);
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage());
            System.out.println();

            commitId = commit.getParent();
        }
    }
    public static  void find(String message){
        boolean found = false;
        for(File file : COMMITS_DIR.listFiles()){
            Commit commit = Utils.readObject(file, Commit.class);
            if(commit!=null &&commit.getMessage().equals(message)){
                found = true;
                System.out.println(file.getName());
            }
        }
        if(!found){
            System.out.println("Found no commit with that message.");
        }
    }
    public static void status(){
        System.out.println("=== Branches ===");

        // 1. 获取所有分支名并排序
        List<String> branches = Utils.plainFilenamesIn(BRANCHES_DIR);
        if (branches != null) {
            String currentHead = Utils.readContentsAsString(HEAD_FILE); // 获取当前分支名
            for (String branch : branches) {
                if (branch.equals(currentHead)) {
                    System.out.println("*" + branch);
                } else {
                    System.out.println(branch);
                }
            }
        }
        System.out.println();
        // ... 后面的代码 ...

        System.out.println("=== Staged Files ===");
        if(INDEX_FILE.exists()){
            Map<String, String> index = Utils.readObject(INDEX_FILE, HashMap.class);
            for(String filename : index.keySet()){
                System.out.println(filename);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        if (REMOVAL_FILE.exists()) {
            Set<String> removal = Utils.readObject(REMOVAL_FILE, HashSet.class);
            List<String> files = new ArrayList<>(removal);
            Collections.sort(files);
            for (String f : files) {
                System.out.println(f);
            }
        }
        System.out.println();

        //1读取三份状态
        // 读取 HEAD commit
        String headCommitId = getHeadCommitId();

        Commit headCommit = null;
        // 防御性编程：虽然理应存在，但刚 init 后可能需要判空（如果你 init 创建了 commit 则不需要）
        File commitFile = new File(COMMITS_DIR, headCommitId);
        if (commitFile.exists()) {
            headCommit = Utils.readObject(commitFile, Commit.class);
        } else {
            // 如果找不到 commit（极少见），为了不崩，创建一个空的 commit 占位
            headCommit = new Commit("", null,  new HashMap<>());
        }

        // 读取 staging area
        Map<String, String> index = new HashMap<>();
        if (INDEX_FILE.exists()) {
            index = Utils.readObject(INDEX_FILE, HashMap.class);
        }

        // commit 中追踪的文件
        Map<String, String> committed = headCommit.getBlobs();

        //2收集所有需要检查的文件名
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(committed.keySet());
        allFiles.addAll(index.keySet());
        //3检查修改和删除
        System.out.println("=== Modifications Not Staged For Commit ===");

        List<String> result = new ArrayList<>();

        for (String filename : allFiles) {
            // 如果已经在 removal 中，不属于 modifications
            if (REMOVAL_FILE.exists()) {
                Set<String> removal = Utils.readObject(REMOVAL_FILE, HashSet.class);
                if (removal.contains(filename)) {
                    continue;
                }
            }
            File f = new File(filename);

            // 文件被删除
            if (!f.exists()) {
                result.add(filename + " (deleted)");
                continue;
            }

            // 计算当前工作目录的 blobId
            byte[] content = Utils.readContents(f);
            String currentBlobId = Utils.sha1(content);

            // staging 中有 → 和 staging 比
            if (index.containsKey(filename)) {
                if (!currentBlobId.equals(index.get(filename))) {
                    result.add(filename + " (modified)");
                }
            }
            // staging 中没有 → 和 commit 比
            else if (committed.containsKey(filename)) {
                if (!currentBlobId.equals(committed.get(filename))) {
                    result.add(filename + " (modified)");
                }
            }
        }

        // 按字典序输出
        Collections.sort(result);
        for (String line : result) {
            System.out.println(line);
        }

        System.out.println();


        System.out.println("=== Untracked Files ===");
        System.out.println();
    }
    public static  void checkoutFile(String filename){

        String headCommitId =getHeadCommitId();

        File commitFile = new File(COMMITS_DIR,headCommitId);
        if(!commitFile.exists()){
            System.out.println("No commit found.");
            System.exit(0);
        }

        Commit commit = Utils.readObject(commitFile,Commit.class);

        String blodId = commit.getBlobs().get(filename);
        if(blodId == null){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File blobFile = new File(OBJECTS_DIR,blodId);
        Blob blob = Utils.readObject(blobFile, Blob.class);

        File cwdFile = new File(filename);
        Utils.writeContents(cwdFile,blob.getContent());
    }
    public static void checkoutFileFromCommit(String commitIdPrefix, String filename) {
        // 1. 找到完整的 commit id（支持前缀）
        String fullCommitId = null;
        for (String name : COMMITS_DIR.list()) {
            if (name.startsWith(commitIdPrefix)) {
                fullCommitId = name;
                break;
            }
        }
        if (fullCommitId == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        // 2. 读取 commit
        File commitFile = new File(COMMITS_DIR, fullCommitId);
        Commit commit = Utils.readObject(commitFile, Commit.class);

        // 3. 在 commit 中查找文件
        String blobId = commit.getBlobs().get(filename);
        if (blobId == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        // 4. 读取 blob
        File blobFile = new File(OBJECTS_DIR, blobId);
        Blob blob = Utils.readObject(blobFile, Blob.class);

        // 5. 写回工作目录
        File cwdFile = new File(filename);
        Utils.writeContents(cwdFile, blob.getContent());
    }
    public static void rm(String filename) {

        // 读取 index
        Map<String, String> index = new HashMap<>();
        if (INDEX_FILE.exists()) {
            index = Utils.readObject(INDEX_FILE, HashMap.class);
        }

        // 读取 removal set
        Set<String> removal = new HashSet<>();
        if (REMOVAL_FILE.exists()) {
            removal = Utils.readObject(REMOVAL_FILE, HashSet.class);
        }

        // 读取 HEAD commit
        String headCommitId = getHeadCommitId();
        Commit headCommit = Utils.readObject(
                new File(COMMITS_DIR, headCommitId),
                Commit.class
        );

        boolean staged = index.containsKey(filename);
        boolean tracked = headCommit.getBlobs().containsKey(filename);

        // 情况 3：没理由 rm
        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            return;
        }

        // 情况 1：如果在 staging 中，先移除 staged
        if (staged) {
            index.remove(filename);
            Utils.writeObject(INDEX_FILE, (Serializable) index);
        }

        // 情况 2：只有 tracked 才进入 removal
        if (tracked) {
            // 确保 removal 目录存在
            REMOVAL_DIR.mkdir();

            removal.add(filename);
            Utils.writeObject(REMOVAL_FILE, (Serializable) removal);

            // 删除工作区文件
            File f = new File(filename);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    public static void branch(String branchName) {
        // 确保 branches 目录存在
        if (!BRANCHES_DIR.exists()) {
            BRANCHES_DIR.mkdir();
        }

        File branchFile = new File(BRANCHES_DIR, branchName);

        // 1. 分支已存在
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        // 2. 获取当前 HEAD 指向的 Commit ID
        // 修正点：getHeadCommitId() 返回的已经是 commit 的哈希值了
        String commitId = getHeadCommitId();

        // 3. 新分支指向同一个 commit
        // 修正点：直接写入这个 ID，不需要再去读取文件
        Utils.writeContents(branchFile, commitId);
    }

    public static void checkoutBranch(String branchName) {

        /* ===== 1. 分支是否存在 ===== */
        File branchFile = new File(BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        /* ===== 2. 是否是当前分支 ===== */
        String currentBranch = Utils.readContentsAsString(HEAD_FILE);
        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        /* ===== 3. 读取当前 commit ===== */
        File currentBranchFile = new File(BRANCHES_DIR, currentBranch);
        String currentCommitId = Utils.readContentsAsString(currentBranchFile);
        Commit currentCommit = Utils.readObject(
                new File(COMMITS_DIR, currentCommitId),
                Commit.class
        );

        /* ===== 4. 读取目标 commit ===== */
        String targetCommitId = Utils.readContentsAsString(branchFile);
        Commit targetCommit = Utils.readObject(
                new File(COMMITS_DIR, targetCommitId),
                Commit.class
        );

        Map<String, String> currentBlobs = currentCommit.getBlobs();
        Map<String, String> targetBlobs = targetCommit.getBlobs();

        /* ===== 5. untracked 文件安全检查 ===== */
        for (String filename : targetBlobs.keySet()) {
            File f = new File(filename);

            // 工作目录有该文件
            // 当前 commit 不跟踪
            // staging area 也没有
            if (f.exists()
                    && !currentBlobs.containsKey(filename)
                    && !isStaged(filename)) {

                System.out.println(
                        "There is an untracked file in the way; delete it, or add and commit it first."
                );
                System.exit(0);
            }
        }

        /* ===== 6. 删除当前有但目标没有的文件 ===== */
        for (String filename : currentBlobs.keySet()) {
            if (!targetBlobs.containsKey(filename)) {
                Utils.restrictedDelete(filename);
            }
        }

        /* ===== 7. 写入目标 commit 中的所有文件 ===== */
        for (String filename : targetBlobs.keySet()) {
            String blobId = targetBlobs.get(filename);
            Blob blob = Utils.readObject(
                    new File(OBJECTS_DIR, blobId),
                    Blob.class
            );
            Utils.writeContents(new File(filename), blob.getContent());
        }

        /* ===== 8. 更新 HEAD ===== */
        Utils.writeContents(HEAD_FILE, branchName);

        /* ===== 9. 清空 staging area 和 removal ===== */
        Utils.writeObject(INDEX_FILE, new HashMap<String, String>());
        Utils.writeObject(REMOVAL_FILE, new HashSet<String>());
    }
    private static boolean isStaged(String filename) {
        if (!INDEX_FILE.exists()) {
            return false;
        }
        Map<String, String> index =
                Utils.readObject(INDEX_FILE, HashMap.class);
        return index.containsKey(filename);
    }
    public static void globalLog() {
        List<String> commitFiles = Utils.plainFilenamesIn(COMMITS_DIR);
        if (commitFiles != null) {
            for (String commitId : commitFiles) {
                File commitFile = new File(COMMITS_DIR, commitId);
                Commit commit = Utils.readObject(commitFile, Commit.class);

                System.out.println("===");
                System.out.println("commit " + commitId);
                System.out.println("Date: " + commit.getTimestamp());
                System.out.println(commit.getMessage());
                System.out.println();
            }
        }
    }
    public static void reset(String commitId) {
        File commitFile = new File(COMMITS_DIR, commitId);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        // 1. 获取目标 Commit
        Commit targetCommit = Utils.readObject(commitFile, Commit.class);

        // 2. 获取当前 Commit
        String headCommitId = getHeadCommitId();
        Commit headCommit = Utils.readObject(new File(COMMITS_DIR, headCommitId), Commit.class);

        // 3. 检查 Untracked Files (安全检查)
        // 逻辑：如果当前工作区有个文件，既没被当前 Commit 追踪，也没在 Staging 区，
        // 但是目标 Commit 里却有这个文件（意味着 reset 会覆盖它），则报错。
        List<String> files = Utils.plainFilenamesIn(CWD);
        if (files != null) {
            for (String filename : files) {
                // 如果目标 Commit 含有该文件
                if (targetCommit.getBlobs().containsKey(filename)) {
                    // 且当前 Commit 没有追踪它
                    if (!headCommit.getBlobs().containsKey(filename)) {
                        // 且 Staging 区里也没有它
                        if (!isStaged(filename)) {
                            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                            System.exit(0);
                        }
                    }
                }
            }
        }

        // 4. Checkout 所有文件 (覆盖或创建)
        Map<String, String> targetBlobs = targetCommit.getBlobs();
        for (Map.Entry<String, String> entry : targetBlobs.entrySet()) {
            String filename = entry.getKey();
            String blobId = entry.getValue();
            File blobFile = new File(OBJECTS_DIR, blobId);
            if(blobFile.exists()){
                Blob blob = Utils.readObject(blobFile, Blob.class);
                Utils.writeContents(new File(filename), blob.getContent());
            }
        }

        // 5. 删除当前有、但目标 Commit 没有的文件
        Map<String, String> currentBlobs = headCommit.getBlobs();
        for (String filename : currentBlobs.keySet()) {
            if (!targetBlobs.containsKey(filename)) {
                Utils.restrictedDelete(filename);
            }
        }

        // 6. 移动指针 (这是 Reset 和 CheckoutCommit 的最大区别)
        // 获取当前分支名 (reset 只移动当前分支)
        String currentBranchName = Utils.readContentsAsString(HEAD_FILE);
        File branchFile = new File(BRANCHES_DIR, currentBranchName);
        Utils.writeContents(branchFile, commitId);

        // 7. 清空暂存区
        if (INDEX_FILE.exists()) {
            INDEX_FILE.delete();
            // 记得重建空文件，或者下次用的时候判空
        }
        if (REMOVAL_FILE.exists()) {
            REMOVAL_FILE.delete();
        }
    }

}
