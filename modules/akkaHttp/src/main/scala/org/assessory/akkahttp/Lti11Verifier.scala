package org.assessory.akkahttp

import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

object Lti11Verifier {

  /**
   * The OAUTH 1.0 spec, used by LTI1.1, defines a more restricted/specific encoding for strings during signature
   * generation. Essentially, to ensure the same parameters produce the same signatures, it forces particular choices
   * for how some characters (such as ' ') are encoded, whereas normal URL encoding is ambiguous.
   */
  def oauthURLencode(s:String) =
    URLEncoder.encode(s, Charset.forName("UTF-8"))
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")
      .replace("%2B", "%20")

  def signature(method:String, scheme:String, authority:String, port:String, path:String, parameters:Seq[(String, String)], clientSecret:String, tokenSecret:String = ""):String = {
    val mac = Mac.getInstance("HmacSHA1")

    val secret = oauthURLencode(clientSecret) + "&" + oauthURLencode(tokenSecret)

    // Encode the parameters
    val encodedPairs:Seq[(String, String)] = (for (key, value) <- parameters if key != "oauth_signature" yield (oauthURLencode(key), oauthURLencode(value)))
    val sortedPairs = encodedPairs.sorted
    val encodedParameters = (for (k, v) <- sortedPairs yield k + '=' + v).mkString("&")

    // Normalise the request URL
    val pathWithSlash = if path.startsWith("/") then path else "/" + path
    val normalisedUrl = if (port == "80" || port == "443") then s"$scheme://$authority/$path" else s"$scheme://$authority:$port$pathWithSlash"

    // Derive the base string
    val baseString = s"${method.toUpperCase}&${oauthURLencode(normalisedUrl)}&${oauthURLencode(encodedParameters)}"

    // Calculate the signature
    val initialised = mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA1"))
    val binarySignature = mac.doFinal(baseString.getBytes("UTF-8"))
    val encodedSignature = Base64.getEncoder.encodeToString(binarySignature)

    encodedSignature
  }



}
