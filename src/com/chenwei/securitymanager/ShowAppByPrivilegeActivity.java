
package com.chenwei.securitymanager;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.chenwei.securitymanager.provider.SecurityManagerContract;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeConfig;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeConfigures;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeDetails;

public class ShowAppByPrivilegeActivity extends Activity {

    public static final String PRIVILEGE_ROW_ID_EXTRA = "privilegeRowId";
    public static final String ACTION = "android.intent.show_app_by_privilege";
    private static final String TAG = "ShowAppByPrivilegeActivity";
    long mPrivilegeRowId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mPrivilegeRowId = intent.getLongExtra(PRIVILEGE_ROW_ID_EXTRA, -1);
        Log.d(TAG, "Passed in privilege row id = " + mPrivilegeRowId);

        FragmentManager fm = getFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            AppListFragment list = new AppListFragment();
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
        private int mPrivilegeConfig;
        private int mPrivilegeId;

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

        public int getPrivilegeConfig() {
            return mPrivilegeConfig;
        }

        public void setPrivilegeConfig(int config) {
            mPrivilegeConfig = config;
        }

        public int getPermissionConfigIconId() {
            int icon_id = R.drawable.privilege_question;
            switch (mPrivilegeConfig) {
                case PrivilegeConfigures.ALLOW:
                    icon_id = R.drawable.privilege_allow;
                    break;
                case PrivilegeConfigures.DENY:
                    icon_id = R.drawable.privilege_deny;
                    break;
                case PrivilegeConfigures.NOT_CONFIG:
                case PrivilegeConfigures.QUESTION:
                    icon_id = R.drawable.privilege_question;
                    break;
                default:
                    Log.w(TAG, "Invalid permission config value: "
                            + mPrivilegeConfig);
                    break;
            }
            return icon_id;
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
        long mPrivilegeRowId;
        List<AppEntry> mApps;
        List<String> mPermissions;

        public AppListLoader(Context context) {
            super(context);

            // Retrieve the package manager for later use; note we don't
            // use 'context' directly but instead the save global application
            // context returned by getContext().
            mPm = getContext().getPackageManager();
            ShowAppByPrivilegeActivity myActivity = (ShowAppByPrivilegeActivity) context;
            if(myActivity != null) {
                mPrivilegeRowId = myActivity.mPrivilegeRowId;
                Log.d(TAG, "Privilege row id: " + mPrivilegeRowId);
            } else {
                Log.e(TAG, "Cannot get privilege row id from Activity.");
            }
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
            mPermissions = getMappedPermissions(mPrivilegeRowId);
            final int priviligeId = getPrivilegeId(mPrivilegeRowId);
            List<AppEntry> entries = new ArrayList<AppEntry>(packages.size());
            for (int i = 0; i < packages.size(); i++) {
                Log.d(TAG, "AppEntry first count: " + entries.size());
                String[] requestedPermissions = packages.get(i).requestedPermissions;
                if (requestedPermissions != null) {
                    for (int j = 0; j < requestedPermissions.length; j++) {
                        if (mPermissions.contains(requestedPermissions[j])) {
                            AppEntry entry = new AppEntry(this, packages.get(i));
                            entry.mPrivilegeId = priviligeId;
                            entry.mPrivilegeConfig = getPermissionConfiguration(
                                    packages.get(i).packageName, priviligeId);
                            entry.loadLabel(context);
                            entries.add(entry);
                            break;
                        }
                    }
                }
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
         * get privilege id use privilege row id in database.
         * 
         * @param rowId privilege row id in database.
         * @return privilege id, this id is defined by design document.
         */
        private int getPrivilegeId(long rowId) {
            int id = 0;
            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver.query(ContentUris.withAppendedId(
                    PrivilegeDetails.CONTENT_URI, rowId),
                    PrivilegeDetails.PROJECTION_FOR_ALL, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getInt(1); // 1 for PRIVILEGE_ID in
                                       // PROJECTION_FOR_ALL
                Log.d(TAG, String.format(
                        "privilege row id: %d, privilege id: %d", rowId, id));
            } else {
                Log.d(TAG,
                        "Query privilege_details table, cursor is null or have no records in it.");
            }
            cursor.close();
            return id;
        }

        /**
         * get an application's permission's configuration against one
         * privilege.
         * 
         * @param packageName
         * @param privilegeId
         * @return permission configuration. PrivilegeConfigures defined those
         *         valaid values.
         */
        private int getPermissionConfiguration(String packageName, int privilegeId) {
            int permission = PrivilegeConfigures.NOT_CONFIG; // default case

            ContentResolver resolver = getContext().getContentResolver();
            String where = PrivilegeConfig.PACKAGE_NAME + "=? AND "
                    + PrivilegeConfig.PRIVILEGE_ID + "=CAST(? AS INTEGER)";
            Cursor cursor = resolver.query(PrivilegeConfig.CONTENT_URI,
                    PrivilegeConfig.PROJECTION_FOR_CONFIGURE, where,
                    new String[] {
                            packageName, String.valueOf(privilegeId)
                    },
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                permission = cursor.getInt(0);
                Log.d(TAG, String.format(
                        "package: %s, privilegeId: %d, permission: %d",
                        packageName, privilegeId, permission));
            } else {
                Log.d(TAG,
                        "Query privilege_config table, cursor is null or have no records in it.");
            }
            cursor.close();
            return permission;
        }

        /**
         * get mapped permissions use privilege row id.
         * 
         * @param privilegeRowId
         * @return
         */
        private List<String> getMappedPermissions(long privilegeRowId) {
            // TODO this function need refactor to use privilege id, but not row
            // id
            List<String> permissions = new ArrayList<String>();
            // get privilege mapped permissions
            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver
                    .query(ContentUris
                            .withAppendedId(
                                    SecurityManagerContract.PERMISSION_BY_PRIVILEGE_URI,
                                    mPrivilegeRowId),
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
    }

    public static class AppListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<List<AppEntry>> {
        // This is the Adapter being used to display the list's data.
        AppListAdapter mAdapter;

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
            // This is called when a new Loader needs to be created.
            return new AppListLoader(getActivity());
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

        public void insertOrUpdatePermission(AppEntry item, int permission) {
            ContentResolver resolver = getContext().getContentResolver();
            if (item.getPrivilegeConfig() == PrivilegeConfigures.NOT_CONFIG) {
                // insert
                ContentValues values = new ContentValues();
                values.put(PrivilegeConfig.PACKAGE_NAME, item.mInfo.packageName);
                values.put(PrivilegeConfig.PACKAGE_UID, item.mInfo.uid);
                values.put(PrivilegeConfig.PRIVILEGE_ID, item.mPrivilegeId);
                values.put(PrivilegeConfig.PERMISSION, permission);
                resolver.insert(PrivilegeConfig.CONTENT_URI, values);
            } else {
                // update
                ContentValues values = new ContentValues();
                values.put(PrivilegeConfig.PERMISSION, permission);
                String where = PrivilegeConfig.PACKAGE_NAME + "='" + item.mInfo.packageName
                        + "' AND " + PrivilegeConfig.PRIVILEGE_ID + "=" + item.mPrivilegeId;
                resolver.update(PrivilegeConfig.CONTENT_URI, values, where, null);
            }
            item.setPrivilegeConfig(permission);
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

            final AppEntry item = getItem(position);
            ((ImageView) view.findViewById(R.id.app_icon))
                    .setImageDrawable(item
                            .getIcon());
            ((TextView) view.findViewById(R.id.app_label)).setText(item
                    .getLabel());

            ((ImageButton) view.findViewById(R.id.config_icon))
                    .setImageResource(item.getPermissionConfigIconId());

            view.findViewById(R.id.config_icon).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ImageButton ib = (ImageButton) v;
                    final AlertDialog dialog = new AlertDialog.Builder(AppListAdapter.this
                            .getContext())
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle(R.string.permission_config)
                            .setSingleChoiceItems(R.array.permission_choices,
                                    item.getPrivilegeConfig(), null)
                            .setPositiveButton(R.string.alert_dialog_ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ListView lw = ((AlertDialog) dialog).getListView();
                                            int pos = lw.getCheckedItemPosition();
                                            Log.d(TAG,
                                                    String.format(
                                                            "Package name: %s, UID: %d, Privilege id: %d, Selected permission: %d.",
                                                            item.mInfo.packageName,
                                                            item.mInfo.uid,
                                                            item.mPrivilegeId, pos));
                                            if (pos != item.getPrivilegeConfig()) {
                                                AppListAdapter.this.insertOrUpdatePermission(item,
                                                        pos);
                                                        // update the icon
                                                        ib.setImageResource(item
                                                                .getPermissionConfigIconId());
                                            }
                                        }
                                    })
                            .setNegativeButton(R.string.alert_dialog_cancel, null).create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                }
            });

            return view;
        }
    }
}
