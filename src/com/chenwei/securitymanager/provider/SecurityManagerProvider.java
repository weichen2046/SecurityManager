package com.chenwei.securitymanager.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.chenwei.securitymanager.provider.DBSchema.Tables;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeCategory;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeConfig;
import com.chenwei.securitymanager.provider.SecurityManagerContract.PrivilegeDetails;

public class SecurityManagerProvider extends ContentProvider {

    private static final String TAG = "SecurityManagerProvider";

    private SecurityManagerDatabaseHelper mOpenHelper;
    private static final UriMatcher sUriMatcher;

    /**
     * helper constants for use with the UriMatcher
     */
    private static final int PRIVILEGE_CATEGORY_MATCH = 1;
    private static final int PRIVILEGE_DETAILS_MATCH = 2;
    private static final int PRIVILEGE_DETAILS_ITEM_MATCH = 21;
    private static final int PRIVILEGE_CONFIG_MATCH = 3;
    private static final int ANDROID_PERMISIONS_MATCH = 4;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(SecurityManagerContract.AUTHORITY,
                "privilege_category",
                PRIVILEGE_CATEGORY_MATCH);
        sUriMatcher.addURI(SecurityManagerContract.AUTHORITY,
                "privilege_details",
                PRIVILEGE_DETAILS_MATCH);
        sUriMatcher.addURI(SecurityManagerContract.AUTHORITY,
                "privilege_details/#", PRIVILEGE_DETAILS_ITEM_MATCH);
        sUriMatcher.addURI(SecurityManagerContract.AUTHORITY,
                "privilege_config",
                PRIVILEGE_CONFIG_MATCH);
        // # is represent for privilege row id in table privilege_details
        // may be this need refactoring
        sUriMatcher.addURI(SecurityManagerContract.AUTHORITY,
                "android_permission/#", ANDROID_PERMISIONS_MATCH);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = SecurityManagerDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO need handle other uri query
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = null;

        switch (sUriMatcher.match(uri)) {
        case PRIVILEGE_CATEGORY_MATCH:
            qb.setTables(Tables.PRIVILEGE_CATEGORY);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = SecurityManagerContract.PrivilegeCategory.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            break;

        case PRIVILEGE_DETAILS_MATCH:
            qb.setTables(Tables.PRIVILEGE_DETAILS);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = SecurityManagerContract.PrivilegeDetails.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            break;
            
        case PRIVILEGE_DETAILS_ITEM_MATCH:
            qb.setTables(Tables.PRIVILEGE_DETAILS);
            qb.appendWhere(PrivilegeDetails._ID + "="
                    + uri.getPathSegments().get(1));
            break;

        case PRIVILEGE_CONFIG_MATCH:
            qb.setTables(Tables.PRIVILEGE_CONFIG);
            break;

        case ANDROID_PERMISIONS_MATCH:
            qb.setTables("privilege_map left join android_origin_privilege "
                    + "on privilege_map.orig_privilege_id = android_origin_privilege.orig_privilege_id");
            // TODO Need refactoring
            qb.appendWhere("privilege_map.privilege_id=(select "
                    + "privilege_details.privilege_id from privilege_details where privilege_details._id="
                    + uri.getPathSegments().get(1) + ")");
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, orderBy);

        Log.d(TAG, "Current query has row: " + c.getCount());

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case PRIVILEGE_CATEGORY_MATCH:
            return PrivilegeCategory.CONTENT_TYPE;
        // TODO: add other matchers
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String table;
        switch (sUriMatcher.match(uri)) {
            case PRIVILEGE_CONFIG_MATCH:
                table = DBSchema.Tables.PRIVILEGE_CONFIG;
                break;
            default:
                Log.e(TAG, "Insert: uri not any matchers, uir: " + uri.toString());
                return null;
        }
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(table, null, values);
        Log.d(TAG, "Insert called, table: " + table);
        return ContentUris.withAppendedId(PrivilegeConfig.CONTENT_URI, rowId);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        String table;
        switch (sUriMatcher.match(uri)) {
            case PRIVILEGE_CONFIG_MATCH:
                table = DBSchema.Tables.PRIVILEGE_CONFIG;
                break;
            default:
                Log.e(TAG, "Update: uri not any matchers, uir: " + uri.toString());
                return 0;
        }
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.d(TAG, "Update called, table: " + table);
        return db.update(table, values, selection, selectionArgs);
    }

}
