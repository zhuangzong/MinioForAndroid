/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2019 MinIO, Inc.
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

import io.minio.errors.InternalException;

/** A source object defintion for {@link ComposeObjectArgs}. */
public class ComposeSource extends ObjectConditionalReadArgs {
  private Long objectSize = null;
  private Multimap<String, String> headers = null;

  protected ComposeSource() {}

  public ComposeSource(ObjectConditionalReadArgs args) {
    this.extraHeaders = args.extraHeaders;
    this.extraQueryParams = args.extraQueryParams;
    this.bucketName = args.bucketName;
    this.region = args.region;
    this.objectName = args.objectName;
    this.versionId = args.versionId;
    this.ssec = args.ssec;
    this.offset = args.offset;
    this.length = args.length;
    this.matchETag = args.matchETag;
    this.notMatchETag = args.notMatchETag;
    this.modifiedSince = args.modifiedSince;
    this.unmodifiedSince = args.unmodifiedSince;
  }

  private void throwException(long objectsize, long arg, String argName) {
    StringBuilder builder =
        new StringBuilder().append("source ").append(bucketName).append("/").append(objectName);

    if (versionId != null) {
      builder.append("?versionId=").append(versionId);
    }

    builder
        .append(": ")
        .append(argName)
        .append(" ")
        .append(arg)
        .append(" is beyond object size ")
        .append(objectSize);

    throw new IllegalArgumentException(builder.toString());
  }

  private void validateSize(long objectSize) {
    if (offset != null && offset >= objectSize) {
      throwException(objectSize, offset, "offset");
    }

    if (length != null) {
      if (length > objectSize) {
        throwException(objectSize, length, "length");
      }

      if (offset + length > objectSize) {
        throwException(objectSize, offset + length, "compose size");
      }
    }
  }

  public void buildHeaders(long objectSize, String etag) {
    validateSize(objectSize);
    this.objectSize = Long.valueOf(objectSize);

    String copySource = S3Escaper.encodePath(bucketName + "/" + objectName);
    if (versionId != null) {
      copySource += "?versionId=" + S3Escaper.encode(versionId);
    }

    Multimap<String, String> headers = HashMultimap.create();
    headers.put("x-amz-copy-source", copySource);
    headers.put("x-amz-copy-source-if-match", (matchETag != null) ? matchETag : etag);

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

    if (ssec != null) {
      headers.putAll(Multimaps.forMap(ssec.copySourceHeaders()));
    }

    this.headers = Multimaps.unmodifiableMultimap(headers);
  }

  public long objectSize() throws InternalException {
    if (this.objectSize == null) {
      throw new InternalException(
          "buildHeaders(long objectSize, String etag) must be called prior to this method invocation");
    }

    return this.objectSize;
  }

  public Multimap<String, String> headers() throws InternalException {
    if (this.headers == null) {
      throw new InternalException(
          "buildHeaders(long objectSize, String etag) must be called prior to this method invocation");
    }

    return this.headers;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link ComposeSource}. */
  public static final class Builder
      extends ObjectConditionalReadArgs.Builder<Builder, ComposeSource> {}
}
