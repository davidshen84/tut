package com.shen.xi.android.tut;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.api.client.http.HttpRequest;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.shen.xi.android.tut.event.SectionAttachEvent;
import com.shen.xi.android.tut.event.SelectItemEvent;
import com.shen.xi.android.tut.sinablog.ArticleInfo;
import com.shen.xi.android.tut.sinablog.QingTagDriver;
import com.shen.xi.android.tut.util.MySimpleImageLoadingListener;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import static com.shen.xi.android.tut.MainActivity.ImageSource.Qing;
import static com.shen.xi.android.tut.MainActivity.ImageSource.QingPage;

public class QingItemFragment extends Fragment implements AbsListView.OnItemClickListener, OnRefreshListener {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TAG = "Qing Tag";
    private static final String TAG = QingItemFragment.class.getName();
    private static final String ARG_PARSE_PAGE = "Parse Page";
    private final List<ArticleInfo> articleInfoList = new ArrayList<ArticleInfo>();
    private final SelectItemEvent mSelectItemEvent = new SelectItemEvent();
    private final SectionAttachEvent mSectionAttachEvent = new SectionAttachEvent();
    private String mQingTag;
    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ArrayAdapter mAdapter;
    @Inject
    private QingTagDriver mQingTagDriver;
    private PullToRefreshLayout mPullToRefreshLayout;
    @Inject
    private Bus mBus;
    private int mPage = 1;
    private boolean mParsePage;
    private ContentLoadingProgressBar mProgressHint;
    private TextView mEmptyText;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QingItemFragment() {
        Injector injector = TuTModule.getInjector();
        injector.injectMembers(this);

        mSelectItemEvent.source = Qing;
    }

    public static QingItemFragment newInstance(String tag, boolean parsePage) {
        QingItemFragment fragment = new QingItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TAG, tag);
        args.putBoolean(ARG_PARSE_PAGE, parsePage);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();

        if (arguments != null) {
            mQingTag = arguments.getString(ARG_TAG);
            mParsePage = getArguments().getBoolean(ARG_PARSE_PAGE);
        }

        HttpRequest httpRequest = mQingTagDriver.buildTagRequest(mQingTag, mPage);
        asyncLoad(httpRequest);

        mSectionAttachEvent.sectionName = mQingTag;
        mBus.post(mSectionAttachEvent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qing_item, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(
                R.id.ptr_layout);
        mAdapter = new ArticleInfoArrayAdapter(getActivity(), articleInfoList);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mEmptyText = (TextView) view.findViewById(android.R.id.empty);
        mProgressHint = (ContentLoadingProgressBar) view.findViewById(android.R.id.hint);
        // set up ads
        AdView adView = (AdView) view.findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder()
                              .addTestDevice("3D3B40496EA6FF9FDA8215AEE90C0808")
                              .build());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        // set up pull to refresh widget
        ActionBarPullToRefresh
                .from(getActivity())
                .theseChildrenArePullable(mListView)
                .listener(this)
                .setup(mPullToRefreshLayout);

    }

    @Override
    public void onPause() {
        super.onPause();

        // simply hide the progress bar
        // when the activity goes to background
        mProgressHint.hide();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // do not post when loading
        if (mProgressHint.getVisibility() != View.VISIBLE) {
            mSelectItemEvent.position = position;
            mSelectItemEvent.source = mParsePage ? QingPage : Qing;
            mBus.post(mSelectItemEvent);
        }

        mProgressHint.show();
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        HttpRequest httpRequest;

        if (mQingTagDriver.isLast()) {
            return;
        } else {
            httpRequest = mQingTagDriver.buildTagRequest(mQingTag, mPage);
        }

        asyncLoad(httpRequest);
    }

    private void asyncLoad(HttpRequest httpRequest) {
        new AsyncTask<HttpRequest, String, List<ArticleInfo>>() {
            @Override
            protected List<ArticleInfo> doInBackground(HttpRequest... requests) {
                if (mQingTagDriver.load(requests[0]))
                    mPage++;

                return mQingTagDriver.hasLoaded() ? mQingTagDriver.getArticleInfoList() : null;
            }

            @Override
            protected void onPostExecute(List<ArticleInfo> articleInfoList) {
                if (articleInfoList != null && articleInfoList.size() > 0) {
                    QingItemFragment.this.articleInfoList.addAll(0, articleInfoList);
                    mAdapter.notifyDataSetChanged();
                    if (mPullToRefreshLayout.isRefreshing())
                        mPullToRefreshLayout.setRefreshComplete();
                } else {
                    if (mAdapter.getCount() == 0)
                        mEmptyText.setVisibility(View.VISIBLE);

                    Toast.makeText(getActivity(), "no more", Toast.LENGTH_SHORT).show();
                }

                mProgressHint.hide();
            }
        }.execute(httpRequest);
    }

    public List<ArticleInfo> getImageUrlList() {
        return mQingTagDriver.getArticleInfoList();
    }

    private class ArticleInfoArrayAdapter extends ArrayAdapter<ArticleInfo> {
        @Inject
        private LayoutInflater mInflater;
        @Inject
        private ImageLoader mImageLoader;
        @Inject
        @Named("low resolution")
        private DisplayImageOptions displayImageOptions;

        public ArticleInfoArrayAdapter(Context context, List<ArticleInfo> list) {
            super(context, R.layout.fragment_qing_item, list);

            TuTModule.getInjector().injectMembers(this);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MySimpleImageLoadingListener listener = null;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_image, parent, false);
            } else {
                ((ImageView) convertView).setImageBitmap(null);
            }

            if (listener == null) {
                int maxWidth = QingItemFragment.this.getView().getWidth();
                int maxHeight = getResources().getDimensionPixelSize(
                        R.dimen.item_image_height);
                listener = new MySimpleImageLoadingListener(maxWidth, maxHeight);
            }

            mImageLoader.displayImage(getItem(position).imageSrc, (ImageView) convertView,
                                      displayImageOptions, listener);

            return convertView;
        }

    }

}