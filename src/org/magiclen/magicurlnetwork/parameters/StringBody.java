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
package org.magiclen.magicurlnetwork.parameters;

/**
 * 字串資料。
 *
 * @author Magic Len
 * @see Body
 */
public class StringBody extends Body {

    //-----物件變數-----
    /**
     * 字串來源。
     */
    private String source;

    //-----建構子-----
    /**
     * 建構子，使用字串作為資料來源。
     *
     * @param source 傳入字串
     */
    public StringBody(final String source) {
	if (source == null) {
	    throw new NullPointerException("Null body source.");
	}
	this.source = source.trim();
    }

    // -----物件方法-----
    /**
     * 取得資料來源。
     *
     * @return 傳回資料來源
     */
    @Override
    public String getSource() {
	return source;
    }

    /**
     * 取得資料字串。
     *
     * @return 傳回資料字串
     */
    @Override
    public String toString() {
	return source;
    }

    /**
     * 取得參數資料的類型。
     *
     * @return 傳回參數資料的類型
     */
    @Override
    public BodyType getBodyType() {
	return BodyType.STRING;
    }

    /**
     * 取得資料的MIME類型。
     *
     * @return 傳回資料的MIME類型
     */
    @Override
    public String getContentType() {
	return "text/plain";
    }
}
