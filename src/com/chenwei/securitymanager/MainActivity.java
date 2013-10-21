package com.chenwei.securitymanager;

import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chenwei.securitymanager.provider.SecurityManagerContract;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeCategory;

public class MainActivity extends Activity {

    private static String TAG = "MainActivity";
    private FrameLayout cv = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check whether this is the first time running this application
        SharedPreferences prefs = getSharedPreferences("configs", MODE_PRIVATE);
        if (prefs.getBoolean("firstRunning", true)) {
            // TODO initial privilege config data
            initPrivilegeConfigureOnce();

            SharedPreferences.Editor edits = prefs.edit();
            edits.putBoolean("firstRunning", false).commit();
        }

        // need api level 11
        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        bar.addTab(bar
                .newTab()
                .setText(R.string.by_privilege)
                .setTabListener(
                        new TabListener<ShowByPrivilegeFragment>(this,
                                "privilege", ShowByPrivilegeFragment.class)));
        bar.addTab(bar
                .newTab()
                .setText(R.string.by_app)
                .setTabListener(
                        new TabListener<CountingFragment>(this, "application",
                                CountingFragment.class)));
        // cv = (FrameLayout)findViewById(android.R.id.content);
        /* log all installed app infos */
        // this.debugPrintApplicationsInfo(this.getApplicationList(this));
        // this.debugPrintPackagesInfo(this.getPackages(this));
        // addListViewForCategories();
        //debugContentProvider();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void initPrivilegeConfigureOnce() {

    }

    private void addListViewForCategories() {
        ContentResolver resolver = this.getContentResolver();
        Cursor cursor = resolver.query(
                SecurityManagerContract.PrivilegeCategory.CONTENT_URI,
                SecurityManagerContract.PrivilegeCategory.PROJECTION_ALL, null,
                null, null);
        this.startManagingCursor(cursor);

        String[] columns = new String[] { PrivilegeCategory.CATEGORY_ID,
                PrivilegeCategory.CATEGORY_NAME };
        int[] to = new int[] { R.id.cat_id, R.id.cat_name };
        SimpleCursorAdapter cAdapter = new SimpleCursorAdapter(this,
                R.layout.category_entry, cursor, columns, to);
        ListView lv = new ListView(this);
        lv.setAdapter(cAdapter);
        cv.addView(lv);

    }

    // debug content provider
    private void debugContentProvider() {
        ContentResolver resolver = this.getContentResolver();
        debugTablePrivilegeCategory(resolver);
        
        debugTablePrivilegeDetails(resolver, 1);
        Log.d(TAG, "*************************");
        debugTablePrivilegeDetails(resolver, 2);
        Log.d(TAG, "*************************");
        debugTablePrivilegeDetails(resolver, 3);
    }
    
    private void debugTablePrivilegeDetails(ContentResolver resolver,
            int categoryId) {
        Cursor cursor = resolver.query(
                SecurityManagerContract.PrivilegeDetails.CONTENT_URI,
                        SecurityManagerContract.PrivilegeDetails.PROJECTION_FOR_SPECIFIC_CATEGORY,
                        "category_id=CAST(? AS INTEGER)",
                        new String[] { String.valueOf(categoryId) }, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                long row_id = cursor.getLong(0);
                int privilege_id = cursor.getInt(1);
                String privilege_name = cursor.getString(2);
                Log.d(TAG, String.format(
                                "Category: row_id=%d, privilege_id=%d, privilege_name=%s",
                                row_id, privilege_id, privilege_name));
            } while (cursor.moveToNext());
        } else {
            Log.d(TAG,
                    "Query privilege_details table, cursor is null or have no records in it.");
        }
    }

    private void debugTablePrivilegeCategory(ContentResolver resolver) {
        Cursor cursor = resolver.query(
                SecurityManagerContract.PrivilegeCategory.CONTENT_URI,
                SecurityManagerContract.PrivilegeCategory.PROJECTION_ALL, null,
                null, null);
        if (cursor.moveToFirst()) {
            do {
                long row_id = cursor.getLong(0);
                int cat_id = cursor.getInt(1);
                String cat_name = cursor.getString(2);
                Log.d(TAG, String.format(
                        "Category: row_id=%d, cat_id=%d, cat_name=%s",
                        row_id,
                        cat_id, cat_name));
            } while (cursor.moveToNext());
         }
        cursor.close();
    }

    /**
     * a wrapper of PackageManager's getInstalledApplications function.
     * 
     * @param con
     * @return
     */
    public List<ApplicationInfo> getApplicationList(Context con) {
        PackageManager pm = con.getPackageManager();
        List<ApplicationInfo> info = pm.getInstalledApplications(0);
        return info;
    }
    
    /**
     * a wrapper of PackageManager's getInstalledPackages function.
     * 
     * @param con
     * @return
     */
    public List<PackageInfo> getPackages(Context con) {
        PackageManager pm = con.getPackageManager();
        List<PackageInfo> packages = pm
                .getInstalledPackages(PackageManager.GET_PERMISSIONS
                        | PackageManager.GET_SIGNATURES);
        return packages;
    }

    private void debugPrintPackagesInfo(List<PackageInfo> packages) {
        if (packages != null) {
            PackageManager pm = this.getPackageManager();
            if (pm != null) {
                Iterator<PackageInfo> iterator = packages.iterator();
                while (iterator.hasNext()) {
                    PackageInfo packageinfo = iterator.next();
                    String packageName = packageinfo.packageName;
                    String packageLabel = packageinfo.applicationInfo
                            .loadLabel(pm).toString();
                    int uid = packageinfo.applicationInfo.uid;
                    Log.d(TAG, "********************");
                    Log.d(TAG, String.format("%s %s %d", packageName,
                            packageLabel, uid));
                    PermissionInfo[] pers = packageinfo.permissions;
                    if (pers != null) {
                        Log.d(TAG, String.format("Has %d permissions.",
                                pers.length));
                        for (int i = 0; i < pers.length; i++) {
                            Log.d(TAG, String.format(
                                    "PermissionInfo[%d]: %s", i,
                                    pers[i].toString()));
                        }
                    }
                    Signature[] sigs = packageinfo.signatures;
                    if (sigs != null) {
                        Log.d(TAG, String.format("Has %d signatures.",
                                sigs.length));
                        for (int i = 0; i < sigs.length; i++) {
                            // Log.d(LOG_TAG,
                            // String.format("Public Key: %s",
                            // sigs[i].getPublicKey()));
                            Log.d(TAG, String.format("Signature[%d]: %s",
                                    i, sigs[i].toCharsString()));
                        }
                    }
                    Log.d(TAG, "********************");
                }
            }
            return;
        }
        Log.d(TAG, "Parameter apps is null, doing nothing.");
    }

    public static class TabListener<T extends Fragment> implements
            ActionBar.TabListener {
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public TabListener(Activity activity, String tag, Class<T> clz,
                Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state. If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager()
                        .beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(),
                        mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            Toast.makeText(mActivity, "Reselected!", Toast.LENGTH_SHORT).show();
        }
    }

    public static class CountingFragment extends Fragment {
        int mNum;

        /**
         * Create a new instance of CountingFragment, providing "num" as an
         * argument.
         */
        static CountingFragment newInstance(int num) {
            CountingFragment f = new CountingFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its instance
         * number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.hello_world, container, false);
            View tv = v.findViewById(R.id.text);
            ((TextView) tv).setText("Fragment #" + mNum);
            return v;
        }
    }

    /**
     * 
     * 
     * @param apps
     * */
    // private void debugPrintApplicationsInfo(List<ApplicationInfo> apps) {
    // if (apps != null) {
    // PackageManager pm = this.getPackageManager();
    // if (pm != null) {
    // Iterator<ApplicationInfo> iterator = apps.iterator();
    // while (iterator.hasNext()) {
    // ApplicationInfo applicationInfo = iterator.next();
    // String packageName = applicationInfo.packageName;// 包名
    // String packageLabel = pm.getApplicationLabel(
    // applicationInfo).toString();
    // int uid = applicationInfo.uid;
    // Log.d(LOG_TAG, String.format("%s %s %d", packageName,
    // packageLabel, uid));
    // }
    // }
    // return;
    // }
    // Log.d(LOG_TAG, "Parameter apps is null, doing nothing.");
    // }

}
