
package com.chenwei.securitymanager;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.chenwei.securitymanager.provider.SecurityManagerContract;

public class ShowAppByPrivilegeActivity extends Activity {

    private static final String TAG = "ShowAppByPrivilegeActivity";
    TextView tv;
    long privilege_row_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        privilege_row_id = intent.getLongExtra("privilegeId", -1);
        Log.d(TAG, "Passed in privilege_row_id = " + privilege_row_id);

        FragmentManager fm = getFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            AppListFragment list = new AppListFragment();
            list.setPrivilegeId(privilege_row_id);
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }

    }

    public static class AppEntry {
        private final AppListLoader mLoader;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;

        public AppEntry(AppListLoader loader, PackageInfo info) {
            mLoader = loader;
            mInfo = info.applicationInfo;
            mApkFile = new File(mInfo.sourceDir);
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mLoader.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mLoader.getContext().getResources()
                    .getDrawable(android.R.drawable.sym_def_app_icon);
        }

        @Override
        public String toString() {
            return mLabel;
        }

        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context
                            .getPackageManager());
                    mLabel = label != null ? label.toString()
                            : mInfo.packageName;
                }
            }
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };

    public static class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {
        final PackageManager mPm;
        long privilegeRowId;
        List<AppEntry> mApps;
        List<String> mPermissions = new ArrayList<String>();

        public AppListLoader(Context context, long privilegeRowId) {
            super(context);

            // Retrieve the package manager for later use; note we don't
            // use 'context' directly but instead the save global application
            // context returned by getContext().
            mPm = getContext().getPackageManager();
            this.privilegeRowId = privilegeRowId;
        }

        @Override
        public List<AppEntry> loadInBackground() {
            // Retrieve all known applications.
            List<PackageInfo> packages = mPm
                    .getInstalledPackages(PackageManager.GET_PERMISSIONS);
            if (packages == null) {
                packages = new ArrayList<PackageInfo>();
            }

            final Context context = getContext();
            mPermissions = getPrivilegePermissions(context);
            // Create corresponding array of entries and load their labels.
            String packageLabel = ""; // for debug
            // int count1 = 0; // for debug
            // int count2 = 0; // for debug
            List<AppEntry> entries = new ArrayList<AppEntry>(packages.size());
            for (int i = 0; i < packages.size(); i++) {
                packageLabel = packages.get(i).applicationInfo
                        .loadLabel(mPm).toString();
                PermissionInfo[] pers = packages.get(i).permissions;
                Log.d(TAG, String.format(
                        "package name: %s has %d permissions.", packageLabel,
                        ((pers == null) ? 0 : pers.length)));
                if (pers != null) {
                    for (int j = 0; j < pers.length; j++) {
                        if (containsPermission(pers[j].name)) {
                            AppEntry entry = new AppEntry(this, packages.get(i));
                            entry.loadLabel(context);
                            entries.add(entry);
                            break;
                        }
                    }
                }
                // count1 = entries.size(); // for debug
                Log.d(TAG, "AppEntry first count: " + entries.size());
                String[] requestedPermissions = packages.get(i).requestedPermissions;
                if (requestedPermissions != null) {
                for (int j = 0; j < requestedPermissions.length; j++) {
                    if (containsPermission(requestedPermissions[j])) {
                        AppEntry entry = new AppEntry(this, packages.get(i));
                        entry.loadLabel(context);
                        entries.add(entry);
                        break;
                    }
                    }
                }
                // count2 = entries.size();
                // if (count1 != count2) {
                // Log.d(TAG, String.format(
                // "noteqaul, package: %s count1: %d, count2: %d",
                // packageLabel,
                // count1, count2));
                // }
            }

            // Sort the list.
            Collections.sort(entries, ALPHA_COMPARATOR);
            Log.d(TAG, "AppEntry count: " + entries.size());
            return entries;
        }

        @Override
        protected void onStartLoading() {
            if (mApps != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mApps);
            }

            if (takeContentChanged() || mApps == null) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        @Override
        public void onCanceled(List<AppEntry> apps) {
            super.onCanceled(apps);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(apps);
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mApps != null) {
                onReleaseResources(mApps);
                mApps = null;
            }
        }

        @Override
        public void deliverResult(List<AppEntry> apps) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (apps != null) {
                    onReleaseResources(apps);
                }
            }
            List<AppEntry> oldApps = apps;
            mApps = apps;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(apps);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        protected void onReleaseResources(List<AppEntry> apps) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }

        /**
         * 
         * @param context
         * @return
         */
        private List<String> getPrivilegePermissions(Context context) {
            List<String> permissions = new ArrayList<String>();
            // get privilege mapped permissions
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver
                    .query(ContentUris
                            .withAppendedId(
                                    SecurityManagerContract.PERMISSION_BY_PRIVILEGE_URI,
                    privilegeRowId),
                    SecurityManagerContract.PERMISSION_BY_PRIVILEGE_PROJECTION,
                            null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int privilegeId = cursor.getInt(0);
                    String permission = cursor.getString(1);
                    permissions.add(permission);
                    Log.d(TAG, String.format("privilege %d has permission %s.",
                            privilegeId, permission));
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG,
                        "Query privilege_details table, cursor is null or have no records in it.");
            }
            cursor.close();
            return permissions;
        }

        /**
         * 
         * @param permission
         * @return
         */
        private boolean containsPermission(String permission) {
            for(int i=0; i<mPermissions.size(); i++) {
                if (mPermissions.get(i).equals(permission)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class AppListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<List<AppEntry>> {
        // This is the Adapter being used to display the list's data.
        AppListAdapter mAdapter;
        long privilegeRowId;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new AppListAdapter(getActivity());
            setListAdapter(mAdapter);

            // Start out with a progress indicator.
            setListShown(false);
            // Prepare the loader. Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
            // This is called when a new Loader needs to be created. This
            // sample only has one Loader with no arguments, so it is simple.
            return new AppListLoader(getActivity(), privilegeRowId);
        }

        /**
         * 
         * @param id
         */
        public void setPrivilegeId(long id) {
            privilegeRowId = id;
        }

        @Override
        public void onLoadFinished(Loader<List<AppEntry>> loader,
                List<AppEntry> data) {
            // Set the new data in the adapter.
            mAdapter.setData(data);
            // The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<AppEntry>> loader) {
            // Clear the data in the adapter.
            mAdapter.setData(null);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            // Insert desired behavior here.
            Log.i("LoaderCustom", "Item clicked: " + id);
        }
    }

    public static class AppListAdapter extends ArrayAdapter<AppEntry> {
        private final LayoutInflater mInflater;

        public AppListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<AppEntry> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        /**
         * Populate new items in the list.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(
                        R.layout.list_item_for_app_by_privilege, parent,
                        false);
            } else {
                view = convertView;
            }

            AppEntry item = getItem(position);
            ((ImageView) view.findViewById(R.id.app_icon))
                    .setImageDrawable(item
                    .getIcon());
            ((TextView) view.findViewById(R.id.app_label)).setText(item
                    .getLabel());
            ((ImageView) view.findViewById(R.id.config_icon))
                    .setImageResource(android.R.drawable.ic_delete);

            view.findViewById(R.id.config_icon).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Log.d(TAG, "Config icon clicked.");

                }
            });

            // uncomment these line will disable the blue backgound when clicked
            // view.setOnClickListener(new OnClickListener() {
            //
            // @Override
            // public void onClick(View v) {
            // // TODO Auto-generated method stub
            //
            // }
            // });
            return view;
        }
    }
}
