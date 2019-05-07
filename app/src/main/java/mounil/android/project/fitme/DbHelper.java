package mounil.android.project.fitme;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.Calendar;

public class DbHelper extends SQLiteOpenHelper {
    public static final String TAG = DbHelper.class.getSimpleName();
    public static final String DB_NAME = "myfitnesstracker.db";
    public static final int DB_VERSION = 1;
    public static final String USER_TABLE = "users";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String CREATE_TABLE_USERS = "CREATE TABLE " + USER_TABLE + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USERNAME + " TEXT,"
            + COLUMN_PASSWORD + " TEXT);";
    public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + USER_TABLE;
    private static final String USER_STEPS_SUMMARY = "StepsSummary";
    private static final String ID = "id";
    private static final String STEPS_COUNT = "stepscount";
    private static final String CREATION_DATE = "creationdate";//Date format is mm/dd/yyyy
    private static String CREATE_USER_STEPS_SUMMARY;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_TABLE);
        onCreate(sqLiteDatabase);
    }
    /*FUNCTION TO ADD NEW USER INTO SQLite DB*/
    public boolean addUser(String uName, String pass) {
        boolean createSuccessful = false;
        String selectQuery = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USERNAME + " = '" +
                uName + "' AND " + COLUMN_PASSWORD + " = '" + pass + "';";
        /*QUERY TO CREATE A SEPARATE DB FOR EACH USER*/
        CREATE_USER_STEPS_SUMMARY = "CREATE TABLE " + uName + "" + USER_STEPS_SUMMARY + "(" + ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                CREATION_DATE + " TEXT," + STEPS_COUNT + " INTEGER DEFAULT 0" + ")";
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                createSuccessful = false;
            } else {
                db.execSQL(CREATE_USER_STEPS_SUMMARY);
                ContentValues values = new ContentValues();
                values.put(COLUMN_USERNAME, uName);
                values.put(COLUMN_PASSWORD, pass);
                db.insert(USER_TABLE, null, values);
                createSuccessful = true;
            }
            c.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createSuccessful;
    }

    public boolean getUser(String uName, String pass) {
        boolean userPresent = false;
        String selectQuery = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USERNAME + " = '"
                + uName + "' AND " + COLUMN_PASSWORD + " = '" + pass + "';";
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.getCount() > 0) {
                userPresent = true;
            }
            c.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userPresent;
    }
    /*FUNCTION TO ADD EACH STEP ENTRY INTO THE DB*/
    public boolean createStepsEntry(String uName) {
        boolean isDateAlreadyPresent = false;
        boolean createSuccessful = false;
        int currentDateStepCounts = 0;
        Calendar mCalendar = Calendar.getInstance();
        String todayDate = String.valueOf(mCalendar.get(Calendar.MONTH)) + "/" +
                String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                String.valueOf(mCalendar.get(Calendar.YEAR));
        String selectQuery = "SELECT " + STEPS_COUNT + " FROM " + uName + "" + USER_STEPS_SUMMARY +
                " WHERE " + CREATION_DATE + " = '" + todayDate + "'";
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                do {
                    isDateAlreadyPresent = true;
                    currentDateStepCounts = c.getInt((c.getColumnIndex(STEPS_COUNT)));
                } while (c.moveToNext());
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(CREATION_DATE, todayDate);
            if (isDateAlreadyPresent) {
                values.put(STEPS_COUNT, ++currentDateStepCounts);
                int row = db.update(uName + "" + USER_STEPS_SUMMARY, values,
                        CREATION_DATE + " = '" + todayDate + "'", null);
                if (row == 1) {
                    createSuccessful = true;
                }
                db.close();
            } else {
                values.put(STEPS_COUNT, 0);
                long row = db.insert(uName + "" + USER_STEPS_SUMMARY, null,
                        values);
                if (row != -1) {
                    createSuccessful = true;
                }
                db.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return createSuccessful;
    }
    /*FUNCTION RETURNING THE TOTAL STEPS WALED FOR EACH DAY*/
    public int readStepsEntries(String uName) {
        int currentStepCounts = 0;
        Calendar mCalendar = Calendar.getInstance();
        String todayDate =
                String.valueOf(mCalendar.get(Calendar.MONTH)) + "/" +
                        String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                        String.valueOf(mCalendar.get(Calendar.YEAR));
        String selectionQuery = "SELECT " + STEPS_COUNT + " FROM " + uName + "" +
                USER_STEPS_SUMMARY + " WHERE " + CREATION_DATE + " = '" + todayDate + "'";
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(selectionQuery, null);
            if (c.moveToFirst()) {
                do {
                    currentStepCounts = c.getInt((c.getColumnIndex(STEPS_COUNT)));
                } while (c.moveToNext());
            }
            c.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentStepCounts;
    }
    /*FUNCTION RETURNING THE TOTAL STEPS WALKED BY A PARTICULAR USER TILL CURRENT DATE*/
    public int readTotalStepsEntries(String userName) {
        String totalStepEntryQuery = "SELECT SUM(" + STEPS_COUNT + ") AS TOTAL FROM " + userName + "" +
                USER_STEPS_SUMMARY + ";";
        int totalSteps = 0;
        try {
            SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
            Cursor c = sqLiteDatabase.rawQuery(totalStepEntryQuery, null);
            if (c.moveToFirst()) {
                totalSteps = c.getInt(c.getColumnIndex("TOTAL"));
            }
            c.close();
            sqLiteDatabase.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalSteps;
    }
}