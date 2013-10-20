package com.chenwei.securitymanager.provider;

import java.io.IOException;
import java.io.InputStream;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.chenwei.securitymanager.provider.DBSchema.Tables;

/* package */class SecurityManagerDatabaseHelper extends SQLiteOpenHelper {

    // private static final String TAG = "SecurityManagerDatabaseHelper";

    /**
     * 
     */
    private static final String TB_PRIVILEGE_CATEGORY_INIT_DATA_FILE = "privilege_category_tb_data.xml";
    private static final String TB_PRIVILEGE_DETAILS_INIT_DATA_FILE = "privilege_details_tb_data.xml";
    private static final String TB_ANROID_ORIGIN_PRIVILEGE_INIT_DATA_FILE = "android_origin_privilege_tb_data.xml";
    private static final String TB_PRIVILEGE_MAP_INIT_DATA_FILE = "privilege_map_tb_data.xml";

    private static SecurityManagerDatabaseHelper sSingleton = null;

    // used to read database initial data when create database
    private final AssetManager assets;

    /**
     * Private constructor, callers except unit tests should obtain an
     * instance through {@link #getInstance(android.content.Context)} instead.
     */
    /* package */SecurityManagerDatabaseHelper(Context context) {
        super(context, DBSchema.DATABASE_NAME, null, DBSchema.DATABASE_VERSION);
        assets = context.getResources().getAssets();
    }

    /**
     * Sigleton mode
     * 
     * @param context
     * @return
     */
    public static synchronized SecurityManagerDatabaseHelper getInstance(
            Context context) {
        if (sSingleton == null) {
            sSingleton = new SecurityManagerDatabaseHelper(context);
        }
        return sSingleton;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /* create tables and initial data @{ */
        createPrivilegeCategoryTable(db);
        initPrivilegeCategoryTable(db);

        createPrivilegeDetailsTable(db);
        initPrivilegeDetailsTable(db);

        createAndroidOriginPrivilegeTable(db);
        initAndroidOriginPrivilegeTable(db);

        createPrivilegeMapTable(db);
        initPrivilegeMapTable(db);

        createPrivilegeConfigTable(db);
        /* }@ */
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
    }

    /**
     * create privilege category table
     * 
     * @param db
     */
    private void createPrivilegeCategoryTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.PRIVILEGE_CATEGORY + " ("
                + SecurityManagerContract.PrivilegeCategory._ID
                + " INTEGER PRIMARY KEY, "
                + SecurityManagerContract.PrivilegeCategory.CATEGORY_ID
                + " INTEGER NOT NULL UNIQUE, "
                + SecurityManagerContract.PrivilegeCategory.CATEGORY_NAME
                + " TEXT NOT NULL" + ");");
        // FOREIGN KEY(column_x) REFERENCES table_y(table_y_column_x)
    }

    /**
     * initial privilege category data
     * 
     * @param db
     */
    private void initPrivilegeCategoryTable(SQLiteDatabase db) {
        // read inital data from xml file
        ContentValues values = new ContentValues();
        InputStream input = null;
        try {
            input = assets.open(TB_PRIVILEGE_CATEGORY_INIT_DATA_FILE);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(input);

            NodeList categories = doc.getElementsByTagName("category");
            for (int i = 0; i < categories.getLength(); i++) {
                Element node = (Element) categories.item(i);
                int catId = Integer.valueOf(node.getAttribute("cat_id"));
                String catName = node.getAttribute("cat_name");

                values.clear();
                values.put(
                        SecurityManagerContract.PrivilegeCategory.CATEGORY_ID,
                        catId);
                values.put(SecurityManagerContract.PrivilegeCategory.CATEGORY_NAME, catName);
                // insert into database
                db.insert(Tables.PRIVILEGE_CATEGORY, null, values);
            }

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * create privilege details table
     * 
     * @param db
     */
    private void createPrivilegeDetailsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.PRIVILEGE_DETAILS + " ("
                + SecurityManagerContract.PrivilegeDetails._ID
                + " INTEGER PRIMARY KEY, "
                + SecurityManagerContract.PrivilegeDetails.PRIVILEGE_ID
                + " INTEGER NOT NULL UNIQUE, "
                + SecurityManagerContract.PrivilegeDetails.PRIVILEGE_NAME
                + " TEXT NOT NULL, "
                + SecurityManagerContract.PrivilegeDetails.CATEGORY_ID
                + " INTEGER NOT NULL, "
                + SecurityManagerContract.PrivilegeDetails.ID_IN_CATEGORY
                + " INTEGER NOT NULL" + ");");
    }

    private void initPrivilegeDetailsTable(SQLiteDatabase db) {
        // read inital data from xml file
        ContentValues values = new ContentValues();
        InputStream input = null;
        try {
            input = assets.open(TB_PRIVILEGE_DETAILS_INIT_DATA_FILE);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(input);

            NodeList categories = doc.getElementsByTagName("privilege");
            for (int i = 0; i < categories.getLength(); i++) {
                Element node = (Element) categories.item(i);
                int privilegeId = Integer.valueOf(node
                        .getAttribute("privilege_id"));
                String privilegeName = node.getAttribute("privilege_name");
                int categoryId = Integer.valueOf(node
                        .getAttribute("category_id"));
                int idInCategory = Integer.valueOf(node
                        .getAttribute("id_in_category"));

                values.clear();
                values.put(
                        SecurityManagerContract.PrivilegeDetails.PRIVILEGE_ID,
                        privilegeId);
                values.put(
                        SecurityManagerContract.PrivilegeDetails.PRIVILEGE_NAME,
                        privilegeName);
                values.put(
                        SecurityManagerContract.PrivilegeDetails.CATEGORY_ID,
                        categoryId);
                values.put(
                        SecurityManagerContract.PrivilegeDetails.ID_IN_CATEGORY,
                        idInCategory);
                // insert into database
                db.insert(Tables.PRIVILEGE_DETAILS, null, values);
            }

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * create android origin privilege table
     * 
     * @param db
     */
    private void createAndroidOriginPrivilegeTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.ANDROID_ORIGIN_PRIVILEGE + " ("
                + SecurityManagerContract.AndroidOriginPrivilege._ID
                + " INTEGER PRIMARY KEY, "
                + SecurityManagerContract.AndroidOriginPrivilege.ORIG_PRIVILEGE_ID
                + " INTEGER NOT NULL UNIQUE, "
                + SecurityManagerContract.AndroidOriginPrivilege.ORIG_PRIVILEGE_NAME
                + " TEXT NOT NULL " + ");");
    }

    private void initAndroidOriginPrivilegeTable(SQLiteDatabase db) {
        // read inital data from xml file
        ContentValues values = new ContentValues();
        InputStream input = null;
        try {
            input = assets.open(TB_ANROID_ORIGIN_PRIVILEGE_INIT_DATA_FILE);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(input);

            NodeList categories = doc.getElementsByTagName("originprivilege");
            for (int i = 0; i < categories.getLength(); i++) {
                Element node = (Element) categories.item(i);
                int originId = Integer.valueOf(node
                        .getAttribute("orig_privilege_id"));
                String originName = node.getAttribute("orig_privilege_name");
                // because our init file not include the prifix
                originName = "android.permission." + originName;

                values.clear();
                values.put(
                        SecurityManagerContract.AndroidOriginPrivilege.ORIG_PRIVILEGE_ID,
                        originId);
                values.put(
                        SecurityManagerContract.AndroidOriginPrivilege.ORIG_PRIVILEGE_NAME,
                        originName);
                // insert into database
                db.insert(Tables.ANDROID_ORIGIN_PRIVILEGE, null, values);
            }

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * create privilege map table
     * 
     * @param db
     */
    private void createPrivilegeMapTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.PRIVILEGE_MAP + " ("
                + SecurityManagerContract.PrivilegeMap._ID
                + " INTEGER PRIMARY KEY, "
                + SecurityManagerContract.PrivilegeMap.PRIVILEGE_ID
                + " INTEGER NOT NULL, "
                + SecurityManagerContract.PrivilegeMap.ORIG_PRIVILEGE_ID
                + " INTEGER NOT NULL " + ");");
    }

    private void initPrivilegeMapTable(SQLiteDatabase db) {
        // read inital data from xml file
        ContentValues values = new ContentValues();
        InputStream input = null;
        try {
            input = assets.open(TB_PRIVILEGE_MAP_INIT_DATA_FILE);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document doc = docBuilder.parse(input);

            NodeList categories = doc.getElementsByTagName("privilegemap");
            for (int i = 0; i < categories.getLength(); i++) {
                Element node = (Element) categories.item(i);
                int privilegeId = Integer.valueOf(node
                        .getAttribute("privilege_id"));
                int originId = Integer.valueOf(node
                        .getAttribute("orig_privilege_id"));

                values.clear();
                values.put(SecurityManagerContract.PrivilegeMap.PRIVILEGE_ID,
                        privilegeId);
                values.put(
                        SecurityManagerContract.PrivilegeMap.ORIG_PRIVILEGE_ID,
                        originId);
                // insert into database
                db.insert(Tables.PRIVILEGE_MAP, null, values);
            }

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * create privilege config table
     * 
     * @param db
     */
    private void createPrivilegeConfigTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.PRIVILEGE_CONFIG + " ("
                + SecurityManagerContract.PrivilegeConfig._ID
                + " INTEGER PRIMARY KEY, "
                + SecurityManagerContract.PrivilegeConfig.PACKAGE_NAME
                + " TEXT NOT NULL, "
                + SecurityManagerContract.PrivilegeConfig.PACKAGE_UID
                + " INTEGER NOT NULL, "
                + SecurityManagerContract.PrivilegeConfig.PRIVILEGE_ID
                + " INTEGER NOT NULL, "
                + SecurityManagerContract.PrivilegeConfig.PERMISSION
                + " INTEGER NOT NULL, "
                + SecurityManagerContract.PrivilegeConfig.ASSIST + " INTEGER "
                + ");");
    }

}
