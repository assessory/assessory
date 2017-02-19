package com.assessory.sjsreact.files

import com.assessory.api.video.{SmallFileDetails, SmallFile}
import com.assessory.sjsreact.services.FileService
import com.assessory.sjsreact.{CommonComponent, Latched}
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.Course
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}


object FileViews {


  sealed trait UploadStatus
  case object Idle extends UploadStatus
  case class ReadyToUpload(file:org.scalajs.dom.raw.File) extends UploadStatus
  case class CannotUpload(file:org.scalajs.dom.raw.File, reason:String) extends UploadStatus
  case class Uploading(file:org.scalajs.dom.raw.File, sent:Long, outOf:Long) extends UploadStatus
  case class Uploaded(d:SmallFileDetails) extends UploadStatus

  case class SmallFileUploadProps(course:Id[Course, String],
                                  initial:Option[Id[SmallFile, String]],
                                  action: Option[Id[SmallFile, String]] => Unit
                                 )

  case class SmallFileUploadState(
                                   props:SmallFileUploadProps,
                                   uploadStatus:UploadStatus,
                                   s:Latched[String]
                                 )


  class SmallFileUploadBackend($: BackendScope[SmallFileUploadProps, SmallFileUploadState]) {

    def sizeString(bytes:Long) = {
      if (bytes > 1000000000) f"${bytes/1000000000.0}%3.0f" + "GB"
      else if (bytes > 1000000) f"${bytes/1000000.0}%3.0f" + "MB"
      else if (bytes > 1000) f"${bytes/1000.0}%3.0f" + "kB"
      else s"${bytes} bytes"
    }

    def updateProgress(sent:Long, ofTotal:Long):Unit = {
      $.modState { state =>
        state.uploadStatus match {
          case Uploading(f, _, _) => state.copy(uploadStatus = Uploading(f, sent, ofTotal))
          case _ => throw new IllegalStateException("Received upload progress event when not uploading")
        }
      }.runNow()
    }

    def chooseFile(file:Option[org.scalajs.dom.raw.File]):Callback = {
      $.modState({ state =>
        file match {
          case Some(f) => state.copy(uploadStatus=ReadyToUpload(f))
          case None => state.copy(uploadStatus=Idle)
        }
      })
    }

    def complete(d:SmallFileDetails):Unit = $.modState { state =>
      state.copy(uploadStatus = Uploaded(d))
    }.runNow()

    def uploadFailed(x:Throwable) = $.modState { state =>
      state.uploadStatus match {
        case Uploading(f, _, _) => state.copy(uploadStatus = CannotUpload(f, x.getMessage))
        case _ => throw new IllegalStateException("uploadFailed called when not uploading")
      }
    }.runNow()

    def uploadFile():Callback = {
      $.modState({ state =>
        state.uploadStatus match {
          case ReadyToUpload(f) => {
            val s = state.copy(uploadStatus = Uploading(f, 0, f.size))
            FileService.uploadFile(state.props.course, f, { (s, t) => updateProgress(s, t) }).onComplete({
              case Success(details) => {
                complete(details)
                state.props.action(Some(details.id))
              }
              case Failure(x) => {
                uploadFailed(x)
              }
            })
            s
          }
          case _ => throw new IllegalStateException("UploadFile called when not ReadyToUpload")
        }
      })
    }

    def chooseFileButton:ReactElement = {
      <.input(^.`type` := "file",
        ^.onChange ==> { (evt: ReactEventI) => {
          val files = evt.target.files
          if (files.length > 0) {
            chooseFile(Some(files(0)))
          } else {
            chooseFile(None)
          }
        }
        }
      )
    }



    def render(state:SmallFileUploadState) = {
      <.div(
        <.div(
          "File: ",
          state.props.initial match {
            case None => <.span("nothing uploaded")
            case Some(id) => CommonComponent.latchR(FileService.getDetails(id)) { details:SmallFileDetails =>
              <.span(
                <.a(
                  ^.href := FileService.downloadUrl(details.id),
                  details.name,
                  ^.target := "__blank"
                ),
                details.size.map(s => s" (${sizeString(s)})")
              )
            }
          }
        ),
        state.uploadStatus match {
          case Idle => <.div(chooseFileButton)
          case ReadyToUpload(f) => <.div(
            chooseFileButton,
            <.button(^.cls := "btn btn-xs btn-default", ^.onClick --> uploadFile(), "Upload")
          )
          case Uploading(f, progress, total) => <.div(
            s"Uploading ${f.name}, ${sizeString(progress)} of ${sizeString(total)}"
          )
          case Uploaded(d) => <.div(
            "Uploaded, but not saved: ",
            <.span(d.name)
          )
          case CannotUpload(f, reason) => <.div(
            chooseFileButton,
            <.div(^.cls := "text-danger", "Cannot upload: ", reason)
          )
          case _ => <.div(
            ^.cls := "text-danger", "File chooser is in an unrenderable state. Please refresh."
          )
        }
      )
    }
  }


  val smallFileUploadWidget = ReactComponentB[SmallFileUploadProps]("smallFileUploadWidget")
    .initialState_P(props => {
      SmallFileUploadState(props, Idle, Latched.immediate(""))
    })
    .renderBackend[SmallFileUploadBackend]
    .build


}
