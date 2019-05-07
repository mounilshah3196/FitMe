package mounil.android.project.fitme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SignupActivity extends AppCompatActivity implements View.OnClickListener {
    private Button signup;
    private EditText username, password;
    private TextView signIn;
    private DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        db = new DbHelper(this);
        signup = (Button) findViewById(R.id.signupActivity_signup_button);
        signIn = (TextView) findViewById(R.id.signupActivity_signin_button);
        username = (EditText) findViewById(R.id.signupActivity_username_editText);
        password = (EditText) findViewById(R.id.signupActivity_password_editText);
        signIn.setOnClickListener(this);
        signup.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signupActivity_signup_button:
                register();
                break;
            case R.id.signupActivity_signin_button:
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
                break;
            default:
        }
    }

    private void register() {
        String uName = username.getText().toString().toLowerCase();
        String pass = password.getText().toString().toLowerCase();
        if (uName.isEmpty() && pass.isEmpty()) {
            displayToast("Username / Password Field Is Empty");
        } else {
            boolean result = db.addUser(uName, pass);
            if (result) {
                displayToast("User Registrstion Successful");
                finish();
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            } else {
                displayToast("User Already Exists");
            }
        }
    }

    private void displayToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}