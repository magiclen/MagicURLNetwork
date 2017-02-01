/*
 *
 * Copyright 2015-2017 magiclen.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magiclen.magicurlnetwork;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import org.magiclen.json.JSONObject;
import org.magiclen.magicurlnetwork.parameters.Body;

/**
 * 對URL發出GET需求(Request)，常用於取得文字訊息或是下載檔案。
 *
 * @author Magic Len
 * @see MagicURLNetwork
 * @see Body
 * @see NetworkListener
 */
public class MagicGET extends MagicURLNetwork {

    // -----建構子-----
    /**
     * 建構子，傳入URL字串。
     *
     * @param url 傳入URL字串
     */
    public MagicGET(final String url) {
	super(url);
    }

    /**
     * 建構子，傳入URL物件。
     *
     * @param url 傳入URL物件
     */
    public MagicGET(final URL url) {
	super(url);
    }

    // -----物件方法-----
    /**
     * 建立連線。
     *
     * @param url 要用來建立連線的URL
     * @throws Exception 拋出例外
     * @return 傳回URLConnection物件
     */
    @Override
    protected URLConnection buildConnection(final URL url) throws Exception {
	final URL urlWithParams = new URL(String.format("%s?%s", url.toString(), createParametersString()));
	final URLConnection conn = urlWithParams.openConnection();
	conn.setDoInput(true);
	conn.setDoOutput(false);
	conn.setUseCaches(false);
	return conn;
    }

    /**
     * 建立HTTP(S)連線。
     *
     * @param http 要建立的HTTP連線
     * @throws Exception 拋出例外
     */
    @Override
    protected void buildHTTPConnection(final HttpURLConnection http) throws Exception {
	http.setRequestMethod("GET");
	http.setInstanceFollowRedirects(true);
    }

    /**
     * 處理傳送連線。
     *
     * @param listener MagicURLNetwork開啟後的監聽者
     * @param parameters 要傳遞的參數
     * @param bosConn 輸出串流，處理傳送給從伺服器的資料
     * @throws Exception 拋出例外
     */
    @Override
    protected void doSendConnection(final NetworkListener listener, final HashMap<String, Body> parameters, final BufferedOutputStream bosConn) throws Exception {
	// 沒有使用到
    }

    /**
     * 處理接收連線。
     *
     * @param listener MagicURLNetwork開啟後的監聽者
     * @param resultHeader URL回傳的標頭
     * @param bisConn 輸入串流，處理從伺服器傳回來的資料
     * @param bos 輸出串流，處理從伺服器傳回來的資料
     * @throws Exception 拋出例外
     */
    @Override
    protected void doReceiveConnection(final NetworkListener listener, final JSONObject resultHeader, final BufferedInputStream bisConn, final BufferedOutputStream bos) throws Exception {
	final long contentLength = resultHeader.getLong("Content-Length");

	int c;
	long sum = 0;
	final byte[] buffer = new byte[BUFFER_SIZE];
	while ((c = bisConn.read(buffer)) >= 0) {
	    if (c > 0) {
		sum += c;
		resetSleepCounter();
	    }
	    bos.write(buffer, 0, c);
	    if (listener != null) {
		try {
		    listener.onRunning(true, sum, contentLength);
		} catch (final Exception ex) {
		    ex.printStackTrace(System.out);
		}
	    }
	}
    }

    /**
     * 取得是否支援非HTTP的連線。
     *
     * @return 傳回是否支援非HTTP的連線
     */
    @Override
    protected boolean supportNonHTTPProtocol() {
	return true;
    }
}
