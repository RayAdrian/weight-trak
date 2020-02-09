package ph.edu.upd.eee.weighttrack;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth fireAuth;
    private EditText inputEmail, inputPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.d("LoginActivity","created");
        setContentView(R.layout.activity_login);

        fireAuth = FirebaseAuth.getInstance();
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        Button btnLogIn = findViewById(R.id.btn_login);
        Button btnRegister = findViewById(R.id.btn_register);

        btnLogIn.setOnClickListener(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (fireAuth.getCurrentUser() != null) {
            Log.d("LoginActivity","currently signed in");
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        else {
            Log.d("LoginActivity", "none currently signed in");
        }

    }

    public void signIn() {
        if ( !isNotEmpty(inputEmail) | !isNotEmpty(inputPassword) ) {
            return;
        }
        fireAuth.signInWithEmailAndPassword(inputEmail.getText().toString(),inputPassword.getText().toString()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d("LoginActivity","signIn: Log in successful" );
                    Toast.makeText(LoginActivity.this, "Log in successful.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    Toast.makeText(LoginActivity.this, "Log in failed.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean isNotEmpty( EditText input ) {
        boolean valid = true;

        String string = input.getText().toString();
        if (TextUtils.isEmpty(string)) {
            input.setError("Required.");
            valid = false;
        } else {
            input.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_login) {
            signIn();
        } else if (i == R.id.btn_register) {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        }
    }

}
