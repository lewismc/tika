/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tika.mime.purifier;

import java.io.IOException;
import java.io.InputStream;

/**
 * This interface defines a minimum set of methods that
 * a {@link org.apache.tika.detect.Detector} could
 * call in order to clean the input before performing the <i>MIME type</i>
 * detection.
 * 
 * @author Davide Palmisano ( dpalmisano@gmail.com )
 */
public interface Purifier {

    /**
     * Performs the purification of the provided resettable {@link java.io.InputStream}.
     * 
     * @param inputStream a resettable {@link java.io.InputStream} to be cleaned.
     */
    void purify(InputStream inputStream) throws IOException;

}
