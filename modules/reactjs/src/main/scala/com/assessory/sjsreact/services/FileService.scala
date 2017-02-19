package com.assessory.sjsreact.services

import com.assessory.api.client.WithPerms
import com.assessory.api.video.{SmallFile, SmallFileDetails}
import com.assessory.sjsreact.{WebApp, Latched}
import com.wbillingsley.handy.Id
import com.wbillingsley.handy.appbase.{Course, Group}
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow


/**
  *
  */
object FileService {

  val cache = mutable.Map.empty[String, Latched[SmallFileDetails]]

  val detailsCache = mutable.Map.empty[String, Latched[SmallFileDetails]]

  def uploadFile(courseId:Id[Course, String], file:org.scalajs.dom.raw.File, onUpdateProgress: (Long, Long) => Unit):Future[SmallFileDetails] = {
    val xhr = new dom.XMLHttpRequest

    val detailsP:Promise[SmallFileDetails] = Promise.apply()

    xhr.upload.onprogress = (e:dom.ProgressEvent) => {
      println(e.loaded, e.total)
      onUpdateProgress(e.loaded, e.total)
    }

    xhr.onreadystatechange = (e:dom.Event) => {
      println("DONE!!!!")
      if (xhr.readyState == dom.XMLHttpRequest.DONE) {
          if (xhr.status == 200) {
            val details = upickle.default.read[SmallFileDetails](xhr.responseText)
            detailsP.success(details)
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
    Ajax.get(detailsUrl(id), headers=AJAX_HEADERS).responseText.map(upickle.default.read[SmallFileDetails])
  }

  def detailsUrl(smallFile:Id[SmallFile, String]):String = {
    s"/api/smallfiles/${smallFile.id}/details"
  }

  def downloadUrl(smallFile:Id[SmallFile, String]):String = {
    s"/api/smallfiles/${smallFile.id}/download"
  }

  def getDetails(id:Id[SmallFile, String]) = cache.getOrElseUpdate(id.id, Latched.lazily(loadDetailsFor(id)))

}
