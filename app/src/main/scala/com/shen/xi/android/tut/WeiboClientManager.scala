package com.shen.xi.android.tut


import com.google.inject.Inject
import com.squareup.otto.{Bus, Subscribe}

import com.shen.xi.android.tut.event.{RefreshStatusCompleteEvent, RefreshWeiboEvent}
import com.shen.xi.android.tut.weibo.WeiboClient

import java.lang.Runnable

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.collection.JavaConversions._


class WeiboClientManager @Inject()(bus: Bus, client: WeiboClient) {

  private val TAG = classOf[WeiboClientManager].getName
  private val mClient = client
  private val mBus = bus

  mBus.register(this)

  @Subscribe
  def refreshStatus(event: RefreshWeiboEvent): Unit = {
    val completeEvent = new RefreshStatusCompleteEvent()

    Future {

      val timeline = mClient.getHomeTimeline(event.sinceId)
      if (timeline != null && timeline.statuses.size > 0) {
        bufferAsJavaList(timeline.statuses filter(_.getImageUrl != null) distinct)
      } else null

    } onComplete {

      case Success(statuses) => {
        event.activity.runOnUiThread(new Runnable() {
          def run() {
            mBus.post(completeEvent.setStatus(statuses))
          }
        })
      }

      case Failure(e) => {
        e.printStackTrace()

        event.activity.runOnUiThread(new Runnable() {
          def run() {
            mBus.post(completeEvent.setStatus(null))
          }
        })
      }

    }
  }

  @throws[Throwable]
  override protected def finalize() = {
    mBus.unregister(this)

    super.finalize()
  }

}
