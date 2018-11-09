package com.multichannel;

/**
 * @author yuandongfei
 * @date 2018/11/8
 */
public class FilePack {

    public static void main(String[] args) {
        long s = System.currentTimeMillis();
        // 获取母包apk文件
        String sourceApk = "D:/workspace/python/2018swsg001.apk";
        String targetDir = "D:/workspace/python/target/";
        // 拼接渠道识别字符串：META-INF/uuchannel_{channel}。（channel：为读取配置的渠道号）
        String channelFileName = "META-INF/uuchannel_";
        String formatChannel = "test%dchannel";
        int left = 1, right = 8;
        BatchPackage bpk = new BatchPackage();
        for (int i = left; i <= right; i++) {
            String channel = String.format(formatChannel, i);
            bpk.offerChannel(channel);
        }
        bpk.start(sourceApk, targetDir, channelFileName);
        int count = right - left + 1;
        while (true) {
            System.out.println("现在完成打包文件数： [" + bpk.getTaskResult().size() + "]/" + count);
            if (bpk.getTaskResult().size() == count) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (String path : bpk.getTaskResult()) {
            System.out.println("渠道包路径： = [" + path + "]");
        }
        bpk.clear();
        long l = System.currentTimeMillis() - s;
        System.out.println("用时：[" + l / 1000 + "s]");
        System.exit(0);
    }
}
