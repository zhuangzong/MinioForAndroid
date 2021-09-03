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

import okhttp3.HttpUrl;

/** Base argument class holds object name and version ID along with bucket information. */
public abstract class ObjectArgs extends BucketArgs {
  protected String objectName;

  public String object() {
    return objectName;
  }

  protected void checkSse(ServerSideEncryption sse, HttpUrl url) {
    if (sse == null) {
      return;
    }

    if (sse.type().requiresTls() && !url.isHttps()) {
      throw new IllegalArgumentException(
          sse.type().name() + " operations must be performed over a secure connection.");
    }
  }

  /** Base argument builder class for {@link ObjectArgs}. */
  public abstract static class Builder<B extends Builder<B, A>, A extends ObjectArgs>
      extends BucketArgs.Builder<B, A> {
    protected void validateObjectName(String name) {
      validateNotEmptyString(name, "object name");
    }

    protected void validate(A args) {
      super.validate(args);
      validateObjectName(args.objectName);
    }

    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B object(String name) {
      validateObjectName(name);
      operations.add(args -> args.objectName = name);
      return (B) this;
    }
  }
}
