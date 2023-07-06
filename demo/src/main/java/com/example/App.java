package com.example;

import com.ruiyun.jvppeteer.core.Puppeteer;
import com.ruiyun.jvppeteer.core.browser.Browser;
import com.ruiyun.jvppeteer.core.browser.BrowserContext;
import com.ruiyun.jvppeteer.core.page.ElementHandle;
import com.ruiyun.jvppeteer.core.page.JSHandle;
import com.ruiyun.jvppeteer.core.page.Page;
import com.ruiyun.jvppeteer.options.LaunchOptions;
import com.ruiyun.jvppeteer.options.LaunchOptionsBuilder;
import com.ruiyun.jvppeteer.options.WaitForSelectorOptions;
import com.ruiyun.jvppeteer.core.page.Target;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        // 設置回應response最後結合成一個json
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> combinedResponse = new LinkedHashMap<>();
        // 先把變數製造出來再塞值進去
        long toStartTime = 0L; // 打開瀏覽器時間初始化為 0
        long toClickTime = 0L; // 點擊按鈕初始化為 0
        long toEndTime = 0L; // 點擊按鈕初始化為 0

        try {
            BrowserContext context = browser.createIncognitoBrowserContext();
            Page page = context.newPage();
            page.goTo(javaSamplerContext.getParameter("meetingUrl"));

            // 跳轉網址開始計算時間
            toStartTime = System.currentTimeMillis();

            /**
             * ***********************************
             * 設置等待畫面渲染
             * ***********************************
             */
            JSHandle isPageLoaded = page.waitForFunction("document.readyState === 'complete'");
            // boolean isPageOk = isPageLoaded.jsonValue().equals(true);

            System.out.println("isPageLoaded" + isPageLoaded);
            System.out.println("畫面渲染完");
            // 新增選擇器
            WaitForSelectorOptions options = new WaitForSelectorOptions(true, true,
                    30000, "raf");
            /*
             * **************************************************************
             * 找到button，因button是動態形成所以用迴圈每毫秒去找,找30秒才停
             * 
             * **************************************************************
             */
            int maxWaitTime = 30000; // 最大等待時間，單位毫秒
            int interval = 1000; // 等待間隔，單位毫秒
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < maxWaitTime) {
                ElementHandle element = page.waitForSelector("#join-button", options);
                if (element != null) {
                    // 找到元素，點擊進入會議室
                    element.click();
                    toClickTime = System.currentTimeMillis();
                    break;
                } else {
                    // 檢查網址有沒有跑掉進不去會議室
                    Target target = page.target();
                    String url = target.url();
                    String[] pathSegments = url.split("/");
                    String ending = pathSegments[pathSegments.length - 1];

                    // 如果直接跳到ending沒進入會議室
                    if (ending == "ending") {
                        toEndTime = System.currentTimeMillis();
                        combinedResponse.put("status", 500);
                        combinedResponse.put("msg", "點選join-button進入失敗畫面跳轉ending");
                        combinedResponse.put("startTime", formatTime(toStartTime));
                        combinedResponse.put("clickTime", formatTime(toClickTime));
                        combinedResponse.put("endTime", formatTime(toEndTime));

                        String json = objectMapper.writeValueAsString(combinedResponse);

                        result.setResponseCode("500"); // 設置錯誤的回應碼
                        result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                        result.setResponseData(json, "UTF-8"); // 設置回應內容
                        return result;
                    }

                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    // 中斷異常
                    e.printStackTrace();

                    toEndTime = System.currentTimeMillis();
                    combinedResponse.put("status", 500);
                    combinedResponse.put("msg", "尋找join-button時出了其他錯誤請看log");
                    combinedResponse.put("startTime", formatTime(toStartTime));
                    combinedResponse.put("clickTime", formatTime(toClickTime));
                    combinedResponse.put("endTime", formatTime(toEndTime));

                    String json = objectMapper.writeValueAsString(combinedResponse);

                    result.setResponseCode("500"); // 設置錯誤的回應碼
                    result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                    result.setResponseData(json, "UTF-8"); // 設置回應內容
                    return result;

                }
            }

            if (System.currentTimeMillis() - startTime >= maxWaitTime) {

                toEndTime = System.currentTimeMillis();
                combinedResponse.put("status", 500);
                combinedResponse.put("msg", "尋找join-button超時且url也未跳轉至ending");
                combinedResponse.put("startTime", formatTime(toStartTime));
                combinedResponse.put("clickTime", formatTime(toClickTime));
                combinedResponse.put("endTime", formatTime(toEndTime));

                String json = objectMapper.writeValueAsString(combinedResponse);

                result.setResponseCode("500"); // 設置錯誤的回應碼
                result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                result.setResponseData(json, "UTF-8"); // 設置回應內容
                return result;
            }

            /**
             * **************************************************
             * 確認已進入視訊室去抓按鈕
             * **************************************************
             */

             while (System.currentTimeMillis() - startTime < maxWaitTime) {
                ElementHandle element = page.waitForSelector("#leave-btn", options);
                if (element != null) {
                    toEndTime = System.currentTimeMillis();
                    break;
                } else {
                    // 檢查網址有沒有跑掉進不去會議室
                    Target target = page.target();
                    String url = target.url();
                    String[] pathSegments = url.split("/");
                    String ending = pathSegments[pathSegments.length - 1];

                    // 如果直接跳到ending沒進入會議室
                    if (ending == "ending") {
                        toEndTime = System.currentTimeMillis();
                        combinedResponse.put("status", 500);
                        combinedResponse.put("msg", "找尋leave-button失敗且畫面跳轉ending");
                        combinedResponse.put("startTime", formatTime(toStartTime));
                        combinedResponse.put("clickTime", formatTime(toClickTime));
                        combinedResponse.put("endTime", formatTime(toEndTime));

                        String json = objectMapper.writeValueAsString(combinedResponse);

                        result.setResponseCode("500"); // 設置錯誤的回應碼
                        result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                        result.setResponseData(json, "UTF-8"); // 設置回應內容
                        return result;
                    }

                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    // 中斷異常
                    e.printStackTrace();

                    toEndTime = System.currentTimeMillis();
                    combinedResponse.put("status", 500);
                    combinedResponse.put("msg", "尋找leave-btn時出了其他錯誤請看log");
                    combinedResponse.put("startTime", formatTime(toStartTime));
                    combinedResponse.put("clickTime", formatTime(toClickTime));
                    combinedResponse.put("endTime", formatTime(toEndTime));

                    String json = objectMapper.writeValueAsString(combinedResponse);

                    result.setResponseCode("500"); // 設置錯誤的回應碼
                    result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                    result.setResponseData(json, "UTF-8"); // 設置回應內容
                    return result;

                }
            }

            if (System.currentTimeMillis() - startTime >= maxWaitTime) {

                toEndTime = System.currentTimeMillis();
                combinedResponse.put("status", 500);
                combinedResponse.put("msg", "尋找leave-btn超時且url也未跳轉至ending");
                combinedResponse.put("startTime", formatTime(toStartTime));
                combinedResponse.put("clickTime", formatTime(toClickTime));
                combinedResponse.put("endTime", formatTime(toEndTime));

                String json = objectMapper.writeValueAsString(combinedResponse);

                result.setResponseCode("500"); // 設置錯誤的回應碼
                result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
                result.setResponseData(json, "UTF-8"); // 設置回應內容
                return result;
            }

            /**
             * ****************************************
             * 組合resonse變成json
             * ****************************************
             */

            // 轉換date型態變成string才能塞入json裡面
            combinedResponse.put("status", 200);
            combinedResponse.put("msg", "測試成功");
            combinedResponse.put("startTime", formatTime(toStartTime));
            combinedResponse.put("clickTime", formatTime(toClickTime));
            combinedResponse.put("endTime", formatTime(toEndTime));

            // 將 Map 物件轉換為 JSON 字串
            String json = objectMapper.writeValueAsString(combinedResponse);

            // 輸出 JSON 字串
            System.out.println(json);
            result.setResponseCode("200");
            result.setSuccessful(true); // 測試成功
            result.setResponseData(json, "UTF-8"); // 設置回應內容

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            result.setResponseCode("500"); // 設置錯誤的回應碼
            result.setResponseMessage("會議失敗"); // 設置錯誤的回應訊息
            result.setResponseData("出了其他錯誤請看log", "UTF-8"); // 設置回應內容
            return result;
        }

        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext javaSamplerContext) {
        // browser.close();
    }

    private static String formatTime(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return sdf.format(new Date(timeInMillis));
    }

}
