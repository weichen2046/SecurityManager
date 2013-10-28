package com.chenwei.securitymanager;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import android.widget.TextView;

class AppEntry {
    private String mPackageName;
    private String mLabelName;
    private Drawable mIcon;
    private PackageManager mPm;
    private ApplicationInfo mAppInfo;

    public AppEntry(PackageManager pm, ApplicationInfo appInfo) {
        mPm = pm;
        mAppInfo = appInfo;
    }

    public String getPackageName() {
        if (mPackageName == null) {
            mPackageName = mAppInfo.packageName;
        }
        return mPackageName;
    }

    public String getLabel() {
        if (mLabelName == null) {
            mLabelName = mAppInfo.loadLabel(mPm).toString();
        }
        return mLabelName;
    }

    public Drawable getIcon() {
        if (mIcon == null) {
            mIcon = mAppInfo.loadIcon(mPm);
        }
        return mIcon;
    }
}

public class ApplicationsListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<AppEntry>> {

    private static final String TAG = "ApplicationsListFragment";

    // This is the Adapter being used to display the list's data.
    AppListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

    private static class AppListLoader extends AsyncTaskLoader<List<AppEntry>> {
        final PackageManager mPm;
        List<AppEntry> mApps;

        public AppListLoader(Context context) {
            super(context);
            mPm = getContext().getPackageManager();
        }

        @Override
        public List<AppEntry> loadInBackground() {
            List<ApplicationInfo> apps = mPm.getInstalledApplications(0);
            if (apps == null) {
                apps = new ArrayList<ApplicationInfo>();
            }

            List<AppEntry> entries = new ArrayList<AppEntry>(apps.size());
            for (int i = 0; i < apps.size(); i++) {
                AppEntry entry = new AppEntry(mPm, apps.get(i));
                entries.add(entry);
            }

            // Sort the list.
            Collections.sort(entries, ALPHA_COMPARATOR);

            // Done!
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

        protected void onReleaseResources(List<AppEntry> apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }

        /**
         * Perform alphabetical comparison of application entry objects.
         */
        private static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
            private final Collator sCollator = Collator.getInstance();

            @Override
            public int compare(AppEntry object1, AppEntry object2) {
                return sCollator.compare(object1.getLabel(), object2.getLabel());
            }
        };

    }

    private static class AppListAdapter extends ArrayAdapter<AppEntry> {
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.list_item_for_app_by_privilege, parent,
                        false);
                holder = new ViewHolder();
                holder.mIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.mLabel = (TextView) convertView.findViewById(R.id.app_label);
                holder.mButton = (ImageButton) convertView.findViewById(R.id.config_icon);
                holder.mButton.setOnClickListener(mIconButtonClickListener);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final AppEntry item = getItem(position);
            holder.mIcon.setImageDrawable(item.getIcon());
            holder.mLabel.setText(item.getLabel());
            holder.mButton.setImageResource(R.drawable.right_arrow);

            holder.mButton.setTag(position);

            return convertView;
        }

        private OnClickListener mIconButtonClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                int pos = (Integer) v.getTag();
                AppEntry item = getItem(pos);
                Log.d(TAG, "Package name: " + item.getPackageName());

                // Start activity to show privilege
                Intent intent = new Intent(PrivilegesActivity.ACTION);
                intent.putExtra(PrivilegesActivity.APP_PACKAGENAME_EXTRA,
                        item.getPackageName());
                getContext().startActivity(intent);
            }

        };

        static class ViewHolder {
            ImageView mIcon;
            TextView mLabel;
            ImageButton mButton;
        }
    }

}
