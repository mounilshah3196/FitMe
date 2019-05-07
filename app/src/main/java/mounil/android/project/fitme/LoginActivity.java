package mounil.android.project.fitme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    DbHelper db;
    private Button login, signup;
    private EditText username, password;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sessionManager = new SessionManager(this);
        db = new DbHelper(this);
        if (sessionManager.loggedIn()) {
            startActivity(new Intent(LoginActivity.this, FitnessActivity.class));
            finish();
        }
        login = (Button) findViewById(R.id.loginActivity_login_button);
        signup = (Button) findViewById(R.id.loginActivity_signup_button);
        username = (EditText) findViewById(R.id.loginActivity_username_ET);
        password = (EditText) findViewById(R.id.loginActivity_password_editText);
        login.setOnClickListener(this);
        signup.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.loginActivity_login_button:
                login();
                break;
            case R.id.loginActivity_signup_button:
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                finish();
                break;
            default:
        }
    }

    private void login() {
        String uName = username.getText().toString().trim();
        String pass = password.getText().toString().trim();
        if (db.getUser(uName, pass)) {
            sessionManager.setLoggedIn(true);
            sessionManager.setUserCredentials(uName, pass);
            startActivity(new Intent(LoginActivity.this, FitnessActivity.class));
            finish();
        } else {
            displayToast("Invalid Credentials");
        }
    }

    private void displayToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}