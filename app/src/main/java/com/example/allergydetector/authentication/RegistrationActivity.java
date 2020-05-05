package com.example.allergydetector.authentication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.allergydetector.HomeActivity;
import com.example.allergydetector.PreferenceController;
import com.example.allergydetector.R;
import com.example.allergydetector.authentication.models.AuthenticationRequest;
import com.example.allergydetector.authentication.models.UserDetails;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_username;
    private EditText et_password;
    private EditText et_email;
    private EditText et_phone;

    private Button registerButton;
    private TextView tv_log_in;

    public static ProgressDialog sProgressDialog;

    private FirebaseAuth auth;

    private DatabaseReference databaseReference;
    private UserDetails userDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        et_username = findViewById(R.id.et_username);
        et_password = findViewById(R.id.et_password);
        et_email = findViewById(R.id.et_email);
        et_phone = findViewById(R.id.et_phone);

        registerButton = findViewById(R.id.registerButton);
        tv_log_in = findViewById(R.id.tv_log_in);

        registerButton.setOnClickListener(this);
        tv_log_in.setOnClickListener(this);
    }


    private boolean validateRegistrationTextFields(){
        if (TextUtils.isEmpty(et_username.getText().toString())){
            String error_message = getResources().getString(R.string.empty_username);
            et_username.requestFocus();
            et_username.setError(error_message);
            return true;
        }else if(TextUtils.isEmpty(et_password.getText().toString())){
            String error_message = getResources().getString(R.string.empty_password);
            et_password.requestFocus();
            et_password.setError(error_message);
            return true;
        }else if(et_password.getText().toString().length() < 6){
            String error_message = getResources().getString(R.string.invalid_password);
            et_password.requestFocus();
            et_password.setError(error_message);
            return true;
        }else if(TextUtils.isEmpty(et_email.getText().toString())){
            String error_message = getResources().getString(R.string.empty_email);
            et_email.requestFocus();
            et_email.setError(error_message);
            return true;
        }else if(TextUtils.isEmpty(et_phone.getText().toString())){
            String error_message = getResources().getString(R.string.empty_phone);
            et_phone.requestFocus();
            et_phone.setError(error_message);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.registerButton:
                if (!validateRegistrationTextFields()){
                    callRegistrationApi();
                }
                break;
            case R.id.tv_log_in:
                Intent loginIntent = new Intent(RegistrationActivity.this,LoginActivity.class);
                startActivity(loginIntent);
                break;

        }
    }

    private void callRegistrationApi() {
        AuthenticationRequest registrationRequest = new AuthenticationRequest();
        registrationRequest.setEmailId(et_email.getText().toString());
        registrationRequest.setPassword(et_password.getText().toString());

        userDetails = new UserDetails();
        userDetails.setEmailId(et_email.getText().toString());
        userDetails.setName(et_username.getText().toString());
        userDetails.setPhone(et_phone.getText().toString());

        createNewUser(registrationRequest);

        showProgressDialog(RegistrationActivity.this);
    }


    public static void showProgressDialog(Context context) {
        dismissProgressDialog();
        sProgressDialog = new ProgressDialog(context, R.style.MyTheme);
        sProgressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        sProgressDialog.setCancelable(false);
        try {
            sProgressDialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void dismissProgressDialog() {
        if (sProgressDialog != null && sProgressDialog.isShowing()) {
            sProgressDialog.dismiss();
        }
    }

    public void createNewUser(AuthenticationRequest registrationRequest){
        auth =FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(registrationRequest.getEmailId(),registrationRequest.getPassword())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(RegistrationActivity.this,"User created successfully",Toast.LENGTH_SHORT).show();
                            PreferenceController.setPreference(RegistrationActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID, auth.getCurrentUser().getUid());
                            saveUserDetailsToDatabase();
                            goToHomeActivity();
                        }else {
                            PreferenceController.setPreference(RegistrationActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID, "");

                        }
                    }
                });
    }

    private void saveUserDetailsToDatabase() {
        String key = auth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("userDetails");
        databaseReference.child(key).setValue(userDetails);
    }

    private void goToHomeActivity() {
        Intent intent = new Intent(RegistrationActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
