package com.multichannel;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 安卓安装包多渠道打包
 *
 * @author yuandongfei
 * @date 2018/11/8
 */
public class BatchPackage {

    private static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private List<Future<String>> taskResult;

    public void offerChannel(String channel) {
        queue.offer(channel);
    }

    /**
     * 开始打包
     */
    public void start(String master, String targetDir, String prefix) {
        File masterFile = new File(master);
        if (!masterFile.exists()) {
            throw new ApkChannelExcception(String.format("母包文件不存在[%s]", master));
        }
        File targetDirectory = new File(targetDir);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        taskResult = new ArrayList<>();
        try {
            String channel;
            while (true) {
                channel = queue.poll(1, TimeUnit.SECONDS);
                System.out.println("读取的channel: " + channel);
                if (null == channel) {
                    System.out.println("队列中的消息已读取完毕");
                    break;
                }
                Future<String> result = ThreadManager.getInstance().submit(new ApkChannel(master, targetDir, channel, prefix));
                taskResult.add(result);
            }

        } catch (Exception e) {
            throw new ApkChannelExcception(e);
        }
    }

    List<String> doneList = new ArrayList<>();

    public List<String> getTaskResult() {
        if (null != taskResult && !taskResult.isEmpty()) {
            for (Future<String> task : taskResult) {
                try {
                    if (task.isDone()) {
                        if (!doneList.contains(task.get())) {
                            doneList.add(task.get());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return doneList;
    }

    public void clear() {
        if (null != taskResult) {
            taskResult.clear();
            taskResult = null;
        }
        if(null != doneList){
            doneList.clear();
            doneList = null;
        }
    }

    class ApkChannel implements Callable<String> {
        private String masterFile;
        private String targetDirectory;
        private String channel;
        private String prefix;

        public ApkChannel(String masterFile, String targetDirectory, String channel, String prefix) {
            this.masterFile = masterFile;
            this.targetDirectory = targetDirectory;
            this.channel = channel;
            this.prefix = prefix;
        }

        @Override
        public String call() throws Exception {
            System.out.println(Thread.currentThread().getName() + ": 开始处理。。。");
            ZipFile sourceZip = new ZipFile(masterFile);
            // 复制母包文件到目标渠道目录下，根据目标渠道文件名生成文件
            String targetApkName = getTargetApkName();
            FileOutputStream fos = new FileOutputStream(targetApkName);
            ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos);
            Enumeration<ZipArchiveEntry> entries = sourceZip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                zos.putArchiveEntry(entry);
                int length;
                byte[] buffer = new byte[1024];
                InputStream is = sourceZip.getInputStream(entry);
                while ((length = is.read(buffer)) != -1) {
                    zos.write(buffer, 0, length);
                }
                is.close();
                buffer = null;
            }
            // 拼接渠道识别字符串：META-INF/uuchannel_{channel}。（channel：为读取配置的渠道号）
            zos.putArchiveEntry(new ZipArchiveEntry(prefix.concat(channel)));
            zos.closeArchiveEntry();
            zos.close();
            sourceZip.close();
            System.out.println(Thread.currentThread().getName() + ": 结束处理。。。");
            return targetApkName;
        }

        /**
         * 获取目标渠道包文件的完整（绝对）路径
         *
         * @return
         */
        private String getTargetApkName() {
            String ext = getSuffix(masterFile);
            return targetDirectory.concat(channel).concat(ext);
        }

        private String getSuffix(String fileName) {
            if (null == fileName || fileName.length() == 0) {
                return "";
            }
            String dot = ".";
            if (fileName.lastIndexOf(dot) < 0) {
                return "";
            }
            // 反向肯定预查。从右向左搜符号.和.后面的字符串
            return fileName.replaceAll("^.+(?<=\\.)(.+)$", ".$1");
        }
    }
}
