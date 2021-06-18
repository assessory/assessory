package com.assessory.api.client

import com.wbillingsley.handy.Id
import com.assessory.api.appbase.{CourseRole, Course}

case class CreateCoursePreenrolData(course:Id[Course,String], name:String, roles:Set[CourseRole], csv:String)

