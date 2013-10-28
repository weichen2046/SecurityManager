package com.chenwei.securitymanager;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.chenwei.securitymanager.provider.SecurityManagerContract.AndroidOriginPrivilege;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeConfig;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeConfigures;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeDetailsCategory;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeMap;

class ExpandableListInfoEntry {
    public List<IdNamePair> mGroups;
    public List<List<IdNamePair>> mChildren;

    public ExpandableListInfoEntry(List<IdNamePair> groups,
            List<List<IdNamePair>> children) {
        mGroups = groups;
        mChildren = children;
    }
}

class IdNamePair {
    private static final String TAG = "IdNamePair";
    public int mId;
    public String mName;
    public int mPermission;
    public String mPackageName;
    public int mPackageUid;

    public IdNamePair(int id, String name) {
        mId = id;
        mName = name;
    }

    public IdNamePair(int id, String name, int permission, String packageName,
            int uid) {
        this(id, name);
        mPermission = permission;
        mPackageName = packageName;
        mPackageUid = uid;
    }

    // TODO need refector for APPEntry also use this function.
    public int getPermissionConfigIconId() {
        int icon_id = R.drawable.privilege_question;
        switch (mPermission) {
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
            Log.w(TAG, "Invalid permission config value: " + mPermission);
            break;
        }
        return icon_id;
    }
}

public class PrivilegesActivity extends Activity implements
        LoaderManager.LoaderCallbacks<ExpandableListInfoEntry> {
    public static final String APP_PACKAGENAME_EXTRA = "packageName";
    public static final String ACTION = "android.intent.show_privilege_by_app";
    public static final String TAG = "PrivilegesActivity";

    private String mPackageName;
    private MyExpandableListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mPackageName = intent.getStringExtra(APP_PACKAGENAME_EXTRA);
        Log.d(TAG, "Passed in package name: " + mPackageName);

        ExpandableListView elv = new ExpandableListView(PrivilegesActivity.this);
        mAdapter = new MyExpandableListAdapter(PrivilegesActivity.this);
        elv.setAdapter(mAdapter);

        setContentView(elv);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, PrivilegesActivity.this);
    }

    @Override
    public Loader<ExpandableListInfoEntry> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        return new InfosLoader(PrivilegesActivity.this);
    }

    @Override
    public void onLoadFinished(Loader<ExpandableListInfoEntry> loader,
            ExpandableListInfoEntry data) {
        if (data != null) {
            mAdapter.setData(data.mGroups, data.mChildren);
        }
    }

    @Override
    public void onLoaderReset(Loader<ExpandableListInfoEntry> loader) {
        // Clear the data in the adapter.
        mAdapter.setData(null, null);
    }

    // loader
    private static class InfosLoader extends
            AsyncTaskLoader<ExpandableListInfoEntry> {
        private String mPackageName;
        private int mPackageUid;
        private ExpandableListInfoEntry mInfos;

        public InfosLoader(Context context) {
            super(context);
            PrivilegesActivity pa = (PrivilegesActivity) context;
            if (pa != null) {
                mPackageName = pa.mPackageName;
            } else {
                Log.e(TAG, "Cannot get package name from Activity.");
            }
        }

        @Override
        public ExpandableListInfoEntry loadInBackground() {
            // 1 get package's required permissions
            // 2 get origin_privilege_id from table android_origin_privilege
            // 3 get privilege_id from table privilege_map
            // 4 get privilege_name and category_id from table privilege_details
            // 5 get category_name from table privilege_category

            final Context context = getContext();
            final PackageManager pm = context.getPackageManager();
            PackageInfo pi = null;

            // 1 get package's required permissions
            try {
                pi = pm.getPackageInfo(mPackageName,
                    PackageManager.GET_PERMISSIONS);
                mPackageUid = pi.applicationInfo.uid;
            } catch (NameNotFoundException e) {
                Log.e(TAG, String.format("pacakge: %s, NameNotFoundException.",
                        mPackageName));
            }

            List<Integer> originIds = null;
            // 2 get origin_privilege_id from table android_origin_privilege
            if (pi != null) {
                String[] permissions = pi.requestedPermissions;
                if (permissions != null) {
                    originIds = getOriginPrivilegeIds(permissions);
                } else {
                    Log.e(TAG, String.format(
                            "package: %s, has no requested permissions.",
                            mPackageName));
                }
            }

            List<Integer> privilegeIds = null;
            // 3 get privilege_id from table privilege_map
            if (originIds != null) {
                privilegeIds = getPrivilegeIds(originIds);
            }

            List<PrivilegeEntry> privilegeEntries = new ArrayList<PrivilegeEntry>();
            // 4 get privilege_name and category_id from table privilege_details
            if (privilegeIds != null) {
                privilegeEntries = getPrivilegeEntries(privilegeIds);
            }

            ExpandableListInfoEntry infoToReturn = null;
            if (privilegeEntries != null) {
                // generate groups and children arrays
                infoToReturn = generateExpandableListInfoEntry(privilegeEntries);
            }

            return infoToReturn;
        }

        @Override
        protected void onStartLoading() {
            if (mInfos != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(mInfos);
            }

            if (takeContentChanged() || mInfos == null) {
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
        public void onCanceled(ExpandableListInfoEntry infos) {
            super.onCanceled(infos);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(infos);
        }

        @Override
        public void deliverResult(ExpandableListInfoEntry infos) {
            if (isReset()) {
                // An async query came in while the loader is stopped. We
                // don't need the result.
                if (infos != null) {
                    onReleaseResources(infos);
                }
            }
            ExpandableListInfoEntry oldInfos = infos;
            mInfos = infos;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(infos);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldInfos != null) {
                onReleaseResources(oldInfos);
            }
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (mInfos != null) {
                onReleaseResources(mInfos);
                mInfos = null;
            }
        }

        protected void onReleaseResources(ExpandableListInfoEntry infos) {
            // For a simple List<> there is nothing to do. For something
            // like a Cursor, we would close it here.
        }

        private List<Integer> getOriginPrivilegeIds(String[] originPermissions) {

            if (originPermissions == null || originPermissions.length == 0) {
                Log.w(TAG, "origin permissions has no element, return null.");
                return null;
            }

            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver
                    .query(AndroidOriginPrivilege.CONTENT_URI,
                    AndroidOriginPrivilege.PROJECTION_FOR_ALL, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "there has no data in from uri: "
                        + AndroidOriginPrivilege.CONTENT_URI.toString());
                return null;
            }

            List<Integer> privilegeIds = new ArrayList<Integer>();
            int nameColumnIndex = cursor
                    .getColumnIndex(AndroidOriginPrivilege.ORIG_PRIVILEGE_NAME);
            int idColumnIndex = cursor
                    .getColumnIndex(AndroidOriginPrivilege.ORIG_PRIVILEGE_ID);

            do {
                String originPrivilegeName = cursor.getString(nameColumnIndex);
                for (int i = 0; i < originPermissions.length; i++) {
                    if (originPermissions[i].equals(originPrivilegeName)) {
                        privilegeIds.add(cursor.getInt(idColumnIndex));
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();

            return privilegeIds;
        }

        private List<Integer> getPrivilegeIds(List<Integer> originIds) {

            if (originIds == null || originIds.size() == 0) {
                Log.w(TAG, "origin privilege ids has no element, return null.");
                return null;
            }

            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver.query(PrivilegeMap.CONTENT_URI,
                    PrivilegeMap.PROJECTION_FOR_ALL, null,
                            null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "there has no data in from uri: "
                        + PrivilegeMap.CONTENT_URI.toString());
                return null;
            }

            List<Integer> privilegeIds = new ArrayList<Integer>();
            int idColumnIndex = cursor
                    .getColumnIndex(PrivilegeMap.PRIVILEGE_ID);
            int originIdColumnIndex = cursor
                    .getColumnIndex(PrivilegeMap.ORIG_PRIVILEGE_ID);

            int privilegeId;
            do {
                int originId = cursor.getInt(originIdColumnIndex);
                if (isInList(originIds, originId)) {
                    privilegeId = cursor.getInt(idColumnIndex);
                    // privilege id only add once
                    if (!(isInList(privilegeIds, privilegeId))) {
                        privilegeIds.add(privilegeId);
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();

            return privilegeIds;
        }

        private List<PrivilegeEntry> getPrivilegeEntries(
                List<Integer> privilegeIds) {
            if (privilegeIds == null || privilegeIds.size() == 0) {
                Log.d(TAG, "privilege ids has no element, return null.");
            }

            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver.query(
                    PrivilegeDetailsCategory.CONTENT_URI,
                    PrivilegeDetailsCategory.PROJECTION_FOR_ALL, null, null,
                    null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "there has no data in from uri: "
                        + PrivilegeDetailsCategory.CONTENT_URI.toString());
                return null;
            }

            List<PrivilegeEntry> entries = new ArrayList<PrivilegeEntry>();
            int idColumnIndex = cursor
                    .getColumnIndex(PrivilegeDetailsCategory.PRIVILEGE_ID);
            int nameColumnIndex = cursor
                    .getColumnIndex(PrivilegeDetailsCategory.PRIVILEGE_NAME);
            int catIdColumnIndex = cursor
                    .getColumnIndex(PrivilegeDetailsCategory.CATEGORY_ID);
            int catNameColumnIndex = cursor
                    .getColumnIndex(PrivilegeDetailsCategory.CATEGORY_NAME);

            do {
                int id = cursor.getInt(idColumnIndex);
                if (isInList(privilegeIds, id)) {
                    // get privilege name
                    String name = cursor.getString(nameColumnIndex);
                    // get category id
                    int catId = cursor.getInt(catIdColumnIndex);
                    // get category name
                    String catName = cursor.getString(catNameColumnIndex);
                    entries.add(new PrivilegeEntry(id, name, catId, catName));
                }
            } while (cursor.moveToNext());
            cursor.close();

            return entries;
        }

        private ExpandableListInfoEntry generateExpandableListInfoEntry(
                List<PrivilegeEntry> privilegeEntries) {
            if (privilegeEntries == null || privilegeEntries.size() == 0) {
                Log.w(TAG, "no privilege entry element, return null.");
            }

            // ExpandableListInfoEntry entry = new ExpandableListInfoEntry();
            // groups
            List<IdNamePair> groups = new ArrayList<IdNamePair>();
            // children
            List<List<IdNamePair>> children = new ArrayList<List<IdNamePair>>();
            List<IdNamePair> childrenInGroup;

            int groupIndex;
            PrivilegeEntry entry;
            for (int i = 0; i < privilegeEntries.size(); i++) {
                entry = privilegeEntries.get(i);
                groupIndex = findIndexInList(groups, entry.mCategoryId);
                if (groupIndex == -1) {
                    groups.add(new IdNamePair(entry.mCategoryId,
                            entry.mCategoryName));

                    // because groups is a ArrayList
                    groupIndex = groups.size() - 1;
                }

                childrenInGroup = (groupIndex >= children.size()) ? null
                        : children.get(groupIndex);
                if (childrenInGroup == null) {
                    childrenInGroup = new ArrayList<IdNamePair>();
                    children.add(groupIndex, childrenInGroup);
                }
                childrenInGroup.add(new IdNamePair(entry.mId, entry.mName,
                        getPermissionConfiguration(mPackageName, entry.mId),
                        mPackageName, mPackageUid));
            }

            if (groups.size() != 0 && groups.size() == children.size()) {
                return new ExpandableListInfoEntry(groups, children);
            }

            return null;
        }

        private boolean isInList(List<Integer> list, int value) {
            if (list != null) {
                for (Integer i : list) {
                    if (i.intValue() == value) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isInIdNamePairList(List<IdNamePair> list, int value) {
            if (list != null) {
                for (IdNamePair pair : list) {
                    if (pair.mId == value) {
                        return true;
                    }
                }
            }
            return false;
        }

        private int findIndexInList(List<IdNamePair> list, int value) {
            IdNamePair temp;
            if (list != null && list.size() != 0) {
                for (int i = 0; i < list.size(); i++) {
                    temp = list.get(i);
                    if (temp.mId == value) {
                        return i;
                    }
                }
            }
            return -1;
        }

        // TODO need refactor, for this function also used by
        // ShowAppByPrivilegeActivity
        private int getPermissionConfiguration(String packageName,
                int privilegeId) {
            int permission = PrivilegeConfigures.NOT_CONFIG; // default case

            ContentResolver resolver = getContext().getContentResolver();
            String where = PrivilegeConfig.PACKAGE_NAME + "=? AND "
                    + PrivilegeConfig.PRIVILEGE_ID + "=CAST(? AS INTEGER)";
            Cursor cursor = resolver.query(PrivilegeConfig.CONTENT_URI,
                    PrivilegeConfig.PROJECTION_FOR_CONFIGURE, where,
                    new String[] { packageName, String.valueOf(privilegeId) },
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

        private static class PrivilegeEntry {
            public int mId;
            public String mName;
            public int mCategoryId;
            public String mCategoryName;

            public PrivilegeEntry(int id, String name, int catId, String catName) {
                mId = id;
                mName = name;
                mCategoryId = catId;
                mCategoryName = catName;
            }
        }
    }

    // adapter
    private static class MyExpandableListAdapter extends
            BaseExpandableListAdapter {

        private List<IdNamePair> mGroups;
        private List<List<IdNamePair>> mChildren;
        private final LayoutInflater mInflater;
        private final Context mContext;

        public MyExpandableListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<IdNamePair> groups,
                List<List<IdNamePair>> children) {
            mGroups = groups;
            mChildren = children;
            this.notifyDataSetChanged();
        }

        @Override
        public int getGroupCount() {
            if (mGroups != null) {
                return mGroups.size();
            }
            return 0;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            if (mChildren != null) {
                return mChildren.get(groupPosition).size();
            }
            return 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            if (mGroups != null) {
                return mGroups.get(groupPosition).mName;
            }
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            if (mGroups != null && mChildren != null) {
                return mChildren.get(groupPosition).get(childPosition);
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(
                        android.R.layout.simple_list_item_1,
                        parent, false);
                holder = new ViewHolder();
                holder.mText = ((TextView) convertView
                        .findViewById(android.R.id.text1));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mText.setText(getGroup(groupPosition).toString());
            holder.mText.setPadding(60, 0, 0, 0);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            ViewHolder holder;
            IdNamePair child;
            if (convertView == null) {
                convertView = mInflater.inflate(
                        R.layout.list_item_for_privilege_by_app,
                        parent, false);
                holder = new ViewHolder();
                holder.mText = (TextView) convertView
                        .findViewById(R.id.privilege_name);
                holder.mIcon = (ImageButton) convertView
                        .findViewById(R.id.config_icon);
                holder.mIcon.setOnClickListener(mIconButtonClickListener);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            child = (IdNamePair) getChild(groupPosition, childPosition);
            holder.mText.setText(child.mName);
            holder.mIcon.setBackgroundResource(child
                    .getPermissionConfigIconId());
            holder.mIcon.setTag(child);
            return convertView;
        }

        private OnClickListener mIconButtonClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                // show alert dialog, allow user to config permission.
                final IdNamePair child = (IdNamePair) v.getTag();
                final ImageButton iconButton = (ImageButton) v;
                final AlertDialog dialog = new AlertDialog.Builder(
                        MyExpandableListAdapter.this.mContext)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle(R.string.permission_config)
                        .setSingleChoiceItems(R.array.permission_choices,
                                child.mPermission, null)
                        .setPositiveButton(R.string.alert_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                            int whichButton) {
                                        ListView lw = ((AlertDialog) dialog)
                                                .getListView();
                                        int pos = lw.getCheckedItemPosition();
                                        Log.d(TAG,
                                                String.format(
                                                        "Package name: %s, UID: %d, Privilege id: %d, Selected permission: %d.",
                                                        child.mPackageName,
                                                        child.mPackageUid,
                                                        child.mId, pos));
                                        if (pos != child.mPermission) {
                                            insertOrUpdatePermission(child, pos);
                                            // reload the list to update the
                                            // icon
                                            iconButton.setImageResource(child
                                                    .getPermissionConfigIconId());
                                        }
                                    }
                                })
                        .setNegativeButton(R.string.alert_dialog_cancel, null)
                        .create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            }

        };

        // TODO need refactor for ShowAppByPrivilegeActivity use this function
        // also
        public void insertOrUpdatePermission(IdNamePair child, int permission) {
            ContentResolver resolver = mContext.getContentResolver();
            if (child.mPermission == PrivilegeConfigures.NOT_CONFIG) {
                // insert
                ContentValues values = new ContentValues();
                values.put(PrivilegeConfig.PACKAGE_NAME, child.mPackageName);
                values.put(PrivilegeConfig.PACKAGE_UID, child.mPackageUid);
                values.put(PrivilegeConfig.PRIVILEGE_ID, child.mId);
                values.put(PrivilegeConfig.PERMISSION, permission);
                resolver.insert(PrivilegeConfig.CONTENT_URI, values);
            } else {
                // update
                ContentValues values = new ContentValues();
                values.put(PrivilegeConfig.PERMISSION, permission);
                String where = PrivilegeConfig.PACKAGE_NAME + "='"
                        + child.mPackageName + "' AND "
                        + PrivilegeConfig.PRIVILEGE_ID + "=" + child.mId;
                resolver.update(PrivilegeConfig.CONTENT_URI, values, where,
                        null);
            }
            child.mPermission = permission;
        }

        static class ViewHolder {
            public TextView mText;
            public ImageButton mIcon;
        }
    }
}
