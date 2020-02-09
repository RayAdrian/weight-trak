package ph.edu.upd.eee.weighttrack;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

import ph.edu.upd.eee.weighttrack.Models.Settings;
import ph.edu.upd.eee.weighttrack.Models.User;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth fireAuth;
    private DatabaseReference fireDb;

    private EditText inputRegEmail, inputRegPassword, inputRegPassword2;
    private CheckBox chkTerms;
    private Button btnNext1;

    private EditText inputName, inputBirthdate, inputOccupation;
    private Button btnNext2;

    private EditText inputWeight;
    private Spinner spinUnit;
    private Button btnRegister;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register1);

        inputRegEmail     = findViewById(R.id.input_regemail);
        inputRegPassword  = findViewById(R.id.input_regpassword);
        inputRegPassword2 = findViewById(R.id.input_regpassword2);
        chkTerms = findViewById(R.id.chk_terms);
        btnNext1 = findViewById(R.id.btn_next1);
        btnNext1.setOnClickListener(this);

    }

    private void next1(){
        int passLength = inputRegPassword.getText().toString().length();
        if (    !isNotEmpty(inputRegEmail) |
                !isNotEmpty(inputRegPassword) |
                !isNotEmpty(inputRegPassword2)  ) {
            return;
        }
        else if( passLength<6 ){
            inputRegPassword.setError("Password should be at least 6 characters.");
            return;
        }
        else if(!inputRegPassword.getText().toString().equals(inputRegPassword2.getText().toString())){
            inputRegPassword2.setError("Password not matched.");
            return;
        }
        else if( !chkTerms.isChecked() ){
            chkTerms.setError("Required.");
            return;
        }
        setContentView(R.layout.activity_register2);
        inputName   = findViewById(R.id.input_name);
        inputBirthdate  = findViewById(R.id.input_birthdate);
        inputOccupation = findViewById(R.id.input_occupation);
        btnNext2 = findViewById(R.id.btn_next2);
        btnNext2.setOnClickListener(this);
    }
    private void next2(){
        setContentView(R.layout.activity_register3);

        fireAuth = FirebaseAuth.getInstance();
        fireDb = FirebaseDatabase.getInstance().getReference();
        inputWeight = findViewById(R.id.input_weight);
        spinUnit = findViewById(R.id.spin_unit);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.pref_example_list_titles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinUnit.setAdapter(adapter);
        btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(this);
    }
    private void register() {
        if ( !isNotEmpty(inputWeight) ) {
            return;
        }

        String email = inputRegEmail.getText().toString();
        String password = inputRegPassword.getText().toString();
        Log.d("RegisterActivity", email+" "+password );
        fireAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("RegisterActivity","onComplete:"+task.isSuccessful() );
                        if (task.isSuccessful()) {
                            Log.d("RegisterActivity","onComplete: task is successful" );
                            writeNewUser(fireAuth.getCurrentUser().getUid(), fireAuth.getCurrentUser().getEmail());
                        } else {
                            Toast.makeText(getApplicationContext(), "Registration failed.", Toast.LENGTH_SHORT).show();
                            //                updateUI(null);
                        }
                    }
                });
    }

    public void writeNewUser(String uid, String email) {
        Log.d("RegisterActivity","writeNewUser:"+uid+" "+email );
        User userValue = new User(
                email,
                inputName.getText().toString(),
                inputBirthdate.getText().toString(),
                inputOccupation.getText().toString()
        );
        fireDb.child("user-info").child(uid).setValue(userValue);

        fireDb.child("user-entries").child(uid)
                .child(Calendar.getInstance().getTime().toString())
                .setValue(inputWeight.getText().toString());

        Settings settings = new Settings(
                spinUnit.getSelectedItem().toString(),
                false
        );
        fireDb.child("user-settings").child(uid).setValue(settings);
        Log.d("RegisterActivity","writeNewUser: starting MainActivity" );
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
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
        switch ( v.getId() ) {
            case R.id.btn_next1:
                next1();
                break;
            case R.id.btn_next2:
                next2();
                break;
            case R.id.btn_register:
                register();
                break;
        }
    }


}
