package controllers

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}

/**
  * Created by dev on 14/11/16.
  */
@Singleton
class AppController extends Controller{

  def index = Action { Ok("Hello play") }

}
