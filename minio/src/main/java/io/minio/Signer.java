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
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.BaseEncoding;

import org.joda.time.DateTime;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;

/** Amazon AWS S3 signature V4 signer. */
class Signer {
  //
  // Excerpts from @lsegal - https://github.com/aws/aws-sdk-js/issues/659#issuecomment-120477258
  //
  //  User-Agent:
  //
  //      This is ignored from signing because signing this causes problems with generating
  // pre-signed URLs
  //      (that are executed by other agents) or when customers pass requests through proxies, which
  // may
  //      modify the user-agent.
  //
  //  Content-Length:
  //
  //      This is ignored from signing because generating a pre-signed URL should not provide a
  // content-length
  //      constraint, specifically when vending a S3 pre-signed PUT URL. The corollary to this is
  // that when
  //      sending regular requests (non-pre-signed), the signature contains a checksum of the body,
  // which
  //      implicitly validates the payload length (since changing the number of bytes would change
  // the checksum)
  //      and therefore this header is not valuable in the signature.
  //
  //  Content-Type:
  //
  //      Signing this header causes quite a number of problems in browser environments, where
  // browsers
  //      like to modify and normalize the content-type header in different ways. There is more
  // information
  //      on this in https://github.com/aws/aws-sdk-js/issues/244. Avoiding this field simplifies
  // logic
  //      and reduces the possibility of future bugs
  //
  //  Authorization:
  //
  //      Is skipped for obvious reasons
  //
  private static final Set<String> IGNORED_HEADERS = new HashSet<>();

  static {
    IGNORED_HEADERS.add("authorization");
    IGNORED_HEADERS.add("content-type");
    IGNORED_HEADERS.add("content-length");
    IGNORED_HEADERS.add("user-agent");
  }

  private Request request;
  private String contentSha256;
  private DateTime date;
  private String region;
  private String accessKey;
  private String secretKey;
  private String prevSignature;

  private String scope;
  private Map<String, String> canonicalHeaders;
  private String signedHeaders;
  private HttpUrl url;
  private String canonicalQueryString;
  private String canonicalRequest;
  private String canonicalRequestHash;
  private String stringToSign;
  private byte[] signingKey;
  private String signature;
  private String authorization;

  /**
   * Create new Signer object for V4.
   *
   * @param request HTTP Request object.
   * @param contentSha256 SHA-256 hash of request payload.
   * @param date Date to be used to sign the request.
   * @param region Amazon AWS region for the request.
   * @param accessKey Access Key string.
   * @param secretKey Secret Key string.
   * @param prevSignature Previous signature of chunk upload.
   */
  public Signer(
      Request request,
      String contentSha256,
      DateTime date,
      String region,
      String accessKey,
      String secretKey,
      String prevSignature) {
    this.request = request;
    this.contentSha256 = contentSha256;
    this.date = date;
    this.region = region;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.prevSignature = prevSignature;
  }

  private void setScope() {
    this.scope = Time.SIGNER_DATE_FORMAT.print(this.date) + "/" + this.region + "/s3/aws4_request";
  }

  private void setCanonicalHeaders() {
    this.canonicalHeaders = new TreeMap<>();

    Headers headers = this.request.headers();
    for (String name : headers.names()) {
      String signedHeader = name.toLowerCase(Locale.US);
      if (!IGNORED_HEADERS.contains(signedHeader)) {
        this.canonicalHeaders.put(signedHeader, headers.get(name));
      }
    }

    this.signedHeaders = Joiner.on(";").join(this.canonicalHeaders.keySet());
  }

  private void setCanonicalQueryString() {
    String encodedQuery = this.url.encodedQuery();
    if (encodedQuery == null) {
      this.canonicalQueryString = "";
      return;
    }

    // Building a multimap which only order keys, ordering values is not performed
    // until MinIO server supports it.
    Multimap<String, String> signedQueryParams =
        MultimapBuilder.treeKeys().arrayListValues().build();

    for (String queryParam : encodedQuery.split("&")) {
      String[] tokens = queryParam.split("=");
      if (tokens.length > 1) {
        signedQueryParams.put(tokens[0], tokens[1]);
      } else {
        signedQueryParams.put(tokens[0], "");
      }
    }

    this.canonicalQueryString =
        Joiner.on("&").withKeyValueSeparator("=").join(signedQueryParams.entries());
  }

  private void setCanonicalRequest() throws NoSuchAlgorithmException {
    setCanonicalHeaders();
    this.url = this.request.url();
    setCanonicalQueryString();

    // CanonicalRequest =
    //   HTTPRequestMethod + '\n' +
    //   CanonicalURI + '\n' +
    //   CanonicalQueryString + '\n' +
    //   CanonicalHeaders + '\n' +
    //   SignedHeaders + '\n' +
    //   HexEncode(Hash(RequestPayload))
    this.canonicalRequest =
        this.request.method()
            + "\n"
            + this.url.encodedPath()
            + "\n"
            + this.canonicalQueryString
            + "\n"
            + Joiner.on("\n").withKeyValueSeparator(":").join(this.canonicalHeaders)
            + "\n\n"
            + this.signedHeaders
            + "\n"
            + this.contentSha256;

    this.canonicalRequestHash = Digest.sha256Hash(this.canonicalRequest);
  }

  private void setStringToSign() {
    this.stringToSign =
        "AWS4-HMAC-SHA256"
            + "\n"
            + Time.AMZ_DATE_FORMAT.print(this.date)
            + "\n"
            + this.scope
            + "\n"
            + this.canonicalRequestHash;
  }

  private void setChunkStringToSign() throws NoSuchAlgorithmException {
    this.stringToSign =
        "AWS4-HMAC-SHA256-PAYLOAD"
            + "\n"
            + Time.AMZ_DATE_FORMAT.print(this.date)
            + "\n"
            + this.scope
            + "\n"
            + this.prevSignature
            + "\n"
            + Digest.sha256Hash("")
            + "\n"
            + this.contentSha256;
  }

  private void setSigningKey() throws NoSuchAlgorithmException, InvalidKeyException {
    String aws4SecretKey = "AWS4" + this.secretKey;

    byte[] dateKey =
        sumHmac(
            aws4SecretKey.getBytes(StandardCharsets.UTF_8),
                Time.SIGNER_DATE_FORMAT.print(this.date).getBytes(StandardCharsets.UTF_8));

    byte[] dateRegionKey = sumHmac(dateKey, this.region.getBytes(StandardCharsets.UTF_8));

    byte[] dateRegionServiceKey = sumHmac(dateRegionKey, "s3".getBytes(StandardCharsets.UTF_8));

    this.signingKey =
        sumHmac(dateRegionServiceKey, "aws4_request".getBytes(StandardCharsets.UTF_8));
  }

  private void setSignature() throws NoSuchAlgorithmException, InvalidKeyException {
    byte[] digest = sumHmac(this.signingKey, this.stringToSign.getBytes(StandardCharsets.UTF_8));
    this.signature = BaseEncoding.base16().encode(digest).toLowerCase(Locale.US);
  }

  private void setAuthorization() {
    this.authorization =
        "AWS4-HMAC-SHA256 Credential="
            + this.accessKey
            + "/"
            + this.scope
            + ", SignedHeaders="
            + this.signedHeaders
            + ", Signature="
            + this.signature;
  }

  /** Returns chunk signature calculated using given arguments. */
  public static String getChunkSignature(
      String chunkSha256, DateTime date, String region, String secretKey, String prevSignature)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Signer signer = new Signer(null, chunkSha256, date, region, null, secretKey, prevSignature);
    signer.setScope();
    signer.setChunkStringToSign();
    signer.setSigningKey();
    signer.setSignature();

    return signer.signature;
  }

  /** Returns seed signature for given request. */
  public static String getChunkSeedSignature(Request request, String region, String secretKey)
      throws NoSuchAlgorithmException, InvalidKeyException {
    String contentSha256 = request.header("x-amz-content-sha256");
    DateTime date = DateTime.parse(request.header("x-amz-date"), Time.AMZ_DATE_FORMAT);

    Signer signer = new Signer(request, contentSha256, date, region, null, secretKey, null);
    signer.setScope();
    signer.setCanonicalRequest();
    signer.setStringToSign();
    signer.setSigningKey();
    signer.setSignature();

    return signer.signature;
  }

  /** Returns signed request object for given request, region, access key and secret key. */
  public static Request signV4(Request request, String region, String accessKey, String secretKey)
      throws NoSuchAlgorithmException, InvalidKeyException {
    String contentSha256 = request.header("x-amz-content-sha256");
    DateTime date = DateTime.parse(request.header("x-amz-date"), Time.AMZ_DATE_FORMAT);

    Signer signer = new Signer(request, contentSha256, date, region, accessKey, secretKey, null);
    signer.setScope();
    signer.setCanonicalRequest();
    signer.setStringToSign();
    signer.setSigningKey();
    signer.setSignature();
    signer.setAuthorization();

    return request.newBuilder().header("Authorization", signer.authorization).build();
  }

  private void setPresignCanonicalRequest(int expires) throws NoSuchAlgorithmException {
    this.canonicalHeaders = new TreeMap<>();
    this.canonicalHeaders.put("host", this.request.headers().get("Host"));
    this.signedHeaders = "host";

    HttpUrl.Builder urlBuilder = this.request.url().newBuilder();
    // order of queryparam addition is important ie has to be sorted.
    urlBuilder.addEncodedQueryParameter(
        S3Escaper.encode("X-Amz-Algorithm"), S3Escaper.encode("AWS4-HMAC-SHA256"));
    urlBuilder.addEncodedQueryParameter(
        S3Escaper.encode("X-Amz-Credential"), S3Escaper.encode(this.accessKey + "/" + this.scope));
    urlBuilder.addEncodedQueryParameter(
        S3Escaper.encode("X-Amz-Date"), S3Escaper.encode(Time.AMZ_DATE_FORMAT.print(this.date)));
    urlBuilder.addEncodedQueryParameter(
        S3Escaper.encode("X-Amz-Expires"), S3Escaper.encode(Integer.toString(expires)));
    urlBuilder.addEncodedQueryParameter(
        S3Escaper.encode("X-Amz-SignedHeaders"), S3Escaper.encode(this.signedHeaders));
    this.url = urlBuilder.build();

    setCanonicalQueryString();

    this.canonicalRequest =
        this.request.method()
            + "\n"
            + this.url.encodedPath()
            + "\n"
            + this.canonicalQueryString
            + "\n"
            + Joiner.on("\n").withKeyValueSeparator(":").join(this.canonicalHeaders)
            + "\n\n"
            + this.signedHeaders
            + "\n"
            + this.contentSha256;

    this.canonicalRequestHash = Digest.sha256Hash(this.canonicalRequest);
  }

  /**
   * Returns pre-signed HttpUrl object for given request, region, access key, secret key and expires
   * time.
   */
  public static HttpUrl presignV4(
      Request request, String region, String accessKey, String secretKey, int expires)
      throws NoSuchAlgorithmException, InvalidKeyException {
    String contentSha256 = "UNSIGNED-PAYLOAD";
    DateTime date = DateTime.parse(request.header("x-amz-date"), Time.AMZ_DATE_FORMAT);

    Signer signer = new Signer(request, contentSha256, date, region, accessKey, secretKey, null);
    signer.setScope();
    signer.setPresignCanonicalRequest(expires);
    signer.setStringToSign();
    signer.setSigningKey();
    signer.setSignature();

    return signer
        .url
        .newBuilder()
        .addEncodedQueryParameter(
            S3Escaper.encode("X-Amz-Signature"), S3Escaper.encode(signer.signature))
        .build();
  }

  /** Returns credential string of given access key, date and region. */
  public static String credential(String accessKey, DateTime date, String region) {
    return accessKey
        + "/"
        + Time.SIGNER_DATE_FORMAT.print(date)
        + "/"
        + region
        + "/s3/aws4_request";
  }

  /** Returns pre-signed post policy string for given stringToSign, secret key, date and region. */
  public static String postPresignV4(
      String stringToSign, String secretKey, DateTime date, String region)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Signer signer = new Signer(null, null, date, region, null, secretKey, null);
    signer.stringToSign = stringToSign;
    signer.setSigningKey();
    signer.setSignature();

    return signer.signature;
  }

  /** Returns HMacSHA256 digest of given key and data. */
  public static byte[] sumHmac(byte[] key, byte[] data)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Mac mac = Mac.getInstance("HmacSHA256");

    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    mac.update(data);

    return mac.doFinal();
  }
}
