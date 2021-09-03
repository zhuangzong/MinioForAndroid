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

import java.util.Arrays;

/** Argument class of MinioClient.ListenBucketNotification(). */
public class ListenBucketNotificationArgs extends BucketArgs {
  private String prefix;
  private String suffix;
  private String[] events = null;

  public String prefix() {
    return prefix;
  }

  public String suffix() {
    return suffix;
  }

  public String[] events() {
    return Arrays.copyOf(events, events.length);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Argument builder of {@link ListenBucketNotificationArgs}. */
  public static final class Builder
      extends BucketArgs.Builder<Builder, ListenBucketNotificationArgs> {
    private void validateEvents(String[] events) {
      validateNotNull(events, "events");
    }

    protected void validate(ListenBucketNotificationArgs args) {
      super.validate(args);
      validateEvents(args.events);
    }

    public Builder prefix(String prefix) {
      operations.add(args -> args.prefix = prefix);
      return this;
    }

    public Builder suffix(String suffix) {
      operations.add(args -> args.suffix = suffix);
      return this;
    }

    public Builder events(String[] events) {
      validateEvents(events);
      final String[] eventsCopy = Arrays.copyOf(events, events.length);
      operations.add(args -> args.events = eventsCopy);
      return this;
    }
  }
}
