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

public abstract class ObjectReadArgs extends ObjectVersionArgs {
  protected ServerSideEncryptionCustomerKey ssec;

  public ServerSideEncryptionCustomerKey ssec() {
    return ssec;
  }

  protected void validateSsec(HttpUrl url) {
    checkSse(ssec, url);
  }

  public abstract static class Builder<B extends Builder<B, A>, A extends ObjectReadArgs>
      extends ObjectVersionArgs.Builder<B, A> {
    @SuppressWarnings("unchecked") // Its safe to type cast to B as B is inherited by this class
    public B ssec(ServerSideEncryptionCustomerKey ssec) {
      operations.add(args -> args.ssec = ssec);
      return (B) this;
    }
  }
}
