package dlei.forkme.gui.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;

import com.hardikgoswami.oauthLibGithub.GithubOauth;

import java.util.ArrayList;

import dlei.forkme.AppCredentials;
import dlei.forkme.R;

public class LogInActivity extends AppCompatActivity {

    private AppCompatButton mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mLoginButton = (AppCompatButton) findViewById(R.id.loginButton);
        final ArrayList<String> scopeList = new ArrayList<String>();
        scopeList.add("user");  // Needed to follow people.
        scopeList.add("public_repo");  // Needed to star repositories.

        mLoginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i("mLoginButton: ", "clicked");
                Log.i("----- Start OAuth", " -----");
                GithubOauth
                        .Builder()
                        .withClientId(AppCredentials.clientId)
                        .withClientSecret(AppCredentials.clientSecret)
                        .withContext(getApplicationContext())
                        .packageName("dlei.forkme")
                        .nextActivity("dlei.forkme.gui.activities.IntermediateActivity")
                        .withScopeList(scopeList)
                        .debug(true)
                        .execute();
            }
        });

    }
}