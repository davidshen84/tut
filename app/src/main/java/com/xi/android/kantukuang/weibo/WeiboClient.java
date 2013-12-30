package com.xi.android.kantukuang.weibo;


import android.util.Log;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.json.JSONException;

import java.io.IOException;

public class WeiboClient {


    private static final String TAG = WeiboClient.class.getName();
    private final String mRedirectUri;
    private final Credential.AccessMethod mAccessMethod;
    private final HttpTransport mHttpTransport;
    private final JsonObjectParser mJsonObjectParser;
    private final AuthorizationCodeFlow mAuthorizationCodeFlow;
    private Credential mCredential;
    private HttpRequestFactory mRequestFactory;
    private String mAccessToken;

    @Inject
    public WeiboClient(AuthorizationCodeFlow authorizationCodeFlow,
                       Credential.AccessMethod accessMethod, HttpTransport httpTransport,
                       JsonObjectParser jsonObjectParser,
                       @Named("redirect uri") String redirectUri) {
        this.mAuthorizationCodeFlow = authorizationCodeFlow;
        this.mAccessMethod = accessMethod;
        this.mHttpTransport = httpTransport;
        this.mJsonObjectParser = jsonObjectParser;
        this.mRedirectUri = redirectUri;
    }

    @Deprecated
    public String[] getMyTags() throws IOException, JSONException {

        String mUid = "2860471240";
        HttpResponse response = mRequestFactory.buildGetRequest(new WeiboTagsUrl(mUid)).execute();

        String[] strings = {response.parseAsString()};
        response.disconnect();

        return strings;
    }

    private WeiboTimeline getWeiboTimelineByUrl(
            AbstractWeiboTimelineUrl url) throws WeiboTimelineException {
        WeiboTimeline weiboTimeline = null;
        HttpResponse httpResponse = null;
        HttpRequestFactory requestFactory = getRequestFactory();

        boolean hasException = false;
        try {
            HttpRequest httpRequest = requestFactory.buildGetRequest(url)
                    .setNumberOfRetries(2);
//                    .setReadTimeout(50000);
            Log.v(TAG, httpRequest.getUrl().toString());

            httpResponse = httpRequest.execute();
            Log.d(TAG, String.format("status: %d", httpResponse.getStatusCode()));

            if (!httpResponse.isSuccessStatusCode()) {
                Log.d(TAG, String.format("there's an error when getting timeline (%s): %s",
                                         httpResponse.getStatusMessage(),
                                         httpResponse.parseAsString()));
            }

            weiboTimeline = httpResponse.parseAs(WeiboTimeline.class);
        } catch (HttpResponseException e) {
//            e.printStackTrace();
            Log.d(TAG, "there's an error in response when getting timeline");
            hasException = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "there's an error when getting timeline.");
            hasException = true;
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (hasException) throw new WeiboTimelineException();

        if (weiboTimeline == null || weiboTimeline.statuses.size() == 0)
            Log.v(TAG, "json parse result is empty or no status is returned");

        return weiboTimeline;
    }

    public WeiboTimeline getPublicTimeline(String sinceId) throws WeiboTimelineException {
        AbstractWeiboTimelineUrl url = new PublicTimelineUrl();
        if (sinceId != null)
            url.SinceId = sinceId;

        return getWeiboTimelineByUrl(url);
    }

    public WeiboTimeline getHomeTimeline(String sinceId) throws WeiboTimelineException {
        AbstractWeiboTimelineUrl url = new HomeTimelineUrl();
        if (sinceId != null)
            url.SinceId = sinceId;

        return getWeiboTimelineByUrl(url);
    }

    public String getAuthorizeUrl() {
        return mAuthorizationCodeFlow.newAuthorizationUrl()
                .setRedirectUri(mRedirectUri)
                .set("display", "mobile")
                .toString();
    }

    public void requestAccessToken(String code) {
        try {
            mAccessToken = mAuthorizationCodeFlow
                    .newTokenRequest(code)
                    .setRedirectUri(mRedirectUri)
                    .execute()
                    .getAccessToken();
        } catch (IOException e) {
            e.printStackTrace();
            mAccessToken = null;
        }

        Log.d(TAG, mAccessToken);
    }

    private HttpRequestFactory getRequestFactory() {
        if (mRequestFactory == null) {
            // build credential
            mCredential = new Credential(mAccessMethod).setAccessToken(mAccessToken);
            Log.d(TAG, String.format("created credential with token: %s", mAccessToken));

            // create request factory
            mRequestFactory = mHttpTransport.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest httpRequest) throws IOException {
                    mCredential.initialize(httpRequest);
                    httpRequest.setParser(mJsonObjectParser);
                }

            });
            Log.v(TAG, "created new request factory");
        }

        return mRequestFactory;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public void setAccessToken(String accessToken) {
        this.mAccessToken = accessToken;
    }

    public boolean IsAuthenticated() {
        return mAccessToken != null && !mAccessToken.equalsIgnoreCase("");
    }

}