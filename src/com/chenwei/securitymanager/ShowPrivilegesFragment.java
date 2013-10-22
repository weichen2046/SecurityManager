
package com.chenwei.securitymanager;

import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleCursorTreeAdapter;

import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeCategory;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeDetails;

// reference urls:
// http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
// http://stackoverflow.com/questions/16791963/expandablelistview-within-fragment

public class ShowPrivilegesFragment extends Fragment {

    private static final String TAG = "ShowByPrivilegeFragment";

    private static final int CATEGORY_ID_COLUMN_INDEX = 1;
    private static final int TOKEN_GROUP = 0;
    private static final int TOKEN_CHILD = 1;

    private static final String[] CATETORIES_PROJECTION = new String[] {
            PrivilegeCategory._ID, PrivilegeCategory.CATEGORY_ID,
            PrivilegeCategory.CATEGORY_NAME
    };
    private static final String[] PRIVILEGE_DETAILS_PROJECTION = new String[] {
            PrivilegeDetails._ID, PrivilegeDetails.PRIVILEGE_NAME
    };

    private QueryHandler mQueryHandler;
    private CursorTreeAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ExpandableListView epv = new ExpandableListView(getActivity());
        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(
                getActivity(),
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[] {
                    PrivilegeCategory.CATEGORY_NAME
                },
                new int[] { android.R.id.text1 },
                new String[] {
                    PrivilegeDetails.PRIVILEGE_NAME
                },
                new int[] { android.R.id.text1 });

        epv.setAdapter(mAdapter);

        epv.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                    int childPosition, long id) {
                Log.d(TAG, String.format(
                        "Child clicked, row id = %d, groupPosition = %d, childPosition = %d", id,
                        groupPosition, childPosition));
                // TODO start activity show applications associate to the
                Intent intent = new Intent(
                        "android.intent.show_app_by_privilege");
                intent.putExtra("privilegeId", id);
                getActivity().startActivity(intent);
                return true;
            }
        });

        mQueryHandler = new QueryHandler(getActivity(), mAdapter);
        // Query for categories
        mQueryHandler.startQuery(TOKEN_GROUP, null,
                PrivilegeCategory.CONTENT_URI,
                CATETORIES_PROJECTION, null, null, null);

        return epv;
    }

    public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {

        // Note that the constructor does not take a Cursor. This is done to
        // avoid querying the
        // database on the main thread.
        public MyExpandableListAdapter(Context context, int groupLayout,
                int childLayout, String[] groupFrom, int[] groupTo,
                String[] childrenFrom, int[] childrenTo) {

            super(context, null, groupLayout, groupFrom, groupTo, childLayout,
                    childrenFrom, childrenTo);
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            // Given the group, we return a cursor for all the children within
            // that group
            mQueryHandler.startQuery(TOKEN_CHILD, groupCursor.getPosition(),
                    PrivilegeDetails.CONTENT_URI, PRIVILEGE_DETAILS_PROJECTION,
                    PrivilegeDetails.CATEGORY_ID
                            + "=CAST(? AS INTEGER)",
                    new String[] {
                        groupCursor.getString(CATEGORY_ID_COLUMN_INDEX)
                    }, null);

            return null;
        }
    }

    private static final class QueryHandler extends AsyncQueryHandler {
        private CursorTreeAdapter mAdapter;

        public QueryHandler(Context context, CursorTreeAdapter adapter) {
            super(context.getContentResolver());
            this.mAdapter = adapter;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
            case TOKEN_GROUP:
                mAdapter.setGroupCursor(cursor);
                break;

            case TOKEN_CHILD:
                int groupPosition = (Integer) cookie;
                mAdapter.setChildrenCursor(groupPosition, cursor);
                break;
            }
        }
    }

}
