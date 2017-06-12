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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.magiclen.json.JSONArray;
import org.magiclen.json.JSONObject;
import org.magiclen.magicurlnetwork.parameters.Body;
import org.magiclen.magicurlnetwork.parameters.BodyType;
import org.magiclen.magicurlnetwork.parameters.StringBody;
import org.magiclen.mson.JSONType;

/**
 *
 * <p>
 * 處理URL的上傳以及下載動作。
 * </p>
 *
 * <p>
 * 如果只想要單純從URL接收資料，可以使用GET方法；如果想要從URL傳送或同時接收資料，可以使用POST、SinglePOST、PUT方法；如果想查看URL回傳的HEADER，可以使用HEAD方法；如果想要刪除URL所指的資料，可以使用DELETE方法。
 * </p>
 *
 * @author Magic Len
 * @see Body
 * @see MagicGET
 * @see MagicPOST
 * @see MagicSinglePOST
 * @see MagicHEAD
 * @see MagicPUT
 * @see MagicDELETE
 */
public abstract class MagicURLNetwork {

    // -----類別常數-----
    /**
     * 執行緒逾時和ReadTimeout、ConnectTimeout的間隔時間。
     */
    private static final int TIMEOUT_INTERVAL = 1500;
    /**
     * 每次逾時檢查的間隔時間。
     */
    private static final int SLEEP_INTERVAL = 200;
    /**
     * 預設的連線逾時時間。
     */
    private static final int DEFAULT_TIMEOUT = 15000;
    /**
     * 緩衝空間大小。
     */
    private static final boolean DEFAULT_ACCEPT_NOT_200_HTTP_RESPONSE_CODE = false;
    /**
     * 緩衝空間大小。
     */
    protected static final int BUFFER_SIZE = 2048;
    /**
     * 換行字元。
     */
    protected static final byte[] LINE_CHARACTER;

    static {
        try {
            LINE_CHARACTER = "\r\n".getBytes("UTF-8");
        } catch (final Exception ex) {
            throw new RuntimeException();
        }
    }

    // -----類別介面-----
    /**
     * MagicURLNetwork開啟後的監聽者。
     */
    public interface NetworkListener {

        /**
         * 開啟後。
         */
        public void onStarted();

        /**
         * 執行時。
         *
         * @param receiving 是否正在接收，否則為傳送
         * @param currentBytes 目前已傳送和接收的位元組數量
         * @param totalBytes 總共需傳送和接收的位元組數量，如果傳回-1，表示無法取得正確的數量
         */
        public void onRunning(final boolean receiving, final long currentBytes, final long totalBytes);

        /**
         * 失敗時。
         *
         * @param message 失敗訊息
         * @param attemptDisconnect 嘗試斷開連結
         */
        public void onFailed(final String message, final boolean attemptDisconnect);

        /**
         * 結束時。
         *
         * @param resultHeader 伺服器回傳的標頭，如果失敗，將為null
         * @param result 執行結果，如果失敗，將為null
         */
        public void onFinished(final JSONObject resultHeader, final Object result);
    }

    // -----類別類別-----
    /**
     * 屬性(Property)所使用的鍵值。
     */
    public static final class PropertyKeys {

        /**
         * 用戶代理。
         */
        public static final String USER_AGENT = "User-Agent";
        /**
         * 授權。
         */
        public static final String AUTHORIZATION = "Authorization";
        /**
         * Cookie。
         */
        public static final String COOKIE = "Cookie";
        /**
         * 內容型態。
         */
        public static final String CONTENT_TYPE = "Content-Type";
        /**
         * 內容處置。
         */
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
    }

    /**
     * <p>
     * 標頭(Header)中User Agent的資訊，可以用來表示用戶端的身份。
     * </p>
     * <p>
     * 這個類別含有預設定義好的User Agent可以使用。
     * </p>
     */
    public static final class UserAgents {

        /**
         * 預設。
         */
        public static final String DEFAULT = "Mozilla/5.0 (Java; magiclen.org) MagicURLNetwork/".concat(Version.getVersion());
        /**
         * 模擬火狐瀏覽器。
         */
        public static final String FIREFOX = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0";
        /**
         * 模擬Google的Chrome瀏覽器。
         */
        public static final String CHROME = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36";
        /**
         * 模擬Android的WebKit。
         */
        public static final String ANDROID = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 7 Build/MMB29K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.91 Mobile Safari/537.36";
        /**
         * 模擬微軟的IE瀏覽器。
         */
        public static final String INTERNET_EXPLORER = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko";
        /**
         * 模擬微軟的EDGE瀏覽器。
         */
        public static final String EDGE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36 Edge/14.14359";

        /**
         * 私有建構子，讓這個類別無法被實體化。
         */
        private UserAgents() {

        }
    }
    // -----類別列舉-----

    // -----類別方法-----
    /**
     * 對URL發出DELETE需求(Request)，常用於刪除指定URL(URI)的資源。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicDELETE物件
     *
     */
    public static MagicDELETE DELETE(final String url) {
        return new MagicDELETE(url);
    }

    /**
     * 對URL發出DELETE需求(Request)，常用於刪除指定URL(URI)的資源。
     *
     * @param url 傳入URL物件
     * @return 傳回MagicDELETE物件
     *
     */
    public static MagicDELETE DELETE(final URL url) {
        return new MagicDELETE(url);
    }

    /**
     * 對URL發出HEAD需求(Request)，常用於取得標頭訊息。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicHEAD物件
     *
     */
    public static MagicHEAD HEAD(final String url) {
        return new MagicHEAD(url);
    }

    /**
     * 對URL發出HEAD需求(Request)，常用於取得標頭訊息。
     *
     * @param url 傳入URL物件
     * @return 傳回MagicHEAD物件
     *
     */
    public static MagicHEAD HEAD(final URL url) {
        return new MagicHEAD(url);
    }

    /**
     * 對URL發出PUT需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicPUT物件
     *
     */
    public static MagicPUT PUT(final String url) {
        return new MagicPUT(url);
    }

    /**
     * 對URL發出PUT需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
     *
     * @param url 傳入URL物件
     * @return 傳回MagicPUT物件
     *
     */
    public static MagicPUT PUT(final URL url) {
        return new MagicPUT(url);
    }

    /**
     * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicSinglePOST物件
     *
     */
    public static MagicSinglePOST SinglePOST(final String url) {
        return new MagicSinglePOST(url);
    }

    /**
     * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
     *
     * @param url 傳入URL物件
     * @return 傳回MagicSinglePOST物件
     *
     */
    public static MagicSinglePOST SinglePOST(final URL url) {
        return new MagicSinglePOST(url);
    }

    /**
     * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicPOST物件
     *
     */
    public static MagicPOST POST(final String url) {
        return new MagicPOST(url);
    }

    /**
     * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案。
     *
     * @param url 傳入URL物件
     * @return 傳回MMagicPOST物件
     *
     */
    public static MagicPOST POST(final URL url) {
        return new MagicPOST(url);
    }

    /**
     * 對URL發出GET需求(Request)，常用於取得文字訊息或是下載檔案。
     *
     * @param url 傳入URL字串
     * @return 傳回MagicGET物件
     *
     */
    public static MagicGET GET(final String url) {
        return new MagicGET(url);
    }

    /**
     * 對URL發出GET需求(Request)，常用於取得文字訊息或是下載檔案。
     *
     * @param url 傳入URL物件
     * @return 傳回MagicGET物件
     *
     */
    public static MagicGET GET(final URL url) {
        return new MagicGET(url);
    }

    /**
     * 將字串編碼成能在標頭上正常使用的字串。
     *
     * @param string 傳入字串
     * @return 回傳編碼之後的字串
     */
    protected static String encodeHeaderString(final String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("[\"\'\n]", "");
    }

    // -----物件常數-----
    /**
     * 使用URL時要傳遞的參數。
     */
    private final HashMap<String, Body> parameters = new HashMap<>();
    /**
     * 發出需求(Request)時要傳遞的屬性。
     */
    private final HashMap<String, String> properties = new HashMap<>();
    /**
     * 發出需求(Request)時要傳遞的Cookie。
     */
    private final HashMap<String, String> cookies = new HashMap<>();
    /**
     * 停滯時間計數器的同步鎖。
     */
    private final String sleepCounterLock = "sleepCounterLock";

    // -----物件變數-----
    /**
     * 儲存URL物件。
     */
    private URL url;
    /**
     * 儲存是否為HTTPS的URL。
     */
    private boolean isHttps;
    /**
     * 儲存是否為HTTP的URL。
     */
    private boolean isHttp;
    /**
     * 儲存是否為HTTP或是HTTPS的URL。
     */
    private boolean isHttpOrHttps;
    /**
     * 儲存URLConnection物件。
     */
    private URLConnection conn = null;
    /**
     * 儲存這個MagicURLNetwork是不是正在開啟中。
     */
    private boolean opening = false;
    /**
     * 連線Timeout時間。
     */
    private int timeout = 0;
    /**
     * 連線Timeout時間計數器。
     */
    private Thread timeoutTimer = null;
    /**
     * 儲存連線停滯的時間單位數量。
     */
    private int sleepCounter = 0;
    /**
     * 儲存是否正在嘗試斷開連線。
     */
    private boolean attemptDisconnecting = false;
    /**
     * 儲存開啟URL之後回傳的結果。
     */
    private Object result = null;
    /**
     * 儲存開啟URL之後回傳的標頭。
     */
    private JSONObject resultHeader = null;
    /**
     * 目標檔案，如果不為null，則開啟URL的結果將會存到這個File物件所指的檔案中。
     */
    private File targetFile = null;
    /**
     * 網路狀態的監聽者。
     */
    private NetworkListener listener;
    /**
     * 是否要允許Response Code非200的HTTP連線。
     */
    private boolean acceptNot200HTTPResponseCode;

    // -----建構子-----
    /**
     * 建構子，傳入URL字串。
     *
     * @param url 傳入URL字串
     */
    public MagicURLNetwork(final String url) {
        try {
            init(new URL(url));
        } catch (final Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * 建構子，傳入URL物件。
     *
     * @param url 傳入URL物件
     */
    public MagicURLNetwork(final URL url) {
        try {
            init(url);
        } catch (final Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    // -----物件方法-----
    /**
     * 初始化。
     *
     * @param url 傳入URL物件
     * @throws Exception 如果初始化過程中出現問題，將拋出例外
     */
    private void init(final URL url) throws Exception {
        //調整URL
        final StringBuilder sbURL = new StringBuilder();
        sbURL.append(url.getProtocol()).append("://").append(url.getAuthority()).append(url.getPath());

        final URL modifyURL = new URL(sbURL.toString());
        this.url = modifyURL;

        // 判別協定
        final String protocol = url.getProtocol();
        isHttps = protocol.equalsIgnoreCase("https");
        isHttp = protocol.equalsIgnoreCase("http");
        isHttpOrHttps = isHttps || isHttp;
        if (!isHttpOrHttps && !supportNonHTTPProtocol()) {
            throw new RuntimeException("Not support for non HTTP(s) protocol.");
        }

        // 取得網址所含的參數(?後的字串)
        String parameter = url.getQuery();
        if (parameter != null) {
            parameter = parameter.trim();
            if (parameter.length() > 0) {
                final String[] parameterArray = parameter.split("&");
                for (final String p : parameterArray) {
                    final String[] pArray = p.split("=");
                    if (pArray.length == 2) { //只吃xxx=yyy這種格式的參數
                        final StringBody body = new StringBody(URLDecoder.decode(pArray[1].trim(), "UTF-8"));
                        setParameter(pArray[0].trim(), body);
                    }
                }
            }
        }

        // 使用預設值
        useDefaultUserAgent();
        useDefaultTimeout();
        useDefaultAcceptNot200HTTPResponseCode();
    }

    /**
     * 判斷這個MagicURLNetwork是不是正在開啟中。
     *
     * @return 傳回這個MagicURLNetwork是不是正在開啟中
     */
    public boolean isOpening() {
        return opening;
    }

    /**
     * 取得參數(Parameter)。
     *
     * @param parameterKey 傳入屬性的鍵值，不能為null
     * @return 傳回參數的內容，如果為null表示傳入的參數鍵值不存在
     */
    public Body getParameter(final String parameterKey) {
        if (parameterKey == null) {
            throwNullParameterKeyException();
        } else if (checkHasArrayCharacters(parameterKey)) {
            throwArrayParameterKeyException();
        }
        synchronized (parameters) {
            return parameters.get(parameterKey);
        }
    }

    /**
     * 刪除參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @return 傳回原本參數鍵值的內容，如果為null表示原本的參數鍵值不存在
     */
    public Body removeParameter(final String parameterKey) {
        if (parameterKey == null) {
            throwNullParameterKeyException();
        } else if (checkHasArrayCharacters(parameterKey)) {
            throwArrayParameterKeyException();
        }
        synchronized (parameters) {
            if (opening) {
                throwOpeningException();
            }
            final Body value = parameters.get(parameterKey);
            if (value != null) {
                parameters.remove(parameterKey);
            }
            return value;
        }
    }

    /**
     * 清空所有參數(Parameter)。
     */
    public void clearParameters() {
        synchronized (parameters) {
            if (opening) {
                throwOpeningException();
            }
            parameters.clear();
        }
    }

    /**
     * 取得所有參數(Parameter)。
     *
     * @return 傳回所有參數
     */
    protected HashMap<String, Body> getParameters() {
        final HashMap<String, Body> newParameters = new HashMap<>();
        synchronized (parameters) {
            newParameters.putAll(parameters);
        }
        return newParameters;
    }

    /**
     * 取得所有參數(Parameter)的鍵值。
     *
     * @return 傳回所有參數的鍵值
     */
    protected Set<String> getParameterKeys() {
        final Set<String> newKeys = new HashSet<>();
        synchronized (parameters) {
            newKeys.addAll(parameters.keySet());
        }
        return newKeys;
    }

    /**
     * 設定參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    public void setParameter(final String parameterKey, final Body parameterValue) {
        if (parameterKey == null) {
            throwNullParameterKeyException();
        } else if (checkHasArrayCharacters(parameterKey)) {
            throwArrayParameterKeyException();
        }
        synchronized (parameters) {
            if (opening) {
                throwOpeningException();
            }
            if (parameterValue == null) {
                // 移除鍵值
                parameters.remove(parameterKey);
            } else {
                // 設定參數
                parameters.put(parameterKey, parameterValue);
            }
        }
    }

    /**
     * 設定字串參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    public void setParameter(final String parameterKey, final String parameterValue) {
        if (parameterValue == null) {
            removeParameter(parameterKey);
        } else {
            setParameter(parameterKey, Body.STRING(parameterValue));
        }
    }

    /**
     * 設定數值參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    public void setParameter(final String parameterKey, final Number parameterValue) {
        if (parameterValue == null) {
            removeParameter(parameterKey);
        } else {
            setParameter(parameterKey, Body.NUMBER(parameterValue));
        }
    }

    /**
     * 設定檔案參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     * @throws java.lang.Exception 拋出例外
     */
    public void setParameter(final String parameterKey, final File parameterValue) throws Exception {
        if (parameterValue == null) {
            removeParameter(parameterKey);
        } else {
            setParameter(parameterKey, Body.FILE(parameterValue));
        }
    }

    /**
     * 設定JSON參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    public void setParameter(final String parameterKey, final JSONType parameterValue) {
        if (parameterValue == null) {
            removeParameter(parameterKey);
        } else {
            setParameter(parameterKey, Body.JSON(parameterValue));
        }
    }

    /**
     * 設定陣列參數(Parameter)。
     *
     * @param parameterKey 傳入參數的鍵值，不能為null
     * @param parameterValue 傳入參數的內容，若傳入null，表示移除這個參數鍵值的資料
     */
    public void setParameter(final String parameterKey, final Body... parameterValue) {
        if (parameterValue == null) {
            removeParameter(parameterKey);
        } else {
            setParameter(parameterKey, Body.ARRAY(parameterValue));
        }
    }

    /**
     * 取得屬性(Property)。
     *
     * @param propertyKey 傳入屬性的鍵值，不能為null
     * @return 傳回屬性的內容，如果為null表示傳入的屬性鍵值不存在
     */
    public String getProperty(final String propertyKey) {
        if (propertyKey == null) {
            throwNullPropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.COOKIE)) {
            throwCookiePropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_TYPE)) {
            throwContentTypeKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_DISPOSITION)) {
            throwContentDispositionKeyException();
        }
        synchronized (properties) {
            return properties.get(propertyKey);
        }
    }

    /**
     * 刪除屬性(Property)。
     *
     * @param propertyKey 傳入屬性的鍵值，不能為null
     * @return 傳回原本屬性鍵值的內容，如果為null表示原本的屬性鍵值不存在
     */
    public String removeProperty(final String propertyKey) {
        if (propertyKey == null) {
            throwNullPropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.COOKIE)) {
            throwCookiePropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_TYPE)) {
            throwContentTypeKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_DISPOSITION)) {
            throwContentDispositionKeyException();
        }
        synchronized (properties) {
            if (opening) {
                throwOpeningException();
            }
            final String value = properties.get(propertyKey);
            if (value != null) {
                properties.remove(propertyKey);
            }
            return value;
        }
    }

    /**
     * 清空所有屬性(Property)。
     */
    public void clearProperties() {
        synchronized (properties) {
            if (opening) {
                throwOpeningException();
            }
            properties.clear();
        }
    }

    /**
     * 設定屬性(Property)。
     *
     * @param propertyKey 傳入屬性的鍵值，不能為null
     * @param propertyValue 傳入屬性的內容，若傳入null，表示移除這個屬性鍵值的資料
     */
    public void setProperty(final String propertyKey, final String propertyValue) {
        if (propertyKey == null) {
            throwNullPropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.COOKIE)) {
            throwCookiePropertyKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_TYPE)) {
            throwContentTypeKeyException();
        } else if (propertyKey.equals(PropertyKeys.CONTENT_DISPOSITION)) {
            throwContentDispositionKeyException();
        }
        synchronized (properties) {
            if (opening) {
                throwOpeningException();
            }
            if (propertyValue == null) {
                // 移除鍵值
                properties.remove(propertyKey);
            } else {
                // 設定屬性
                properties.put(propertyKey, propertyValue.trim());
            }
        }
    }

    /**
     * 取得Cookie。
     *
     * @param cookieKey 傳入Cookie的鍵值，不能為null
     * @return 傳回Cookie的內容，如果為null表示傳入的Cookie鍵值不存在
     */
    public String getCookie(final String cookieKey) {
        if (cookieKey == null) {
            throwNullCookieKeyException();
        }
        synchronized (cookies) {
            return cookies.get(cookieKey);
        }
    }

    /**
     * 刪除Cookie。
     *
     * @param cookieKey 傳入Cookie的鍵值，不能為null
     * @return 傳回原本Cookie鍵值的內容，如果為null表示原本的Cookie鍵值不存在
     */
    public String removeCookie(final String cookieKey) {
        if (cookieKey == null) {
            throwNullCookieKeyException();
        }
        synchronized (cookies) {
            if (opening) {
                throwOpeningException();
            }
            final String value = cookies.get(cookieKey);
            if (value != null) {
                cookies.remove(cookieKey);
            }
            return value;
        }
    }

    /**
     * 清空所有Cookies。
     */
    public void clearCookies() {
        synchronized (cookies) {
            if (opening) {
                throwOpeningException();
            }
            cookies.clear();
        }
    }

    /**
     * 設定Cookie。
     *
     * @param cookieKey 傳入Cookie的鍵值，不能為null
     * @param cookieValue 傳入Cookie的內容，若傳入null，表示移除這個Cookie鍵值的資料
     */
    public void setCookie(final String cookieKey, final String cookieValue) {
        if (cookieKey == null) {
            throwNullCookieKeyException();
        }
        synchronized (cookies) {
            if (opening) {
                throwOpeningException();
            }
            if (cookieValue == null) {
                // 移除鍵值
                cookies.remove(cookieKey);
            } else {
                // 設定屬性
                cookies.put(cookieKey, cookieValue.trim());
            }
        }
    }

    /**
     * 取得用戶代理(User-Agent)。
     *
     * @return 傳回用戶代理的內容，若沒有設定用戶代理，傳回null
     */
    public String getUserAgent() {
        return getProperty(PropertyKeys.USER_AGENT);
    }

    /**
     * 設定用戶代理(User-Agent)。
     *
     * @param userAgent 傳入用戶代理的訊息，若傳入null，表示移除用戶代理的資料
     */
    public void setUserAgent(final String userAgent) {
        setProperty(PropertyKeys.USER_AGENT, userAgent);
    }

    /**
     * 使用預設的用戶代理(User-Agent)設定。
     */
    public void useDefaultUserAgent() {
        setUserAgent(UserAgents.DEFAULT);
    }

    /**
     * 移除用戶代理(User-Agent)設定。
     */
    public void removeUserAgent() {
        removeProperty(PropertyKeys.USER_AGENT);
    }

    /**
     * 取得授權(Authorization)。
     *
     * @return 傳回授權的內容，若沒有設定授權，傳回null
     */
    public String getAuthorization() {
        return getProperty(PropertyKeys.AUTHORIZATION);
    }

    /**
     * 設定授權(Authorization)。
     *
     * @param authorization 傳入授權的訊息，若傳入null，表示移除授權的資料
     */
    public void setAuthorization(final String authorization) {
        setProperty(PropertyKeys.AUTHORIZATION, authorization);
    }

    /**
     * 移除授權(Authorization)設定。
     */
    public void removeAuthorization() {
        removeProperty(PropertyKeys.AUTHORIZATION);
    }

    /**
     * 取得URL物件。
     *
     * @return 傳回URL物件
     */
    public URL getURL() {
        return url;
    }

    /**
     * 取得URL的協定。
     *
     * @return 傳回URL的協定
     */
    public String getProtocol() {
        return url.getProtocol();
    }

    /**
     * 取得逾時時間。
     *
     * @return 傳回逾時時間
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 使用預設逾時設定。
     */
    public void useDefaultTimeout() {
        setTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * 設定逾時時間。
     *
     * @param timeout 傳入逾時時間
     */
    public void setTimeout(final int timeout) {
        if (timeout <= 0) {
            throw new RuntimeException("Timout duration needs to be more than 0.");
        }
        if (opening) {
            throwOpeningException();
        }
        this.timeout = timeout;
    }

    /**
     * 取得開啟URL之後回傳的結果。
     *
     * @return 傳回開啟URL之後回傳的結果，如果傳回null，表示URL還沒有開啟，或是URL沒有開啟成功
     */
    public Object getResult() {
        return result;
    }

    /**
     * 取得開啟URL之後回傳的標頭。
     *
     * @return 傳回開啟URL之後回傳的標頭，如果傳回null，表示URL還沒有開啟，或是URL沒有開啟成功
     */
    public JSONObject getResultHeader() {
        return resultHeader;
    }

    /**
     * 以字串的形式取得開啟URL之後的結果。
     *
     * @return 傳回開啟URL之後的結果，如果傳回null，表示URL還沒有開啟，或是URL沒有開啟成功
     * @throws RuntimeException 當結果不是字串的時候拋出例外
     */
    public String getResultAsString() throws RuntimeException {
        if (result == null) {
            return null;
        }
        try {
            return new String((byte[]) result, "UTF-8");
        } catch (final Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * 以檔案的形式取得開啟URL之後的結果。
     *
     * @return 傳回開啟URL之後的結果，如果傳回null，表示URL還沒有開啟，或是URL沒有開啟成功
     * @throws RuntimeException 當結果不是檔案的時候拋出例外
     */
    public File getResultAsFile() throws RuntimeException {
        if (result == null) {
            return null;
        }
        return (File) result;
    }

    /**
     * 取得目標檔案。
     *
     * @return 傳回目標檔案，如果不為null，則開啟URL的結果將會存到這個File物件所指的檔案中
     */
    public File getTargetFile() {
        return targetFile;
    }

    /**
     * 設定目標檔案。
     *
     * @param targetFile 傳入目標檔案，如果不為null，則開啟URL的結果將會存到這個File物件所指的檔案中
     */
    public void setTargetFile(final File targetFile) {
        if (opening) {
            throwOpeningException();
        }
        this.targetFile = targetFile;
    }

    /**
     * 取得MagicURLNetwork開啟後的監聽者。
     *
     * @return 傳回MagicURLNetwork開啟後的監聽者，如果沒有設定，傳回null
     */
    public NetworkListener getNetworkListener() {
        return listener;
    }

    /**
     * 設定MagicURLNetwork開啟後的監聽者。
     *
     * @param listener 傳入MagicURLNetwork開啟後的監聽者
     */
    public void setNetworkListener(final NetworkListener listener) {
        if (opening) {
            throwOpeningException();
        }
        this.listener = listener;
    }

    /**
     * 使用預設值設定是否要允許Response Code非200的HTTP連線。
     *
     */
    public void useDefaultAcceptNot200HTTPResponseCode() {
        acceptNot200HTTPResponseCode = DEFAULT_ACCEPT_NOT_200_HTTP_RESPONSE_CODE;
    }

    /**
     * 設定是否要允許Response Code非200的HTTP連線。
     *
     * @param acceptNot200HTTPResponseCode 傳入是否要允許Response Code非200的HTTP連線
     */
    public void setAcceptNot200HTTPResponseCode(final boolean acceptNot200HTTPResponseCode) {
        this.acceptNot200HTTPResponseCode = acceptNot200HTTPResponseCode;
    }

    /**
     * 取得是否要允許Response Code非200的HTTP連線。
     *
     * @return 傳回是否要允許Response Code非200的HTTP連線
     */
    public boolean isAcceptNot200HTTPResponseCode() {
        return acceptNot200HTTPResponseCode;
    }

    /**
     * 嘗試關閉連線。
     */
    public void attemptDisconnect() {
        attemptDisconnecting = true;
        stop();
    }

    /**
     * 將Cookie中的資料串成一個字串。
     *
     * @return 傳回串好的字串
     */
    protected String createCookiesString() {
        final StringBuilder sb = new StringBuilder("");
        synchronized (cookies) {
            final Set<String> keys = cookies.keySet();
            for (final String key : keys) {
                final String cookie = cookies.get(key);
                try {
                    sb.append(URLEncoder.encode(key, "UTF-8")).append("=");
                    sb.append(URLEncoder.encode(cookie, "UTF-8"));
                    sb.append("; ");
                } catch (final Exception ex) {
                    //不可能執行到這裡
                }
            }
        }
        final int length = sb.length();
        if (length > 0) {
            sb.deleteCharAt(length - 2);
        }
        return sb.toString();
    }

    /**
     * 將參數(parameter)中的資料串成一個字串(常用於GET)。
     *
     * @return 傳回串好的字串
     */
    protected String createParametersString() {
        final StringBuilder sb = new StringBuilder("");
        synchronized (parameters) {
            final Set<String> keys = parameters.keySet();
            for (final String key : keys) {
                final Body body = parameters.get(key);
                final BodyType bodyType = body.getBodyType();
                try {
                    if (bodyType == BodyType.ARRAY) {
                        final Body[] bodyArray = (Body[]) body.getSource();
                        for (final Body b : bodyArray) {
                            sb.append(URLEncoder.encode(key.concat("[]"), "UTF-8")).append("=");
                            sb.append(URLEncoder.encode(b.toString(), "UTF-8"));
                            sb.append("&");
                        }
                    } else {
                        sb.append(URLEncoder.encode(key, "UTF-8")).append("=");
                        sb.append(URLEncoder.encode(body.toString(), "UTF-8"));
                        sb.append("&");
                    }
                } catch (final Exception ex) {
                    //不可能執行到這裡
                }
            }
        }
        final int length = sb.length();
        if (length > 0) {
            sb.deleteCharAt(length - 1);
        }
        return sb.toString();
    }

    /**
     * 重設停滯計數器。
     */
    protected void resetSleepCounter() {
        synchronized (sleepCounterLock) {
            sleepCounter = 0;
        }
    }

    /**
     * 判斷參數中是否有指定的參數資料類型。
     *
     * @param types 傳入指定的參數資料類型
     * @return 傳回參數中是否有檔案資料
     */
    protected boolean hasParameterType(final BodyType... types) {
        if (types == null || types.length == 0) {
            return false;
        }
        final HashSet<BodyType> typeSet = new HashSet<>();
        for (final BodyType type : types) {
            typeSet.add(type);
        }
        synchronized (parameters) {
            final Set<String> keys = parameters.keySet();
            for (final String key : keys) {
                final Body body = parameters.get(key);
                final BodyType type = body.getBodyType();
                if (typeSet.contains(type)) {
                    return true;
                }
                if (type == BodyType.ARRAY) {
                    final Body[] bodyArray = (Body[]) body.getSource();
                    for (final Body b : bodyArray) {
                        final BodyType t = b.getBodyType();
                        if (typeSet.contains(t)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 建立不檢查證書，允許所有SSL連線的SSLSocketFactory。
     *
     * @return 傳回SSLSocketFactory
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private SSLSocketFactory createAnySSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{new X509TrustManager() {

            @Override
            public void checkClientTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] arg0, final String arg1) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }}, new SecureRandom());
        return ctx.getSocketFactory();
    }

    /**
     * 建立允許所有主機名稱的HostnameVerifier。
     *
     * @return 傳回HostnameVerifier
     */
    private HostnameVerifier createAnyHostnameVerifier() {
        return new HostnameVerifier() {

            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        };
    }

    /**
     * 檢查字串是否有陣列使用的字元('['、']')。
     *
     * @param string 傳入要檢查的字串
     * @return 傳回字串是否有陣列使用的字元
     */
    private boolean checkHasArrayCharacters(final String string) {
        if (string == null) {
            return false;
        }
        return string.contains("[") || string.contains("]");
    }

    /**
     * 拋出使用陣列型態的參數例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwArrayParameterKeyException() throws RuntimeException {
        throw new RuntimeException("Can not use '[' or ']' characters in your parameter key. If you want to pass array parameter, please use ArrayBody.");
    }

    /**
     * 拋出空的Cookie鍵值例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwNullCookieKeyException() throws RuntimeException {
        throw new RuntimeException("Null cookie key.");
    }

    /**
     * 拋出空的屬性鍵值例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwNullPropertyKeyException() throws RuntimeException {
        throw new RuntimeException("Null property key.");
    }

    /**
     * 拋出Cookie屬性鍵值例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwCookiePropertyKeyException() throws RuntimeException {
        throw new RuntimeException("Please use setCookie method to set cookies.");
    }

    /**
     * 拋出內容型態屬性鍵值例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwContentTypeKeyException() throws RuntimeException {
        throw new RuntimeException("Content-Type can't be set by user.");
    }

    /**
     * 拋出內容處置屬性鍵值例外。
     *
     * @throws RuntimeException 拋出例外
     */
    private void throwContentDispositionKeyException() throws RuntimeException {
        throw new RuntimeException("Content-Disposition can't be set by user.");
    }

    /**
     * 拋出空的參數鍵值例外。
     *
     * @throws RuntimeException
     */
    private void throwNullParameterKeyException() throws RuntimeException {
        throw new RuntimeException("Null parameter key.");
    }

    /**
     * 拋出開啟中的例外。
     *
     * @throws RuntimeException
     */
    private void throwOpeningException() throws RuntimeException {
        throw new RuntimeException("\"".concat(url.toString()).concat("\" is opening."));
    }

    /**
     * 開啟URL。
     */
    public synchronized void open() {
        if (opening || timeoutTimer != null) {
            throwOpeningException();
        }
        opening = true;

        if (listener != null) {
            try {
                listener.onStarted();
            } catch (final Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
        try {
            // 建立連線
            conn = buildConnection(url);

            // 設定屬性
            final Set<String> propertyKeys = properties.keySet();
            for (final String propertyKey : propertyKeys) {
                conn.setRequestProperty(propertyKey, properties.get(propertyKey));
            }
            if (!cookies.isEmpty()) {
                conn.setRequestProperty(PropertyKeys.COOKIE, createCookiesString());
            }

            // 判別協定
            if (isHttpOrHttps) {
                // 設定HTTP
                buildHTTPConnection((HttpURLConnection) conn);
                if (isHttps) {
                    final HttpsURLConnection https = (HttpsURLConnection) conn;
                    https.setSSLSocketFactory(createAnySSLSocketFactory());
                    https.setHostnameVerifier(createAnyHostnameVerifier());
                }
            }

            // 設定逾時時間
            final int fixedTimeout = timeout + TIMEOUT_INTERVAL;
            conn.setReadTimeout(fixedTimeout);
            conn.setConnectTimeout(fixedTimeout);

            // 設定與執行逾時計時器
            sleepCounter = 0;
            timeoutTimer = new Thread() {
                @Override
                public void run() {
                    final int sleepCount = timeout / SLEEP_INTERVAL;
                    while (sleepCounter < sleepCount && conn != null) {
                        try {
                            Thread.sleep(SLEEP_INTERVAL);
                        } catch (final Exception ex) {
                            ex.printStackTrace(System.out);
                        }
                        synchronized (sleepCounterLock) {
                            ++sleepCounter;
                        }
                    }
                    if (conn != null) {
                        if (listener != null) {
                            try {
                                listener.onFailed("Timeout", attemptDisconnecting);
                            } catch (final Exception ex) {
                                ex.printStackTrace(System.out);
                            }
                        }
                        MagicURLNetwork.this.stop();
                        conn = null;
                    }
                    timeoutTimer = null;
                }
            };
            timeoutTimer.setPriority(Thread.MIN_PRIORITY);
            timeoutTimer.start();

            // 建立輸出串流
            final OutputStream outputStream;
            final BufferedOutputStream bufferedOutputStream;
            if (targetFile == null) {
                outputStream = new ByteArrayOutputStream();
            } else {
                outputStream = new FileOutputStream(targetFile);
            }
            bufferedOutputStream = new BufferedOutputStream(outputStream);

            // 開始處理連線
            if (conn.getDoOutput()) {
                BufferedOutputStream bosConn = new BufferedOutputStream(conn.getOutputStream());
                doSendConnection(listener, getParameters(), bosConn);
            }

            final JSONObject headersObj = new JSONObject();
            if (conn.getDoInput()) {
                BufferedInputStream bisConn = null;
                if (conn instanceof HttpURLConnection) {
                    final HttpURLConnection http = (HttpURLConnection) conn;
                    final int responseCode = http.getResponseCode();
                    headersObj.put("Response-Code", responseCode); // 儲存Response Code

                    if (responseCode != 200) {
                        if (acceptNot200HTTPResponseCode) {
                            bisConn = new BufferedInputStream(http.getErrorStream());
                        } else {
                            throw new Exception(String.format("Response Code = %d.", responseCode));
                        }
                    }
                }
                if (bisConn == null) {
                    bisConn = new BufferedInputStream(conn.getInputStream());
                }

                // 儲存標頭
                final Map<String, List<String>> headers = conn.getHeaderFields();
                final Set<String> keys = headers.keySet();
                for (final String key : keys) {
                    if (key == null) {
                        continue;
                    }
                    final JSONArray array = new JSONArray();
                    final List<String> list = headers.get(key);
                    if (list == null) {
                        continue;
                    }
                    for (final String item : list) {
                        if (item == null) {
                            continue;
                        }
                        array.put(item);
                    }
                    headersObj.put(key, array);
                }

                headersObj.put("Content-Length", getContentLength());

                doReceiveConnection(listener, headersObj, bisConn, bufferedOutputStream);
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            resultHeader = headersObj;
            if (targetFile == null) {
                result = ((ByteArrayOutputStream) outputStream).toByteArray();
            } else {
                result = targetFile;
            }
            if (listener != null) {
                if (attemptDisconnecting) {
                    try {
                        listener.onFailed("Finish but the connection has been disconnected.", attemptDisconnecting);
                    } catch (final Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }
                try {
                    listener.onFinished(resultHeader, result);
                } catch (final Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        } catch (final Exception ex) {
            if (listener != null) {
                try {
                    listener.onFailed(ex.getMessage(), attemptDisconnecting);
                } catch (final Exception ex2) {
                    ex2.printStackTrace(System.out);
                }
                try {
                    listener.onFinished(null, result);
                } catch (final Exception ex2) {
                    ex2.printStackTrace(System.out);
                }
            }
        } finally {
            stop();
            conn = null;
            opening = false;
        }
    }

    /**
     * <p>
     * 取得URLNetwork的內容長度。
     * </p>
     *
     * <p>
     * Android可能無法支援URLNetwork的getContentLength方法，故自行實作之。
     * </p>
     *
     * @return 傳回URLNetwork的內容長度
     */
    private long getContentLength() {
        final String value = conn.getHeaderField("content-length");
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (final Exception e) {
            }
        }
        return -1;
    }

    /**
     * 停止連線。
     */
    private void stop() {
        try {
            conn.getInputStream().close();
        } catch (final Exception ex) {

        }
        try {
            conn.getOutputStream().close();
        } catch (final Exception ex) {

        }
        if (conn instanceof HttpURLConnection) {
            final HttpURLConnection http = (HttpURLConnection) conn;
            try {
                http.getErrorStream().close();
            } catch (final Exception ex) {

            }
            try {
                http.disconnect();
            } catch (final Exception ex) {

            }
        }
    }

    /**
     * 建立連線。
     *
     * @param url 要用來建立連線的URL
     * @throws Exception 拋出例外
     * @return 傳回URLConnection物件
     */
    protected abstract URLConnection buildConnection(final URL url) throws Exception;

    /**
     * 建立HTTP(S)連線。
     *
     * @param http 要建立的HTTP連線
     * @throws Exception 拋出例外
     */
    protected abstract void buildHTTPConnection(final HttpURLConnection http) throws Exception;

    /**
     * 處理傳送連線。
     *
     * @param listener MagicURLNetwork開啟後的監聽者
     * @param parameters 要傳遞的參數
     * @param bosConn 輸出串流，處理傳送給從伺服器的資料
     * @throws Exception 拋出例外
     */
    protected abstract void doSendConnection(final NetworkListener listener, final HashMap<String, Body> parameters, final BufferedOutputStream bosConn) throws Exception;

    /**
     * 處理接收連線。
     *
     * @param listener MagicURLNetwork開啟後的監聽者
     * @param resultHeader URL回傳的標頭
     * @param bisConn 輸入串流，處理從伺服器傳回來的資料
     * @param bos 輸出串流，處理從伺服器傳回來的資料
     * @throws Exception 拋出例外
     */
    protected abstract void doReceiveConnection(final NetworkListener listener, final JSONObject resultHeader, final BufferedInputStream bisConn, final BufferedOutputStream bos) throws Exception;

    /**
     * 取得是否支援非HTTP的連線。
     *
     * @return 傳回是否支援非HTTP的連線
     */
    protected abstract boolean supportNonHTTPProtocol();
}
