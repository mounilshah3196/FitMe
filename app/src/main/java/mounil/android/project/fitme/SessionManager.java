package mounil.android.project.fitme;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    Context context;

    public SessionManager(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("myapp", Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void setLoggedIn(boolean loggedIn) {
        editor.putBoolean("loggedInMode", loggedIn);
        editor.commit();
    }

    public boolean loggedIn() {
        return preferences.getBoolean("loggedInMode", false);
    }

    public void setUserCredentials(String userName, String password) {
        editor.putString("userName", userName);
        editor.putString("password", password);
        editor.commit();
    }

    public String getUserName() {
        return preferences.getString("userName", null);
    }

    public boolean getSwitchState() {
        return preferences.getBoolean("switchState", false);
    }

    public void setSwitchState(boolean switchState) {
        editor.putBoolean("switchState", switchState);
        editor.commit();
    }
}