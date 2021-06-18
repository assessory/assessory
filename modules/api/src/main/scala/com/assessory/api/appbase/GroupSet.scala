package com.assessory.api.appbase

import com.wbillingsley.handy.{Id, HasId}


case class GroupSet (

  id:GroupSetId,

  name:Option[String] = None,

  description:Option[String] = None,

  course: CourseId,

  parent: Option[GroupSetId] = None,

  created: Long = System.currentTimeMillis

) extends HasId[GroupSetId]

case class GroupSetId(id:String) extends Id[GroupSet, String]