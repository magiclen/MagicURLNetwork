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
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Set;
import org.magiclen.json.JSONObject;
import org.magiclen.magicurlnetwork.parameters.Body;
import org.magiclen.magicurlnetwork.parameters.BodyType;

/**
 * 對URL發出PUT需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
 *
 * @author Magic Len
 * @see MagicURLNetwork
 * @see Body
 * @see NetworkListener
 */
public class MagicPUT extends MagicURLNetwork {

    // -----物件常數-----
    /**
     * 設定參數的同步鎖。
     */
    private final String setParameterLock = "setParameterLock";

    // -----物件變數-----
    /**
     * 儲存目前唯一的參數資料內容。
     */
    private Body body;
    /**
     * 儲存總共需傳送的資料大小，如果無法預估，值為-1。
     */
    private long totalLength = 0;
    /**
     * 儲存目前已傳送的資料大小。
     */
    private long sum = 0;

    // -----建構子-----
    /**
     * 建構子，傳入URL字串。
     *
     * @param url 傳入URL字串
     */
    public MagicPUT(final String url) {
        super(url);
        checkParameterCount();
    }

    /**
     * 建構子，傳入URL物件。
     *
     * @param url 傳入URL物件
     */
    public MagicPUT(final URL url) {
        super(url);
        checkParameterCount();
    }

    // -----物件方法-----
    /**
     * 檢查參數數量。
     */
    private void checkParameterCount() {
        if (getParameterKeys().size() > 1) {
            throw new RuntimeException("Can't set more than 1 parameters.");
        }
    }

    /**
     * 設定參數(Parameter)，只能設定一個，而且參數內容不能是陣列的型態。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    @Override
    public void setParameter(final String parameterKey, final Body parameterValue) {
        synchronized (setParameterLock) {
            final Body originalBody = getParameter(parameterKey);
            if (parameterValue != null) {
                if (parameterValue.getBodyType() == BodyType.ARRAY) {
                    throw new RuntimeException("Can't use ArrayBody.");
                }
                if (!getParameters().isEmpty() && originalBody == null) {
                    throw new RuntimeException("Can't set more than 1 parameters.");
                }
            }
            super.setParameter(parameterKey, parameterValue);
        }
    }

    /**
     * 建立連線。
     *
     * @param url 要用來建立連線的URL
     * @throws Exception 拋出例外
     * @return 傳回URLConnection物件
     */
    @Override
    protected URLConnection buildConnection(final URL url) throws Exception {
        final URL urlWithoutParams = url;
        final URLConnection conn = urlWithoutParams.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        final Set<String> keys = getParameterKeys();
        if (keys.isEmpty()) {
            body = null;
        } else {
            body = getParameter(keys.iterator().next());
            final String contentType = body.getContentType();
            if (contentType != null) {
                final boolean useUTF8 = contentType.equalsIgnoreCase("text/plain") || contentType.endsWith("application/json");
                conn.setRequestProperty(PropertyKeys.CONTENT_TYPE, contentType.concat(useUTF8 ? "; charset=UTF-8" : ""));
            }
            if (body.getBodyType() == BodyType.FILE) {
                final File file = (File) body.getSource();
                conn.setRequestProperty(PropertyKeys.CONTENT_DISPOSITION, String.format("fileName=\"%s\"", encodeHeaderString(file.getName())));
            }
        }
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
        http.setRequestMethod("PUT");
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
        if (body != null) {
            final BodyType bodyType = body.getBodyType();
            switch (bodyType) {
                case FILE: {
                    final File file = (File) body.getSource();
                    totalLength += file.length();
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                        final byte[] buffer = new byte[BUFFER_SIZE];
                        int c;
                        while ((c = bis.read(buffer)) >= 0) {
                            if (c > 0) {
                                sum += c;
                                resetSleepCounter();
                            }
                            bosConn.write(buffer, 0, c);
                            if (listener != null) {
                                try {
                                    listener.onRunning(false, sum, totalLength);
                                } catch (final Exception ex) {
                                    ex.printStackTrace(System.out);
                                }
                            }
                        }
                    }
                    totalLength += LINE_CHARACTER.length;
                    sum += LINE_CHARACTER.length;
                    bosConn.write(LINE_CHARACTER);
                }
                break;
                default: {
                    final byte[] data = body.toString().getBytes("UTF-8");
                    final int length = data.length;
                    totalLength += length;
                    sum += length;
                    bosConn.write(data);
                }
                break;
            }
        }

        bosConn.flush();

        if (listener
                != null) {
            try {
                listener.onRunning(false, sum, totalLength);
            } catch (final Exception ex) {
                ex.printStackTrace(System.out);
            }
            try {
                listener.onRunning(true, sum, totalLength);
            } catch (final Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
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
        if (contentLength > -1) {
            totalLength += contentLength;
        } else {
            totalLength = -1;
        }

        int c;
        final byte[] buffer = new byte[BUFFER_SIZE];
        while ((c = bisConn.read(buffer)) >= 0) {
            if (c > 0) {
                sum += c;
                resetSleepCounter();
            }
            bos.write(buffer, 0, c);
            if (listener != null) {
                try {
                    listener.onRunning(true, sum, totalLength);
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
        return false;
    }
}
