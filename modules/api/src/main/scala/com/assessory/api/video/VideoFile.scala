package com.assessory.api.video

import com.assessory.api.{TaskOutputBody, TaskBody, Target}
import com.wbillingsley.handy.appbase.{Question, User}
import com.wbillingsley.handy.{HasStringId, Id}

trait File extends HasStringId[File]

/**
  * A plain old file on disk
  */
case class VideoFile(
    id: Id[VideoFile, String],
    name: Option[String] = None,
    format: Option[String] = None,
    provenance: Option[String] = None,
    path: String,
    created: Long,
    updated: Long
) extends HasStringId[VideoFile] with File

case class Video(
    id: Id[Video, String],
    addedBy: Id[User, String],
    belongsTo: Target,
    files: Seq[VideoFile],
    created: Long,
    updated: Long
) extends HasStringId[Video]

case class VideoTask(

) extends TaskBody

case class VideoTaskOutput(videoId:Option[Id[Video,String]]) extends TaskOutputBody {
  def kind = "Video"
}