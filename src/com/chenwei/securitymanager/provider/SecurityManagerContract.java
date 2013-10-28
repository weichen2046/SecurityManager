package com.chenwei.securitymanager.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.chenwei.securitymanager.provider.DBSchema.Tables;

public final class SecurityManagerContract {

    // private static final String TAG = "SecurityManagerContract";

    public static final String AUTHORITY = "com.chenwei.securitymanager";

    /**
     * The content:// style URL for the top-level security manager
     * authority
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * This utility class cannot be instantiated
     */
    private SecurityManagerContract() {
    }

    public static final Uri PERMISSION_BY_PRIVILEGE_URI = Uri
            .parse("content://" + AUTHORITY + "/android_permission");
    public static final String[] PERMISSION_BY_PRIVILEGE_PROJECTION = new String[] {
            PrivilegeDetails.PRIVILEGE_ID,
            AndroidOriginPrivilege.ORIG_PRIVILEGE_NAME };

    public interface PrivilegeConfigures {
        int NOT_CONFIG = -1;
        int ALLOW = 0;
        int DENY = 1;
        int QUESTION = 2;

    }

    /**
     * Constants and helpers for the privilege_category table
     * */
    public static final class PrivilegeCategory implements BaseColumns {

        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                SecurityManagerContract.CONTENT_URI, "privilege_category");

        /**
         * The mime type of a directory of items.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd."
                + SecurityManagerContract.AUTHORITY
                + "."
                + Tables.PRIVILEGE_CATEGORY;

        /**
         * column category_id.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String CATEGORY_ID = "category_id";

        /**
         * column category_name.
         * 
         * <p>
         * Type: TEXT (NOT NULL)
         * </p>
         */
        public static final String CATEGORY_NAME = "category_name";

        /**
         * a projection of all columns in the privilege_category table.
         */
        public static final String[] PROJECTION_ALL = { _ID, CATEGORY_ID,
                CATEGORY_NAME };

        /**
         * define default sort order.
         */
        public static final String DEFAULT_SORT_ORDER = CATEGORY_ID + " ASC";

        /**
         * This utility class cannot be instantiated
         */
        private PrivilegeCategory() {
        }
    }

    /**
     * Constants and helpers for the privilege_details table
     * */
    public static final class PrivilegeDetails implements BaseColumns {
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                SecurityManagerContract.CONTENT_URI, "privilege_details");

        /**
         * The mime type of a directory of items.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd."
                + SecurityManagerContract.AUTHORITY
                + "."
                + Tables.PRIVILEGE_DETAILS;

        /**
         * column privilege_id.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String PRIVILEGE_ID = "privilege_id";

        /**
         * column category_name.
         * 
         * <p>
         * Type: TEXT (NOT NULL)
         * </p>
         */
        public static final String PRIVILEGE_NAME = "privilege_name";

        /**
         * column category_id.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String CATEGORY_ID = "category_id";

        /**
         * column id_in_category.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String ID_IN_CATEGORY = "id_in_category";

        /**
         * a projection of columns in the privilege_details table when
         * specific a category_id.
         */
        public static final String[] PROJECTION_FOR_SPECIFIC_CATEGORY = { _ID,
                PRIVILEGE_ID, PRIVILEGE_NAME, ID_IN_CATEGORY };
        public static final String[] PROJECTION_FOR_ALL = { _ID, PRIVILEGE_ID,
                PRIVILEGE_NAME, CATEGORY_ID, ID_IN_CATEGORY };

        /**
         * define default sort order.
         */
        public static final String DEFAULT_SORT_ORDER = ID_IN_CATEGORY + " ASC";

        /**
         * This utility class cannot be instantiated
         */
        private PrivilegeDetails() {
        }
    }

    /**
     * Constants and helpers for the android_origin_privilege table
     * */
    public static final class AndroidOriginPrivilege implements BaseColumns {

        public static final Uri CONTENT_URI = Uri
                .withAppendedPath(SecurityManagerContract.CONTENT_URI,
                        "android_origin_privilege");

        /**
         * column orig_privilege_id.
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String ORIG_PRIVILEGE_ID = "orig_privilege_id";

        /**
         * column orig_privilege_name.
         * 
         * <p>
         * Type: TEXT (NOT NULL)
         * </p>
         */
        public static final String ORIG_PRIVILEGE_NAME = "orig_privilege_name";

        public static final String[] PROJECTION_FOR_ALL = new String[] { _ID,
                ORIG_PRIVILEGE_ID, ORIG_PRIVILEGE_NAME };

        /**
         * This utility class cannot be instantiated
         */
        private AndroidOriginPrivilege() {
        }
    }

    /**
     * Constants and helpers for the privilege_map table
     * */
    public static final class PrivilegeMap implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                SecurityManagerContract.CONTENT_URI, "privilege_map");

        /**
         * column privilege_id.
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String PRIVILEGE_ID = "privilege_id";

        /**
         * column orig_privilege_id.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String ORIG_PRIVILEGE_ID = "orig_privilege_id";

        public static final String[] PROJECTION_FOR_ALL = new String[] { _ID,
                PRIVILEGE_ID, ORIG_PRIVILEGE_ID };
        /**
         * This utility class cannot be instantiated
         */
        private PrivilegeMap() {
        }
    }

    /**
     * Constants and helpers for the PrivilegeConfig table
     * */
    public static final class PrivilegeConfig implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                SecurityManagerContract.CONTENT_URI, "privilege_config");
        /**
         * column package_name.
         * 
         * <p>
         * Type: TEXT (NOT NULL)
         * </p>
         */
        public static final String PACKAGE_NAME = "package_name";

        /**
         * column package_uid.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String PACKAGE_UID = "package_uid";

        /**
         * column privilege_id.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String PRIVILEGE_ID = "privilege_id";

        /**
         * column permission.
         * 
         * <p>
         * Type: INTEGER (NOT NULL)
         * </p>
         */
        public static final String PERMISSION = "permission";

        /**
         * column assist.
         * 
         * <p>
         * Type: INTEGER
         * </p>
         */
        public static final String ASSIST = "assist";

        public static final String[] PROJECTION_FOR_CONFIGURE = new String[] { PERMISSION };

        public static int ALLOW = 0;
        public static int FOBIDDEN = 1;
        public static int PROMPT = 2;
        public static int ALLOW_DURRING_PIEROD = 3;
        public static int ALLOW_EXPIERING_TIMES = 4;

        /**
         * This utility class cannot be instantiated
         */
        private PrivilegeConfig() {
        }
    }

    /**
     * Helper view table privilege_details_category
     * 
     * @author chenwei
     * 
     */
    public static final class PrivilegeDetailsCategory implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(
                SecurityManagerContract.CONTENT_URI,
                "privilege_details_category");

        public static final String PRIVILEGE_ID = "privilege_id";
        public static final String PRIVILEGE_NAME = "privilege_name";
        public static final String CATEGORY_ID = "category_id";
        public static final String CATEGORY_NAME = "category_name";

        public static final String[] PROJECTION_FOR_ALL = new String[] { _ID,
                PRIVILEGE_ID, PRIVILEGE_NAME, CATEGORY_ID, CATEGORY_NAME };

    }
}
