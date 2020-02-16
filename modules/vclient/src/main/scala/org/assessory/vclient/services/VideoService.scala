package org.assessory.vclient.services

import com.assessory.api.video.{Kaltura, UnrecognisedVideoUrl, VideoResource, YouTube}

object VideoService {

  def video(url:String):Option[VideoResource] = {
    isYouTube(url)
      .orElse(isKaltura(url))
      .orElse({
        if (url.trim.isEmpty) None else Some(UnrecognisedVideoUrl(url))
      })
  }

  /*
   * If this is a YouTube URL, returns Some(url), else None
   */
  def isYouTube(url:String):Option[YouTube] = {
    val withWWW = "(https\\:\\/\\/www.youtube.com\\/watch\\?v=)([^#\\&\\?]*).*".r
    val youTudotBe = ".*(youtu.be\\/)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(youTudotBe.findFirstMatchIn(url).map(_.group(2))).map({ case _ => YouTube(url) })
  }

  def isKaltura(url:String):Option[Kaltura] = {
    val withWWW="(https\\:\\/\\/kaf.une.edu.au\\/media\\/[^#\\&\\?\\/]*\\/)([^#\\&\\?]*).*".r
    val embed = ".*(playerId=kaltura_player\\&entry_id=)([^#\\&\\?]*).*".r

    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(embed.findFirstMatchIn(url).map(_.group(2)))
      .map({ case _ => Kaltura(url) })
  }

  def extractKalturaId(url:String):String = {
    val withWWW="(https\\:\\/\\/kaf.une.edu.au\\/media\\/[^#\\&\\?\\/]*\\/)([^#\\&\\?]*).*".r
    val embed = ".*(playerId=kaltura_player\\&entry_id=)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(url).map(_.group(2))
      .orElse(embed.findFirstMatchIn(url).map(_.group(2)))
      .getOrElse(url)
  }


  def extractYouTubeId(string:String) = {
    val withWWW = "(https\\:\\/\\/www.youtube.com\\/watch\\?v=)([^#\\&\\?]*).*".r
    val youTudotBe = ".*(youtu.be\\/)([^#\\&\\?]*).*".r
    withWWW.findFirstMatchIn(string).map(_.group(2))
      .orElse(youTudotBe.findFirstMatchIn(string).map(_.group(2)))
      .getOrElse(string)
  }

}
