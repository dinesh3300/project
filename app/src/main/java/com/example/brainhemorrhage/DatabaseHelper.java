package com.example.brainhemorrhage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BrainHemorrhage.db";
    private static final int DATABASE_VERSION = 2;

    // Table Scans
    private static final String TABLE_SCANS = "scans";
    private static final String KEY_ID = "id";
    private static final String KEY_PATIENT_ID = "patient_id";
    private static final String KEY_PATIENT_NAME = "patient_name";
    private static final String KEY_PATIENT_AGE = "patient_age";
    private static final String KEY_PATIENT_GENDER = "patient_gender";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_RESULT = "result";
    private static final String KEY_RISK_LEVEL = "risk_level";
    private static final String KEY_IMAGE_URI = "image_uri";
    private static final String KEY_INTRAVENTRICULAR = "intraventricular";
    private static final String KEY_INTRAPARENCHYMAL = "intraparenchymal";
    private static final String KEY_SUBARACHNOID = "subarachnoid";
    private static final String KEY_EPIDURAL = "epidural";
    private static final String KEY_SUBDURAL = "subdural";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SCANS_TABLE = "CREATE TABLE " + TABLE_SCANS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PATIENT_ID + " TEXT,"
                + KEY_PATIENT_NAME + " TEXT,"
                + KEY_PATIENT_AGE + " TEXT,"
                + KEY_PATIENT_GENDER + " TEXT,"
                + KEY_DATE + " TEXT,"
                + KEY_TIME + " TEXT,"
                + KEY_RESULT + " TEXT,"
                + KEY_RISK_LEVEL + " TEXT,"
                + KEY_IMAGE_URI + " TEXT,"
                + KEY_INTRAVENTRICULAR + " REAL,"
                + KEY_INTRAPARENCHYMAL + " REAL,"
                + KEY_SUBARACHNOID + " REAL,"
                + KEY_EPIDURAL + " REAL,"
                + KEY_SUBDURAL + " REAL"
                + ")";
        db.execSQL(CREATE_SCANS_TABLE);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_scans_patient_id ON " + TABLE_SCANS + " (" + KEY_PATIENT_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCANS);
        onCreate(db);
    }

    // Add new scan result
    public long insertScan(String patientId, String name, String age, String gender, 
                          String date, String time, String result, String riskLevel, String imageUri,
                          float intraventricular, float intraparenchymal, float subarachnoid, 
                          float epidural, float subdural) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATIENT_ID, patientId);
        values.put(KEY_PATIENT_NAME, name);
        values.put(KEY_PATIENT_AGE, age);
        values.put(KEY_PATIENT_GENDER, gender);
        values.put(KEY_DATE, date);
        values.put(KEY_TIME, time);
        values.put(KEY_RESULT, result);
        values.put(KEY_RISK_LEVEL, riskLevel);
        values.put(KEY_IMAGE_URI, imageUri);
        values.put(KEY_INTRAVENTRICULAR, intraventricular);
        values.put(KEY_INTRAPARENCHYMAL, intraparenchymal);
        values.put(KEY_SUBARACHNOID, subarachnoid);
        values.put(KEY_EPIDURAL, epidural);
        values.put(KEY_SUBDURAL, subdural);

        long id = db.insert(TABLE_SCANS, null, values);
        return id;
    }

    // Fetch local patient records (grouped by unique patient ID)
    public List<ScanItem> getAllLocalPatients() {
        List<ScanItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Fetch only the latest scan for each unique patient_id to prevent SQL grouping quirks
        String selectQuery = "SELECT " + KEY_PATIENT_ID + ", " + KEY_PATIENT_NAME + ", " 
                + KEY_DATE + ", " + KEY_RESULT + ", " + KEY_PATIENT_AGE + ", " + KEY_PATIENT_GENDER + ", " + KEY_IMAGE_URI + ", " + KEY_ID
                + " FROM " + TABLE_SCANS 
                + " WHERE " + KEY_ID + " IN (SELECT MAX(" + KEY_ID + ") FROM " + TABLE_SCANS + " GROUP BY " + KEY_PATIENT_ID + ")"
                + " ORDER BY " + KEY_ID + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                String patientId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PATIENT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PATIENT_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE));
                String result = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESULT));
                String age = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PATIENT_AGE));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PATIENT_GENDER));
                String imageUri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMAGE_URI));
                
                ScanItem item = new ScanItem(id, patientId, name, result, date, imageUri);
                item.setAge(age);
                item.setGender(gender);
                item.setDbPatientId(patientId);
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get all scans for a single patient
    public List<ScanTimelineItem> getScansForPatient(String patientId) {
        List<ScanTimelineItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selectQuery = "SELECT * FROM " + TABLE_SCANS 
                + " WHERE " + KEY_PATIENT_ID + " = ?" 
                + " ORDER BY " + KEY_ID + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{patientId});
        int count = cursor.getCount();
        int idx = 0;
        
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TIME));
                String result = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESULT));
                String risk = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RISK_LEVEL));
                String uri = cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMAGE_URI));
                
                boolean isLast = (idx == count - 1);
                ScanTimelineItem item = new ScanTimelineItem(String.valueOf(id), date, time, result, risk, isLast);
                item.setImageUri(uri);
                list.add(item);
                idx++;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Get specific scan by ID
    public Cursor getScanById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SCANS + " WHERE " + KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_SCANS);
    }
}
