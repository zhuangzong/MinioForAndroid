/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2019 MinIO, Inc.
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
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

/**
 * Object representation of request XML of <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/API_SelectObjectContent.html">SelectObjectContent
 * API</a>.
 */
@Root(name = "SelectObjectContentRequest")
@Namespace(reference = "http://s3.amazonaws.com/doc/2006-03-01/")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "URF_UNREAD_FIELD")
public class SelectObjectContentRequest {
  @Element(name = "Expression")
  private String expression;

  @Element(name = "ExpressionType")
  private String expressionType = "SQL";

  @Element(name = "RequestProgress", required = false)
  private RequestProgress requestProgress;

  @Element(name = "InputSerialization")
  private InputSerialization inputSerialization;

  @Element(name = "OutputSerialization")
  private OutputSerialization outputSerialization;

  @Element(name = "ScanRange", required = false)
  private ScanRange scanRange;

  /** Constructs new SelectObjectContentRequest object for given parameters. */
  public SelectObjectContentRequest(
      String expression,
      boolean requestProgress,
      InputSerialization is,
      OutputSerialization os,
      Long scanStartRange,
      Long scanEndRange) {
    this.expression = expression;
    if (requestProgress) {
      this.requestProgress = new RequestProgress();
    }
    this.inputSerialization = is;
    this.outputSerialization = os;
    if (scanStartRange != null || scanEndRange != null) {
      this.scanRange = new ScanRange(scanStartRange, scanEndRange);
    }
  }
}
