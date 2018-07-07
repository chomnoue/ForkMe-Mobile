package dlei.forkme.gui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import dlei.forkme.R;
import dlei.forkme.datastore.DatabaseHelper;
import dlei.forkme.datastore.NoDataException;
import dlei.forkme.datastore.TooMuchDataException;
import dlei.forkme.endpoints.BaseUrls;
import dlei.forkme.endpoints.GithubApi;
import dlei.forkme.gui.activities.github.TrendingRepositoriesActivity;
import dlei.forkme.helpers.NetworkAsyncCheck;
import dlei.forkme.helpers.NetworkHelper;
import dlei.forkme.model.User;
import dlei.forkme.state.AppSettings;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Retrofit tutorials:
// http://www.vogella.com/tutorials/Retrofit/article.html
// https://github.com/codepath/android_guides/wiki/Consuming-APIs-with-Retrofit
// https://www.youtube.com/watch?v=R4XU8yPzSx0
// http://square.github.io/retrofit/
/**
 * Due to restrictions in OAuth Library, need this activity to load in the user token into a
 * static string which is better than shared preferences. Also ensures no events can happen before
 * user details are retrieved from getAuthenticatedUser().
 */
public class IntermediateActivity extends AppCompatActivity {

    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intermediate);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        // Safer to have OAuth token as static variable as afaik shared preferences is just XML.
        SharedPreferences sharedPreferences = getSharedPreferences("github_prefs", 0);
        AppSettings.setOAuthToken(sharedPreferences.getString("oauth_token", null));

        // Remove OAuth token from shared preferences.
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("github_prefs", "");
        editor.apply();
        editor.commit();

        this.getAuthenticatedUser();
    }


    /**
     * Get a GitHub user who logged in via OAuthToken.
     */
    public void getAuthenticatedUser() {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                // Manipulate request to add headers.
                // Can't mutate the request but can make a new one.
                Request request = chain.request();
                Request.Builder newRequest = request.newBuilder()
                        // Add in user access token.
                        .addHeader("Authorization", "token " + AppSettings.sOAuthToken);
                // Pass on our request to execute.
                return chain.proceed(newRequest.build());
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpBuilder.build())
                .baseUrl(BaseUrls.githubApi)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GithubApi endpoint = retrofit.create(GithubApi.class);
        Call<User> call = endpoint.getAuthenticatedUser();

        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.code() == 200 && response.isSuccessful()) {
                    User u = response.body();
                    // Initalize user details.
                    AppSettings.setUserLogin(u.getLogin());
                    AppSettings.setUserName(u.getName());
                    AppSettings.setUserAvatarUrl(u.getAvatarUrl());
                    AppSettings.setUserEmail(u.getEmail());

                    // Get information from db.
                    mDbHelper = DatabaseHelper.getDbInstance(getApplicationContext());
                    try {
                        // Loads settings from persistent storage to AppSettings attributes.
                        mDbHelper.loadSettings();
                        Log.d("IntermediateActivity: ", "Data loaded from db");
                    } catch (NoDataException e) {
                        Log.d("No data: ", "No data from db");
                        if (!AppSettings.sUserLogin.equals("")) {
                            mDbHelper.insertSettings();
                        } else {
                            // Should never happen.
                            Log.wtf("IntermediateActivity: ", "AppSettings.sUserLogin has no value");
                        }
                    } catch (TooMuchDataException e) {
                        // Should not ever happen.
                        Log.wtf("IntermediateActivity: ", "Too much data from db");
                    }
                    // Up to here: AppSettings will always have most up to date data.


                    Log.d("IntermediateActivity: ", "getAuthenticatedUser: " + u.getLogin());
                    Intent intent = new Intent(getApplicationContext(), TrendingRepositoriesActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.wtf("IntermediateActivity: ", String.format(Locale.getDefault(),
                            "getAuthenticatedUser: Error: Status code: %d, successful: %s," + "headers: %s",
                            response.code(), response.isSuccessful(), response.headers())
                    );
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Failure to connect to endpoint.
                Log.i("TrendingActivity: ", "getAuthenticatedUser: Failed: " + t.getMessage());
                NetworkAsyncCheck n = NetworkHelper.checkNetworkConnection(
                        findViewById(R.id.progress_bar_spinner)
                );
                if (n != null) {
                    n.execute();
                }


            }
        });



    }

}
