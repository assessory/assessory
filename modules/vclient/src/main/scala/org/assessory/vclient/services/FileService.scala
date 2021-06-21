package org.assessory.vclient.services

import com.assessory.api.video.{SmallFile, SmallFileDetails}
import com.assessory.clientpickle.Pickles
import com.assessory.clientpickle.Pickles._
import com.assessory.api.appbase.Course
import com.wbillingsley.handy.{Id, Latch}
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global


/**
  *
  */
object FileService {

  val cache = mutable.Map.empty[String, Latch[SmallFileDetails]]

  val detailsCache = mutable.Map.empty[String, Latch[SmallFileDetails]]

  def uploadFile(courseId:Id[Course, String], file:org.scalajs.dom.raw.File, onUpdateProgress: (Double, Double) => Unit):Future[SmallFileDetails] = {
    val xhr = new dom.XMLHttpRequest

    val detailsP:Promise[SmallFileDetails] = Promise.apply()

    xhr.upload.onprogress = (e:dom.ProgressEvent) => {
      println((e.loaded, e.total))
      onUpdateProgress(e.loaded, e.total)
    }

    xhr.onreadystatechange = (e:dom.Event) => {
      println("DONE!!!!")
      if (xhr.readyState == dom.XMLHttpRequest.DONE) {
          if (xhr.status == 200) {
            val details = Pickles.read[SmallFileDetails](xhr.responseText)
            detailsP.complete(details)
          } else {
            detailsP.failure(new RuntimeException(xhr.statusText))
          }
      }
    }

    if (xhr.upload != null) {
      xhr.open("POST", s"/api/smallfiles/${courseId.id}/upload")
      xhr.setRequestHeader("X-FILENAME", file.name)
      xhr.send(file)
    }

    detailsP.future
  }

  def loadDetailsFor(id:Id[SmallFile, String]):Future[SmallFileDetails] = {
    Ajax.get(detailsUrl(id), headers=AJAX_HEADERS).responseText.flatMap(Pickles.readF[SmallFileDetails])
  }

  def detailsUrl(smallFile:Id[SmallFile, String]):String = {
    s"/api/smallfiles/${smallFile.id}/details"
  }

  def downloadUrl(smallFile:Id[SmallFile, String]):String = {
    s"/api/smallfiles/${smallFile.id}/download"
  }

  def getDetails(id:Id[SmallFile, String]) = cache.getOrElseUpdate(id.id, Latch.lazily(loadDetailsFor(id)))

}
