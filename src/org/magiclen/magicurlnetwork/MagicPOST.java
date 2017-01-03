/*
 *
 * Copyright 2015-2016 magiclen.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.magiclen.json.JSONObject;
import org.magiclen.magicurlnetwork.parameters.Body;
import org.magiclen.magicurlnetwork.parameters.BodyType;

/**
 * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案。
 *
 * @author Magic Len
 * @see MagicURLNetwork
 * @see Body
 * @see NetworkListener
 */
public class MagicPOST extends MagicURLNetwork {

    // -----類別類別-----
    /**
     * 儲存Body和鍵值，用來處理陣列型態的資料重複鍵值的情形。
     */
    private static class BodyWithKey {

	final Body body;
	final String key;

	BodyWithKey(final Body body, final String key) {
	    this.body = body;
	    this.key = key;
	}
    }

    // -----物件常數-----
    /**
     * 儲存當以MultiPart的方式傳遞內容時，要用來分界的字串。
     */
    private final String boundary = String.format("magiclen.org(%d)", System.currentTimeMillis());

    // -----物件變數-----
    /**
     * 儲存是否要以MultiPart的方式傳遞內容。
     */
    private boolean multiPart;
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
    public MagicPOST(final String url) {
	super(url);
    }

    /**
     * 建構子，傳入URL物件。
     *
     * @param url 傳入URL物件
     */
    public MagicPOST(final URL url) {
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
	multiPart = hasParameterType(BodyType.FILE, BodyType.JSON);

	final URL urlWithoutParams = url;
	final URLConnection conn = urlWithoutParams.openConnection();
	conn.setDoInput(true);
	conn.setDoOutput(true);
	conn.setUseCaches(false);
	conn.setRequestProperty(PropertyKeys.CONTENT_TYPE, multiPart ? String.format("multipart/form-data; boundary=%s", boundary) : "application/x-www-form-urlencoded");
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
	http.setRequestMethod("POST");
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
	if (multiPart) {
	    final ArrayList<BodyWithKey> parametersList = new ArrayList<>();
	    final Set<String> originalKeySet = parameters.keySet();

	    // 展開ArrayBody
	    for (final String originalKey : originalKeySet) {
		final Body originalBody = parameters.get(originalKey);
		final BodyType originalBodyType = originalBody.getBodyType();
		if (originalBodyType == BodyType.ARRAY) {
		    final String newBodyKey = originalKey.concat("[]");
		    final Body[] bodyArray = (Body[]) originalBody.getSource();
		    for (final Body b : bodyArray) {
			parametersList.add(new BodyWithKey(b, newBodyKey));
		    }
		} else {
		    parametersList.add(new BodyWithKey(originalBody, originalKey));
		}
	    }

	    // 先儲存要傳送的資料，並計算要傳送資料量
	    final ArrayList<byte[]> sendList = new ArrayList<>();
	    final ArrayList<File> sendFileList = new ArrayList<>();
	    for (final BodyWithKey parameter : parametersList) {
		final Body body = parameter.body;
		final String key = parameter.key;
		final BodyType bodyType = body.getBodyType();
		final String contentType = body.getContentType();
		switch (bodyType) {
		    case FILE: {
			final File file = (File) body.getSource();
			final String formattedContentType;
			if (contentType == null) {
			    formattedContentType = "";
			} else {
			    formattedContentType = String.format("Content-Type: %s%n", contentType);
			}
			final String formData = String.format("--%s%nContent-Disposition: form-data; name=\"%s\"; fileName=\"%s\"%n%sContent-Transfer-Encoding: binary%n%n", boundary, key, encodeHeaderString(file.getName()), formattedContentType);
			final byte[] data = formData.getBytes("UTF-8");
			totalLength += data.length;
			sendList.add(data);
			totalLength += file.length();
			sendList.add(null);
			sendFileList.add(file);
			++totalLength;
			sendList.add(LINE_CHARACTER);
		    }
		    break;
		    case JSON: {
			final String formData = String.format("--%s%nContent-Disposition: form-data; name=\"%s\"%nContent-Type: %s%n%n", boundary, key, contentType);
			byte[] data = formData.getBytes("UTF-8");
			totalLength += data.length;
			sendList.add(data);
			data = body.toString().getBytes("UTF-8");
			totalLength += data.length;
			sendList.add(data);
			++totalLength;
			sendList.add(LINE_CHARACTER);
		    }
		    break;
		    default: {
			final String formattedContentType;
			if (contentType == null) {
			    formattedContentType = "";
			} else {
			    formattedContentType = String.format("Content-Type: %s; charset=UTF-8%n", contentType);
			}
			final String formData = String.format("--%s%nContent-Disposition: form-data; name=\"%s\"%n%s%n%s%n", boundary, key, formattedContentType, body.toString());
			final byte[] data = formData.getBytes("UTF-8");
			totalLength += data.length;
			sendList.add(data);
		    }
		    break;
		}
	    }

	    // 開始傳送資料
	    int fileIndex = 0;
	    for (final byte[] data : sendList) {
		if (data != null) {
		    sum += data.length;
		    bosConn.write(data);
		} else {
		    final File file = sendFileList.get(fileIndex++);
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
		}
	    }
	} else {
	    final String formData = createParametersString();
	    final byte[] data = formData.getBytes("UTF-8");
	    totalLength += data.length;
	    sum += data.length;
	    bosConn.write(data);
	}
	bosConn.flush();

	if (listener != null) {
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
