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

package io.minio;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

/** Post policy information to be used to generate presigned post policy form-data. */
public class PostPolicy {
  private static final String ALGORITHM = "AWS4-HMAC-SHA256";

  private final String bucketName;
  private final String objectName;
  private final boolean startsWith;
  private final DateTime expirationDate;
  private String contentType;
  private int successActionStatus;
  private String contentEncoding;
  private long contentRangeStart;
  private long contentRangeEnd;

  public PostPolicy(String bucketName, String objectName, DateTime expirationDate)
      throws IllegalArgumentException {
    this(bucketName, objectName, false, expirationDate);
  }

  /**
   * Creates PostPolicy for given bucket name, object name, string to match object name starting
   * with and expiration time.
   */
  public PostPolicy(
      String bucketName, String objectName, boolean startsWith, DateTime expirationDate)
      throws IllegalArgumentException {
    if (bucketName == null) {
      throw new IllegalArgumentException("null bucket name");
    }
    this.bucketName = bucketName;

    if (objectName == null) {
      throw new IllegalArgumentException("null object name or prefix");
    }
    this.objectName = objectName;

    this.startsWith = startsWith;

    if (expirationDate == null) {
      throw new IllegalArgumentException("null expiration date");
    }
    this.expirationDate = expirationDate;
  }

  /** Sets content type. */
  public void setContentType(String contentType) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(contentType)) {
      throw new IllegalArgumentException("empty content type");
    }

    this.contentType = contentType;
  }

  /** Sets success action status. */
  public void setSuccessActionStatus(int successActionStatus) throws IllegalArgumentException {
    if (!(successActionStatus == 200 || successActionStatus == 201 || successActionStatus == 204)) {
      throw new IllegalArgumentException(
          "Invalid action status, acceptable values are 200, 201, or 204");
    }

    this.successActionStatus = successActionStatus;
  }

  /** Sets content encoding. */
  public void setContentEncoding(String contentEncoding) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(contentEncoding)) {
      throw new IllegalArgumentException("empty content encoding");
    }

    this.contentEncoding = contentEncoding;
  }

  /** Sets content length. */
  public void setContentLength(long contentLength) throws IllegalArgumentException {
    if (contentLength <= 0) {
      throw new IllegalArgumentException("negative content length");
    }

    this.setContentRange(contentLength, contentLength);
  }

  /** Sets content range. */
  public void setContentRange(long startRange, long endRange) throws IllegalArgumentException {
    if (startRange <= 0 || endRange <= 0) {
      throw new IllegalArgumentException("negative start/end range");
    }

    if (startRange > endRange) {
      throw new IllegalArgumentException("start range is higher than end range");
    }

    this.contentRangeStart = startRange;
    this.contentRangeEnd = endRange;
  }

  /** Returns bucket name. */
  public String bucketName() {
    return this.bucketName;
  }

  private byte[] marshalJson(ArrayList<String[]> conditions) {
    StringBuilder sb = new StringBuilder();
    Joiner joiner = Joiner.on("\",\"");

    sb.append("{");

    if (expirationDate != null) {
      sb.append(
          "\"expiration\":" + "\"" +Time.EXPIRATION_DATE_FORMAT.print(expirationDate) + "\"");
    }

    if (!conditions.isEmpty()) {
      sb.append(",\"conditions\":[");

      ListIterator<String[]> iterator = conditions.listIterator();
      while (iterator.hasNext()) {
        sb.append("[\"" + joiner.join(iterator.next()) + "\"]");
        if (iterator.hasNext()) {
          sb.append(",");
        }
      }

      sb.append("]");
    }

    sb.append("}");

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /** Returns form data of this post policy setting the provided region. */
  public Map<String, String> formData(String accessKey, String secretKey, String region)
      throws NoSuchAlgorithmException, InvalidKeyException, IllegalArgumentException {

    if (Strings.isNullOrEmpty(region)) {
      throw new IllegalArgumentException("empty region");
    }

    return Collections.unmodifiableMap(makeFormData(accessKey, secretKey, region));
  }

  protected Map<String, String> makeFormData(String accessKey, String secretKey, String region)
      throws NoSuchAlgorithmException, InvalidKeyException {

    ArrayList<String[]> conditions = new ArrayList<>();
    Map<String, String> formData = new HashMap<>();

    conditions.add(new String[] {"eq", "$bucket", this.bucketName});
    formData.put("bucket", this.bucketName);

    if (this.startsWith) {
      conditions.add(new String[] {"starts-with", "$key", this.objectName});
      formData.put("key", this.objectName);
    } else {
      conditions.add(new String[] {"eq", "$key", this.objectName});
      formData.put("key", this.objectName);
    }

    if (this.contentType != null) {
      conditions.add(new String[] {"eq", "$Content-Type", this.contentType});
      formData.put("Content-Type", this.contentType);
    }

    if (this.contentEncoding != null) {
      conditions.add(new String[] {"eq", "$Content-Encoding", this.contentEncoding});
      formData.put("Content-Encoding", this.contentEncoding);
    }

    if (this.successActionStatus > 0) {
      conditions.add(
          new String[] {
            "eq", "$success_action_status", Integer.toString(this.successActionStatus)
          });
      formData.put("success_action_status", Integer.toString(this.successActionStatus));
    }

    if (this.contentRangeStart > 0 && this.contentRangeEnd > 0) {
      conditions.add(
          new String[] {
            "content-length-range",
            Long.toString(this.contentRangeStart),
            Long.toString(this.contentRangeEnd)
          });
    }

    conditions.add(new String[] {"eq", "$x-amz-algorithm", ALGORITHM});
    formData.put("x-amz-algorithm", ALGORITHM);

    DateTime utcNow = DateTime.now(Time.UTC);
    String credential = Signer.credential(accessKey, utcNow, region);
    conditions.add(new String[] {"eq", "$x-amz-credential", credential});
    formData.put("x-amz-credential", credential);

    String amzDate =Time.AMZ_DATE_FORMAT.print(utcNow) ;
    conditions.add(new String[] {"eq", "$x-amz-date", amzDate});
    formData.put("x-amz-date", amzDate);

    String policybase64 = BaseEncoding.base64().encode(this.marshalJson(conditions));
    String signature = Signer.postPresignV4(policybase64, secretKey, utcNow, region);

    formData.put("policy", policybase64);
    formData.put("x-amz-signature", signature);

    return formData;
  }
}
