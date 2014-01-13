package com.xi.android.kantukuang;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.inject.Inject;
import com.xi.android.kantukuang.weibo.WeiboClient;


/**
 * An action view. Should be used to embed into the action bar
 * It inflates the {@code abc_weibo_repost.xml}
 */
public class WeiboRepostView extends LinearLayout implements View.OnClickListener {
    private final EditText mText;
    @Inject
    private LayoutInflater mInflater;
    @Inject
    private WeiboClient mWeiboClient;
    private WeiboRepostView.WeiboRepostListener mRepostListener;

    public WeiboRepostView(Context context) {
        super(context);

        KanTuKuangModule.getInjector().injectMembers(this);

        mInflater.inflate(R.layout.abc_weibo_repost, this, true);

        mText = (EditText) findViewById(android.R.id.edit);
        Button mButton = (Button) findViewById(android.R.id.button1);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String text = String.valueOf(mText.getText());

        assert mRepostListener != null;
        mRepostListener.post(text);
    }

    public void setOnRepostListener(WeiboRepostListener listener) {
        mRepostListener = listener;
    }

    public interface WeiboRepostListener {
        void post(String text);
    }
}
