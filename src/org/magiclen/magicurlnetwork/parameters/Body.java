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

import java.io.File;
import org.magiclen.mson.JSONType;

/**
 * <p>
 * 參數資料內容。
 * </p>
 *
 * <p>
 * 如果想要傳送數值資料，可以使用NUMBER方法；如果想要傳送字串資料，可以使用STRING方法；如果想要傳送JSON資料，可以使用JSON方法；如果想要傳送檔案，可以使用FILE方法；如果想要將多筆資料以陣列形式傳送，可以使用ARRAY方法。
 * </p>
 *
 * @author Magic Len
 * @see FileBody
 * @see StringBody
 * @see JSONBody
 * @see NumberBody
 * @see ArrayBody
 */
public abstract class Body {

    // -----類別方法-----
    /**
     * 使用多個Body作為資料來源。
     *
     * @param source 傳入Body
     * @return 傳回ArrayBody物件
     */
    public static ArrayBody ARRAY(final Body... source) {
	return new ArrayBody(source);
    }

    /**
     * 建構子，使用檔案作為資料來源。
     *
     * @param source 傳入檔案
     * @throws Exception 拋出例外
     * @return 傳回FileBody物件
     */
    public static FileBody FILE(final File source) throws Exception {
	return new FileBody(source);
    }

    /**
     * 使用JSON作為資料來源。
     *
     * @param source 傳入JSON
     * @return 傳回JSONBody物件
     */
    public static JSONBody JSON(final JSONType source) {
	return new JSONBody(source);
    }

    /**
     * 使用數值作為資料來源。
     *
     * @param source 傳入檔案
     * @return 傳回NumberBody物件
     */
    public static NumberBody NUMBER(final Number source) {
	return new NumberBody(source);
    }

    /**
     * 使用字串作為資料來源。
     *
     * @param source 傳入字串
     * @return 傳回StringBody物件
     */
    public static StringBody STRING(final String source) {
	return new StringBody(source);
    }

    //-----物件方法-----
    /**
     * 取得資料來源。
     *
     * @return 傳回資料來源
     */
    public abstract Object getSource();

    /**
     * 取得資料字串。
     *
     * @return 傳回資料字串
     */
    @Override
    public abstract String toString();

    /**
     * 取得參數資料的類型。
     *
     * @return 傳回參數資料的類型
     */
    public abstract BodyType getBodyType();

    /**
     * 取得資料的MIME類型。
     *
     * @return 傳回資料的MIME類型
     */
    public abstract String getContentType();
}
