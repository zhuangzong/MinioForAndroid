/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2020 MinIO, Inc.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.joda.time.DateTime;

public abstract class ObjectConditionalReadArgs extends ObjectReadArgs {
  protected Long offset;
  protected Long length;
  protected String matchETag;
  protected String notMatchETag;
  protected DateTime modifiedSince;
  protected DateTime unmodifiedSince;

  public Long offset() {
    return offset;
  }

  public Long length() {
    return length;
  }

  public String matchETag() {
    return matchETag;
  }

  public String notMatchETag() {
    return notMatchETag;
  }

  public DateTime modifiedSince() {
    return modifiedSince;
  }

  public DateTime unmodifiedSince() {
    return unmodifiedSince;
  }

  public Multimap<String, String> genCopyHeaders() {
    Multimap<String, String> headers = HashMultimap.create();

    String copySource = S3Escaper.encodePath("/" + bucketName + "/" + objectName);
    if (versionId != null) {
      copySource += "?versionId=" + S3Escaper.encode(versionId);
    }

    headers.put("x-amz-copy-source", copySource);

    if (ssec != null) {
      headers.putAll(Multimaps.forMap(ssec.copySourceHeaders()));
    }

    if (matchETag != null) {
      headers.put("x-amz-copy-source-if-match", matchETag);
    }

    if (notMatchETag != null) {
      headers.put("x-amz-copy-source-if-none-match", notMatchETag);
    }

    if (modifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-modified-since",
              Time.HTTP_HEADER_DATE_FORMAT.print(modifiedSince));
    }

    if (unmodifiedSince != null) {
      headers.put(
          "x-amz-copy-source-if-unmodified-since",
              Time.HTTP_HEADER_DATE_FORMAT.print(modifiedSince));
    }

    return headers;
  }

  @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
  public abstract static class Builder<B extends Builder<B, A>, A extends ObjectConditionalReadArgs>
      extends ObjectReadArgs.Builder<B, A> {
    private void validateLength(Long length) {
      if (length != null && length <= 0) {
        throw new IllegalArgumentException("length should be greater than zero");
      }
    }

    private void validateOffset(Long offset) {
      if (offset != null && offset < 0) {
        throw new IllegalArgumentException("offset should be zero or greater");
      }
    }

    public B offset(Long offset) {
      validateOffset(offset);
      operations.add(args -> args.offset = offset);
      return (B) this;
    }

    public B length(Long length) {
      validateLength(length);
      operations.add(args -> args.length = length);
      return (B) this;
    }

    public B matchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.matchETag = etag);
      return (B) this;
    }

    public B notMatchETag(String etag) {
      validateNullOrNotEmptyString(etag, "etag");
      operations.add(args -> args.notMatchETag = etag);
      return (B) this;
    }

    public B modifiedSince(DateTime modifiedTime) {
      operations.add(args -> args.modifiedSince = modifiedTime);
      return (B) this;
    }

    public B unmodifiedSince(DateTime unmodifiedTime) {
      operations.add(args -> args.unmodifiedSince = unmodifiedTime);
      return (B) this;
    }
  }
}
