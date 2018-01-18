/*
 *
 * Copyright 2015-2018 magiclen.org
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

import java.net.HttpURLConnection;
import java.net.URL;
import org.magiclen.magicurlnetwork.parameters.Body;

/**
 * 對URL發出POST需求(Request)，常用於上傳文字訊息或是檔案，直接把一個資料的內容原封不動地放在Body傳送。
 *
 * @author Magic Len
 * @see MagicURLNetwork
 * @see Body
 * @see NetworkListener
 * @see MagicPUT
 */
public class MagicSinglePOST extends MagicPUT {

    // -----建構子-----
    /**
     * 建構子，傳入URL字串。
     *
     * @param url 傳入URL字串
     */
    public MagicSinglePOST(final String url) {
	super(url);
    }

    /**
     * 建構子，傳入URL物件。
     *
     * @param url 傳入URL物件
     */
    public MagicSinglePOST(final URL url) {
	super(url);
    }

    // -----物件方法-----
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
}
