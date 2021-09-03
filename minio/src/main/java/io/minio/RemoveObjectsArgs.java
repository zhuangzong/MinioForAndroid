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

import java.util.LinkedList;

import io.minio.messages.DeleteObject;

/** Argument class of MinioClient.removeObjects(). */
public class RemoveObjectsArgs extends BucketArgs {
  private boolean bypassGovernanceMode;
  private Iterable<DeleteObject> objects = new LinkedList<>();
  private boolean quiet;

  public boolean bypassGovernanceMode() {
    return bypassGovernanceMode;
  }

  public Iterable<DeleteObject> objects() {
    return objects;
  }

  public boolean quiet() {
    return quiet;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link RemoveObjectsArgs}. */
  public static final class Builder extends BucketArgs.Builder<Builder, RemoveObjectsArgs> {
    public Builder bypassGovernanceMode(boolean flag) {
      operations.add(args -> args.bypassGovernanceMode = flag);
      return this;
    }

    public Builder objects(Iterable<DeleteObject> objects) {
      validateNotNull(objects, "objects");
      operations.add(args -> args.objects = objects);
      return this;
    }

    public Builder quiet(boolean flag) {
      operations.add(args -> args.quiet = flag);
      return this;
    }
  }
}
