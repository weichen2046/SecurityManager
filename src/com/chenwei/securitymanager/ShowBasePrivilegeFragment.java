package com.sprd.securitymanager;

import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;

public class ShowBasePrivilegeFragment extends Fragment {

    // http://www.androidhive.info/2013/07/android-expandable-list-view-tutorial/
    // http://stackoverflow.com/questions/16791963/expandablelistview-within-fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // lvExp
        // View v = inflater.inflate(R.layout.expandid_view, container, false);
        // ExpandableListView epv = (ExpandableListView) v
        // .findViewById(R.id.lvExp);
        // Create expandable view
        ExpandableListView epv = new ExpandableListView(getActivity());
        // Set up our adapter
        mAdapter = new MyExpandableListAdapter(
                getActivity(),
                android.R.layout.simple_expandable_list_item_1,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { Contacts.DISPLAY_NAME }, // Name for group
                                                        // layouts
                new int[] { android.R.id.text1 },
                new String[] { Phone.NUMBER }, // Number for child layouts
                new int[] { android.R.id.text1 });

        epv.setAdapter(mAdapter);

        mQueryHandler = new QueryHandler(getActivity(), mAdapter);
        // Query for people
        mQueryHandler.startQuery(TOKEN_GROUP, null, Contacts.CONTENT_URI,
                CONTACTS_PROJECTION, Contacts.HAS_PHONE_NUMBER + "=1", null,
                null);

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

            // Return a cursor that points to this contact's phone numbers
            Uri.Builder builder = Contacts.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder,
                    groupCursor.getLong(GROUP_ID_COLUMN_INDEX));
            builder.appendEncodedPath(Contacts.Data.CONTENT_DIRECTORY);
            Uri phoneNumbersUri = builder.build();

            mQueryHandler.startQuery(TOKEN_CHILD, groupCursor.getPosition(),
                    phoneNumbersUri, PHONE_NUMBER_PROJECTION, Phone.MIMETYPE
                            + "=?", new String[] { Phone.CONTENT_ITEM_TYPE },
                    null);

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

    private static final int GROUP_ID_COLUMN_INDEX = 0;
    private static final int TOKEN_GROUP = 0;
    private static final int TOKEN_CHILD = 1;

    private static final String[] CONTACTS_PROJECTION = new String[] {
            Contacts._ID, Contacts.DISPLAY_NAME };
    private static final String[] PHONE_NUMBER_PROJECTION = new String[] {
            Phone._ID, Phone.NUMBER };

    private QueryHandler mQueryHandler;
    private CursorTreeAdapter mAdapter;

}
