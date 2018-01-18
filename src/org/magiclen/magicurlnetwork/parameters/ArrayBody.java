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
package org.magiclen.magicurlnetwork.parameters;

import java.util.ArrayList;
import java.util.List;
import org.magiclen.json.JSONArray;

/**
 * 陣列資料，可包含多個其他類型的資料內容。
 *
 * @author Magic Len
 * @see Body
 */
public class ArrayBody extends Body {

    // -----類別方法-----
    /**
     * 展開Body成為清單。
     *
     * @param body 傳入Body
     * @return 傳回Body清單
     */
    private static List<Body> extractBody(final Body body) {
	if (body == null) {
	    return null;
	}
	final ArrayList<Body> bodyList = new ArrayList<>();
	final BodyType bodyType = body.getBodyType();
	if (bodyType == BodyType.ARRAY) {
	    final Body[] bodyArray = (Body[]) body.getSource();
	    for (final Body b : bodyArray) {
		final List<Body> extractedBodyList = extractBody(b);
		if (extractedBodyList == null) {
		    continue;
		}
		bodyList.addAll(extractedBodyList);
	    }
	} else {
	    bodyList.add(body);
	}
	return bodyList;
    }

    // -----物件變數-----
    /**
     * Body來源。
     */
    private Body[] source;

    // -----建構子-----
    /**
     * 建構子，使用多個Body作為資料來源。
     *
     * @param source 傳入Body
     */
    public ArrayBody(final Body... source) {
	if (source == null) {
	    throw new NullPointerException("Null body source.");
	}
	final ArrayList<Body> bodyList = new ArrayList<>();
	for (final Body body : source) {
	    final List<Body> extractedBodyList = extractBody(body);
	    if (extractedBodyList == null) {
		continue;
	    }
	    bodyList.addAll(extractedBodyList);
	}
	final int size = bodyList.size();
	if (size == 0) {
	    throw new NullPointerException("Empty body source.");
	}
	final Body[] bodyArray = new Body[size];
	bodyList.toArray(bodyArray);
	this.source = bodyArray;
    }

    // -----物件方法-----
    /**
     * 取得資料來源。
     *
     * @return 傳回資料來源
     */
    @Override
    public Body[] getSource() {
	return source;
    }

    /**
     * 取得資料字串。
     *
     * @return 傳回資料字串
     */
    @Override
    public String toString() {
	final JSONArray array = new JSONArray();
	for (final Body body : source) {
	    array.put(body.toString());
	}
	return array.toString();
    }

    /**
     * 取得參數資料的類型。
     *
     * @return 傳回參數資料的類型
     */
    @Override
    public BodyType getBodyType() {
	return BodyType.ARRAY;
    }

    /**
     * 取得資料的MIME類型。
     *
     * @return 傳回資料的MIME類型
     */
    @Override
    public String getContentType() {
	return "*/*";
    }
}
