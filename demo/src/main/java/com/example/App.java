package com.example;

import com.ruiyun.jvppeteer.core.Puppeteer;
import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.core.browser.BrowserContext;
import com.ruiyun.jvppeteer.core.page.ElementHandle;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.options.LaunchOptions;
import com.ruiyun.jvppeteer.options.LaunchOptionsBuilder;
import com.ruiyun.jvppeteer.options.WaitForSelectorOptions;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class App implements JavaSamplerClient {
    Browser browser;
    Page page;

    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("chromePath", "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
        params.addArgument("fakeVideoPath", "C:\\test.y4m");
        params.addArgument("fakeAudioPath", "C:\\test.wav");
        params.addArgument("isHeadless", "true");
        params.addArgument("isLocalMedia", "true");
        params.addArgument("isDefaultMedia", "false");
        params.addArgument("meetingUrl", "https://test.io");
        return params;
    }

    @Override
    public void setupTest(JavaSamplerContext javaSamplerContext) {
        String chromePath = javaSamplerContext.getParameter("chromePath");
        String fakeVideoPath = javaSamplerContext.getParameter("fakeVideoPath");
        String fakeAudioPath = javaSamplerContext.getParameter("fakeAudioPath");
        String path = new String(chromePath.getBytes(), StandardCharsets.UTF_8);
        ArrayList<String> argList = new ArrayList<>();
        argList.add("--no-sandbox");
        argList.add("--disable-setuid-sandbox");
        // argList.add("--ignore-certificate-errors");
        argList.add("--use-fake-ui-for-media-stream");
        argList.add("--use-fake-device-for-media-stream");
        // 啟動無痕瀏覽器
        argList.add("--incognito");
        if (javaSamplerContext.getParameter("isLocalMedia").equals("true")) {
            argList.add("--use-file-for-fake-video-capture=" + fakeVideoPath);
            argList.add("--use-file-for-fake-audio-capture=" + fakeAudioPath);
        }
        boolean isHeadless = javaSamplerContext.getParameter("isHeadless").equals("true");
        LaunchOptions options = new LaunchOptionsBuilder().withArgs(argList).withHeadless(isHeadless)
                .withExecutablePath(path).build();
        try {
            browser = Puppeteer.launch(options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        try {
            BrowserContext context = browser.createIncognitoBrowserContext();
            Page page = context.newPage();
            page.goTo(javaSamplerContext.getParameter("meetingUrl"));
            // 等待10秒等畫面渲染完
            Thread.sleep(10000);
            // 新曾選擇器
            WaitForSelectorOptions options = new WaitForSelectorOptions(true, true, 30000, "raf");
            ElementHandle element = page.waitForSelector("#join-button", options);
            if (element != null) {
                // 找到元素，執行相應的操作
                element.click();
                result.setSuccessful(true); // 測試成功
                result.setResponseData("測試成功", "UTF-8"); // 設置回應內容
            } else {
                // 超時，找不到元素
                System.out.println("元素未找到");
                result.setSuccessful(false); // 測試失敗
                result.setResponseCode("500"); // 設置錯誤的回應碼
                result.setResponseMessage("進入會議室失敗"); // 設置錯誤的回應訊息
                result.setResponseData("進入會議室失敗", "UTF-8"); // 設置回應內容

            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.setResponseCode("500"); // 設置錯誤的回應碼
            result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
            result.setResponseData("出了錯誤", "UTF-8"); // 設置回應內容
        }
        return null;
    }

    @Override
    public void teardownTest(JavaSamplerContext javaSamplerContext) {
        // browser.close();
    }
}
