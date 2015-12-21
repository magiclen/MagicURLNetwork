/*
 *
 * Copyright 2015 magiclen.org
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.io.FileNotFoundException;

/**
 * 檔案資料。
 *
 * @author Magic Len
 * @see Body
 */
public class FileBody extends Body {

    //-----物件變數-----
    /**
     * 檔案來源。
     */
    private File source;
    /**
     * 內容型態。
     */
    private String contentType;

    //-----建構子-----
    /**
     * 建構子，使用檔案作為資料來源。
     *
     * @param source 傳入檔案
     * @throws Exception 拋出例外
     */
    public FileBody(final File source) throws Exception {
	if (source == null) {
	    throw new NullPointerException("Null body source.");
	} else if (!source.exists()) {
	    throw new FileNotFoundException("Body source doesn't exist.");
	} else if (!source.isFile()) {
	    throw new RuntimeException("Body source is not a file.");
	}
	this.source = source;
	contentType = URLConnection.guessContentTypeFromName(source.getName());
	if (contentType == null) {
	    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source))) {
		contentType = URLConnection.guessContentTypeFromStream(bis);
	    }
	}
    }

    // -----物件方法-----
    /**
     * 取得資料來源。
     *
     * @return 傳回資料來源
     */
    @Override
    public File getSource() {
	return source;
    }

    /**
     * 取得資料字串。
     *
     * @return 傳回資料字串
     */
    @Override
    public String toString() {
	return source.getAbsolutePath();
    }

    /**
     * 取得參數資料的類型。
     *
     * @return 傳回參數資料的類型
     */
    @Override
    public BodyType getBodyType() {
	return BodyType.FILE;
    }

    /**
     * 取得資料的MIME類型。
     *
     * @return 傳回資料的MIME類型
     */
    @Override
    public String getContentType() {
	return contentType;
    }
}
