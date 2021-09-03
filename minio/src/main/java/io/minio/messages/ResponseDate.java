/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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
 */

package io.minio.messages;

import com.fasterxml.jackson.annotation.JsonCreator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import java.util.Locale;

import io.minio.Time;

/** S3 specified response time wrapping {@link DateTime}. */
@Root
@Convert(ResponseDate.ResponseDateConverter.class)
public class ResponseDate {
  public static final DateTimeFormatter MINIO_RESPONSE_DATE_FORMAT =
          DateTimeFormat.forPattern("yyyy-MM-dd'T'HH':'mm':'ss'Z'").withLocale(Locale.US).withZone(Time.UTC);

  private DateTime DateTime;

  public ResponseDate() {}

  public ResponseDate(DateTime DateTime) {
    this.DateTime = DateTime;
  }

  public DateTime DateTime() {
    return DateTime;
  }

  public String toString() {
    return Time.RESPONSE_DATE_FORMAT.print(DateTime);
  }

  @JsonCreator
  public static ResponseDate fromString(String responseDateString) {
    try {
      return new ResponseDate(Time.RESPONSE_DATE_FORMAT.parseDateTime(responseDateString));
    } catch (Exception e) {
      return new ResponseDate(MINIO_RESPONSE_DATE_FORMAT.parseDateTime(responseDateString));
    }
  }

  /** XML converter class. */
  public static class ResponseDateConverter implements Converter<ResponseDate> {
    @Override
    public ResponseDate read(InputNode node) throws Exception {
      return ResponseDate.fromString(node.getValue());
    }

    @Override
    public void write(OutputNode node, ResponseDate amzDate) {
      node.setValue(amzDate.toString());
    }
  }
}
