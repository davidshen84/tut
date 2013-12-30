package com.xi.android.kantukuang;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.xi.android.kantukuang.weibo.WeiboClient;

import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, ItemFragment.OnFragmentInteractionListener, ImageViewFragment.OnFragmentInteractionListener {

    private static final int ACTIVITY_REQUEST_CODE_BIND_WEIBO = 0x000A;
    private static final String TAG = MainActivity.class.getName();
    private final Injector mInjector;
    private WeiboClient mWeiboClient;
    private String mAccessToken;
    private boolean mShowActionBarOptions = true;

    public MainActivity() {
        mInjector = KanTuKuangModule.getInjector();
        mWeiboClient = mInjector.getInstance(WeiboClient.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // TODO remove this
        mAccessToken = mInjector.getInstance(Key.get(String.class, Names.named("access token")));
        mWeiboClient.setAccessToken(mAccessToken);


//      Fragment managing the behaviors, interactions and presentation of the navigation drawer.

        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // set up action bar title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getTitle());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        ItemFragment itemFragment = null;

        switch (position) {
            case 0:
                // Public Home
                itemFragment = ItemFragment.newInstance("public", mAccessToken);

                break;
            case 1:
                // Private Home
                itemFragment = ItemFragment.newInstance("home", mAccessToken);

                break;
            default:
                Log.d(TAG, "default not ready");

                break;
        }

        // update the main content by replacing fragments
        if (position == 0) {
            // do not put HOME into back stack
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, itemFragment)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, itemFragment)
                    .addToBackStack("drawer")
                    .commit();
        }
    }

    public void onSectionAttached(String section) {
        setTitle(section);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mShowActionBarOptions) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();

            return true;
        }

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ACTIVITY_REQUEST_CODE_BIND_WEIBO) {
            if (resultCode == RESULT_OK) {
                // get authorize code
                String code = data.getStringExtra(WebActivity.AUTHORIZE_CODE);
                Log.d(TAG, String.format("authorize code: %s", code));

                // request access token
                mWeiboClient.requestAccessToken(code);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

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
            case R.id.action_bind_weibo:
                Intent intent = new Intent(this, WebActivity.class);
                intent.putExtra(WebActivity.LANDING_URL, mWeiboClient.getAuthorizeUrl());

                startActivityForResult(intent, ACTIVITY_REQUEST_CODE_BIND_WEIBO);

                return true;
            case R.id.action_refresh:
                OnRefreshListener refreshListener = (OnRefreshListener) getSupportFragmentManager().findFragmentById(
                        R.id.container);
                if (refreshListener != null)
                    refreshListener.onRefreshStarted(null);
                else
                    Log.v(TAG, "refresh action has no listener");

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemFragmentInteraction(String url) {
        // load full image view fragment

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, ImageViewFragment.newInstance(url))
                .addToBackStack("image_view")
                .commit();
    }

    @Override
    public void onImageViewFragmentInteraction(Uri uri) {

    }

    public void showActionBarOptions(boolean showOptions) {
        mShowActionBarOptions = showOptions;
        supportInvalidateOptionsMenu();
    }

}