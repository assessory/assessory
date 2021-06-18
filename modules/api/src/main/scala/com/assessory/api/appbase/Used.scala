package com.assessory.api.appbase

import com.wbillingsley.handy.Id

case class Used[T] (target:Id[T,String], time:Long)
