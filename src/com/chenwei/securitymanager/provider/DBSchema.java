package com.chenwei.securitymanager.provider;

/* package */class DBSchema {

    static final String DATABASE_NAME = "securitymanager.db";

    /**
     * Database version.
     * <ul>
     * <li>Version 1: initial database.</li>
     * </ul>
     */
    static final int DATABASE_VERSION = 1;

    public interface Tables {
        public static final String PRIVILEGE_CATEGORY = "privilege_category";
        public static final String PRIVILEGE_DETAILS = "privilege_details";
        public static final String ANDROID_ORIGIN_PRIVILEGE = "android_origin_privilege";
        public static final String PRIVILEGE_MAP = "privilege_map";
        public static final String PRIVILEGE_CONFIG = "privilege_config";
    }

}
