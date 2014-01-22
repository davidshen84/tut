package com.xi.android.kantukuang;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.viewpagerindicator.UnderlinePageIndicator;
import com.xi.android.kantukuang.weibo.WeiboClient;
import com.xi.android.kantukuang.weibo.WeiboStatus;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ImageViewActivity extends ActionBarActivity {

    public static final String ITEM_POSITION = "item position";
    public static final String STATUS_JSON = "weibo status in json";
    public static final String PREF_BLACKLIST = "blacklist set";
    private static final String TAG = ImageViewActivity.class.getName();
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    @Inject
    private WeiboClient weiboClient;
    @Inject
    private JsonFactory mJsonFactory;
    private List<WeiboStatus> mStatusList;
    private boolean mShowRepostAction = false;
    private WeiboRepostView mWeiboRepostView;
    @Inject
    private Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KanTuKuangModule.getInjector().injectMembers(this);
        setContentView(R.layout.activity_image_view);

        setUpActionBar();
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        Intent intent = getIntent();
        int currentPosition = intent.getIntExtra(ITEM_POSITION, 0);

        try {
            String stringExtra = intent.getStringExtra(STATUS_JSON);
            JsonParser jsonParser = mJsonFactory.createJsonParser(stringExtra);
            mStatusList = (List<WeiboStatus>) jsonParser.parseArray(List.class, WeiboStatus.class);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mStatusList);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(currentPosition);

        // set up pager indicator
        UnderlinePageIndicator indicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);
/*        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateRepostText(getTextByOrder(position));
            }
        });*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBus.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mBus.unregister(this);
    }

    private void setUpActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mShowRepostAction) {
            // show repost action
            getMenuInflater().inflate(R.menu.action_weibo_repost, menu);
            // set up repost listener
            MenuItem menuItem = menu.findItem(R.id.action_weibo_repost);
//            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
            mWeiboRepostView = (WeiboRepostView) MenuItemCompat.getActionView(menuItem);
        } else {
            // show normal action
            getMenuInflater().inflate(R.menu.image_view, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_weibo_add_blacklist:
                WeiboStatus status = mStatusList.get(mViewPager.getCurrentItem());
                long uid;
                if (status.repostedStatus != null)
                    uid = status.repostedStatus.uid;
                else
                    uid = status.uid;

                addUidToBlackList(uid);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void addUidToBlackList(long uid) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Set<Long> blackList = Sets.newHashSet();
        String blackListJson;

        blackListJson = sp.getString(PREF_BLACKLIST, "");
        try {
            if (!Strings.isNullOrEmpty(blackListJson)) {
                JsonParser jsonParser = mJsonFactory.createJsonParser(blackListJson);
                jsonParser.parseArray(blackList, Long.class);
            }

            blackList.add(uid);
            sp.edit()
                    .putString(PREF_BLACKLIST, mJsonFactory.toString(blackList))
                    .commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getImageUrlByOrder(int order) {
        return mStatusList.get(order).getImageUrl();
    }

    @Subscribe
    public void tapImage(final ImageViewFragment.TapImageEvent event) {
        mShowRepostAction = !mShowRepostAction;

//        updateRepostText(getTextByOrder(event.order));
        supportInvalidateOptionsMenu();
    }

    private void updateRepostText(final String text) {
        if (mShowRepostAction) {
            mWeiboRepostView.post(new Runnable() {
                @Override
                public void run() {
                    mWeiboRepostView.setText(text);
                }
            });
        }
    }

    @Subscribe
    public void repostStatus(WeiboRepostView.RepostStatusEvent event) {
        String id = mStatusList.get(mViewPager.getCurrentItem()).id;

        Log.d(TAG, String.format("%s: %s", id, event.text));
        new AsyncTask<String, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(String... strings) {
                return weiboClient.repost(strings[0], strings[1]) != null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Toast.makeText(ImageViewActivity.this, R.string.message_info_success,
                                   Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ImageViewActivity.this, R.string.message_error_fail,
                                   Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(id, event.text);

        mShowRepostAction = false;
        supportInvalidateOptionsMenu();
    }

    public String getTextByOrder(int order) {
        return mStatusList.get(order).text;
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private List<WeiboStatus> mStatusList;

        public SectionsPagerAdapter(FragmentManager fm, List<WeiboStatus> statusList) {
            super(fm);

            mStatusList = statusList;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            return ImageViewFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return mStatusList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return String.format(getString(R.string.format_info_page_order), position);
        }
    }
}
