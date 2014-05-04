package com.shen.xi.android.tut;


import android.os.AsyncTask;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.shen.xi.android.tut.event.RefreshStatusCompleteEvent;
import com.shen.xi.android.tut.event.RefreshWeiboEvent;
import com.shen.xi.android.tut.util.Util;
import com.shen.xi.android.tut.weibo.WeiboClient;
import com.shen.xi.android.tut.weibo.WeiboStatus;
import com.shen.xi.android.tut.weibo.WeiboTimeline;
import com.shen.xi.android.tut.weibo.WeiboTimelineException;

import java.util.Collection;
import java.util.List;

public class WeiboClientManager {

    private static final String TAG = WeiboClientManager.class.getName();
    private final WeiboClient mClient;
    private final Bus mBus;

    @Inject
    public WeiboClientManager(Bus bus, WeiboClient client) {
        mBus = bus;
        mClient = client;

        mBus.register(this);
    }

    @Subscribe
    public void refreshStatus(RefreshWeiboEvent event) {
        new AsyncTask<String, Integer, List<WeiboStatus>>() {

            private final RefreshStatusCompleteEvent completeEvent = new RefreshStatusCompleteEvent();

            @Override
            protected List<WeiboStatus> doInBackground(String... strings) {
                List<WeiboStatus> statusList = null;

                try {
                    String sinceId = strings[0];
                    WeiboTimeline timeline = mClient.getHomeTimeline(sinceId);

                    if (timeline != null && timeline.statuses.size() > 0) {
                        Collection<WeiboStatus> statusCollection =
                                Collections2.filter(timeline.statuses, Util.ImageUrlPredictor);
                        statusList = Lists.newArrayList(statusCollection);
                    }
                } catch (WeiboTimelineException e) {
                    e.printStackTrace();
                }

                return statusList;
            }

            @Override
            protected void onPostExecute(List<WeiboStatus> weiboStatuses) {
                mBus.post(completeEvent.setStatus(weiboStatuses));
            }

        }.execute(event.sinceId);
    }

    @Override
    protected void finalize() throws Throwable {
        mBus.unregister(this);

        super.finalize();
    }

}