package com.assessory.api.video

import com.assessory.api.{TaskOutputBody, TaskBody, Target}
import com.assessory.api.appbase.{Course, User}
import com.assessory.api.question.Question
import com.wbillingsley.handy.{HasId, Id}




case class VideoTask(

) extends TaskBody


sealed trait VideoResource
case class YouTube(ytId:String) extends VideoResource
case class Kaltura(kId:String) extends VideoResource
case class UnrecognisedVideoUrl(url:String) extends VideoResource

case class VideoTaskOutput(video:Option[VideoResource]) extends TaskOutputBody {
  def kind = "Video"
}

case class MessageTask(text:String) extends TaskBody
case class MessageTaskOutput(text:String) extends TaskOutputBody {
  def kind = "Message"
}

sealed trait File

case class SmallFileDetails(
    id:Id[SmallFile, String],
    courseId:Id[Course, String],
    ownerId:Id[Course.Reg, String],
    name:String,
    size:Option[Long],
    mime:Option[String],
    created: Long,
    updated: Long
) extends HasId[Id[SmallFile, String]] with File

case class SmallFile(
    id:SmallFileId,
    details: SmallFileDetails,
    data:Array[Byte]
) extends HasId[SmallFileId] with File

case class SmallFileId(id:String) extends Id[SmallFile, String]

case class SmallFileTask() extends TaskBody
case class SmallFileTaskOutput(file:Option[Id[SmallFile, String]]) extends TaskOutputBody {
  def kind = "SmallFile"
}

case class CompositeTask(tasks:Seq[TaskBody]) extends TaskBody
case class CompositeTaskOutput(outputs:Seq[TaskOutputBody]) extends TaskOutputBody {
  def kind = "Composite"
}

