package com.chenwei.securitymanager;

import java.util.Iterator;
import java.util.List;

import com.chenwei.securitymanager.provider.SecurityManagerContract;
import com.chenwei.securitymanager.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* log all installed app infos */
        // this.debugPrintApplicationsInfo(this.getApplicationList(this));
        // this.debugPrintPackagesInfo(this.getPackages(this));

        debugContentProvider();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
