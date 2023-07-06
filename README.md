## 利用java腳本搭配jmeter寫測試webRTC腳本  

1.先建立maven專案,再把pom.xml把需要依賴的套件都建入  
2.在app主檔裡面編輯(不用建main檔)  
chromePath放電腦內安裝google的地方  
fakeVideoPath設定本地端假視訊影片(僅支援y4m檔案)  
fakeAudioPath設定本地假音訊檔(目前chrome已關掉支援)  
isHeadless是否啟用無頭視窗  
isDefaultMedia是否使用預設值  
meetingUrl要進入視訊的URL  
這些設定參數都透過getDefaultParameters會在jmeter上可以有欄位去抓值  
3.setupTest這邊是設定腳本的地方,初始化瀏覽器設定  
4.runTest這邊是按下測試後會跑的code  

