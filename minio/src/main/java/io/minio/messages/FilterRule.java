/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2017 MinIO, Inc.
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

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Helper class to denote FilterRule configuration of {@link CloudFunctionConfiguration}, {@link
 * QueueConfiguration} or {@link TopicConfiguration}.
 */
@Root(name = "FilterRule", strict = false)
public class FilterRule {
  @Element(name = "Name")
  private String name;

  @Element(name = "Value")
  private String value;

  public FilterRule() {}

  public FilterRule(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /** Returns filter name. */
  public String name() {
    return name;
  }

  /** Returns filter value. */
  public String value() {
    return value;
  }
}
