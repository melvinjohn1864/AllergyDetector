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
import android.widget.TextView;
import android.widget.Toast;

import com.example.allergydetector.HomeActivity;
import com.example.allergydetector.PreferenceController;
import com.example.allergydetector.R;
import com.example.allergydetector.authentication.models.AuthenticationRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText et_email;
    private EditText et_password;
    private Button loginButton;
    private TextView tv_register;

    private FirebaseAuth auth;

    public static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        et_email = findViewById(R.id.et_email);
        et_password = findViewById(R.id.et_password);
        loginButton = findViewById(R.id.loginButton);
        tv_register = findViewById(R.id.tv_register);


        loginButton.setOnClickListener(this);
        tv_register.setOnClickListener(this);
    }

    private boolean validateLoginTextFields(){
        if (TextUtils.isEmpty(et_email.getText().toString())){
            String error_message = getResources().getString(R.string.empty_email);
            et_email.requestFocus();
            et_email.setError(error_message);
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
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.loginButton:
                if (!validateLoginTextFields()){
                    callLoginApi();
                }
                break;
            case R.id.tv_register:
                Intent registerIntent = new Intent(LoginActivity.this,RegistrationActivity.class);
                startActivity(registerIntent);
                break;
        }
    }

    private void callLoginApi() {
        AuthenticationRequest loginRequest = new AuthenticationRequest();
        loginRequest.setEmailId(et_email.getText().toString());
        loginRequest.setPassword(et_password.getText().toString());

        loginUser(loginRequest);
        showProgressDialog(LoginActivity.this);

    }

    public static void showProgressDialog(Context context) {
        dismissProgressDialog();
        progressDialog = new ProgressDialog(context, R.style.MyTheme);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
        progressDialog.setCancelable(false);
        try {
            progressDialog.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void loginUser(AuthenticationRequest loginRequestParams){
        auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(loginRequestParams.getEmailId(),loginRequestParams.getPassword())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            dismissProgressDialog();
                            Toast.makeText(LoginActivity.this,"Login Successful",Toast.LENGTH_SHORT).show();
                            PreferenceController.setPreference(LoginActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID, auth.getCurrentUser().getUid());
                            PreferenceController.setPreference(LoginActivity.this, PreferenceController.PreferenceKeys.COUNT_ID, 0);
                            goToHomeActivity();

                        }else {
                            PreferenceController.setPreference(LoginActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID, "");
                        }
                    }
                });
    }

    private void goToHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
