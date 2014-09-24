package pl.warsjawa.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;

import pl.warsjawa.android.lab1.R;


public class LoggedActivity extends ListActivity {

    private LabService mLabService;
    private View mProgressView;
    private FetchUserProfile mUserProfileTask;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LabService.LabBinder localBinder = (LabService.LabBinder)service;
            mLabService = localBinder.getService();

            String session = getIntent().getStringExtra("session");
            mUserProfileTask = new FetchUserProfile(session);
            mUserProfileTask.execute();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLabService = null;
        }
    };

    public class FetchUserProfile extends AsyncTask<Void, Void, String[]> {

        private final String mSession;

        FetchUserProfile(String session) {
            mSession = session;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            return mLabService.getItems(mSession);
        }

        @Override
        protected void onPostExecute(final String[] values) {
            mUserProfileTask = null;
            showProgress(false);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(LoggedActivity.this,
                        android.R.layout.simple_list_item_1, android.R.id.text1, values);

                setListAdapter(adapter);
        }

        @Override
        protected void onCancelled() {
            mUserProfileTask = null;
            showProgress(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logged);

        mProgressView = (View)findViewById(R.id.progress);
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, LabService.class),
                mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mLabService != null) {
            unbindService(mServiceConnection);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Logout");
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                finish();
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
