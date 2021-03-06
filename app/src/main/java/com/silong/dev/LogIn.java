package com.silong.dev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogIn extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference databaseReferenceUser;
    private DatabaseReference databaseReferenceAdmin;

    Button signUp, logIn;
    EditText tfloginEmail, tfloginPassword;
    TextView forgotPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //for hiding status bar
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        //for transpa status bar
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        //make fully Android Transparent Status bar
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        //Initialize Firebase objects
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://silongdb-1-default-rtdb.asia-southeast1.firebasedatabase.app/");

        tfloginEmail = findViewById(R.id.tfloginEmail);
        tfloginPassword = findViewById(R.id.tfloginPassword);

        //For auto-fill after registration
        try {
            String email = (String) getIntent().getStringExtra("email");
            String password = (String) getIntent().getStringExtra("password");
            tfloginEmail.setText(email);
            tfloginPassword.setText(password);
        }
        catch (Exception e){
            //ignore, no value passed by previous activity
        }

        signUp = (Button) findViewById(R.id.btnSignup);
        logIn = (Button) findViewById(R.id.btnLogin);
        forgotPass = (TextView) findViewById(R.id.forgotPassword);


        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = tfloginEmail.getText().toString();
                String password = tfloginPassword.getText().toString();

                Pattern pattern = Pattern.compile("^(.+)@(.+)$");
                Matcher matcher = pattern.matcher(email);

                if (email.equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter your email.", Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (!matcher.matches()){
                    Toast.makeText(getApplicationContext(), "Please check the format of your email.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter your password.", Toast.LENGTH_SHORT).show();
                    return;
                }


                attemptLogin(email, password);
            }
        });

        //intent to SignUp Screen
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LogIn.this, SignUp.class);
                startActivity(intent);
            }
        });

        forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotPassDia(LogIn.this);
            }
        });
    }
    private void attemptLogin(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Intent intent = new Intent(LogIn.this, LoginLoadingScreen.class);
                intent.putExtra("UID", mAuth.getCurrentUser().getUid());
                startActivity(intent);
                finish();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(LogIn.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //method for forgot password dialog
    public void forgotPassDia(Context context){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(Html.fromHtml("<b>"+"Forgot Password"+"</b>"));
        builder.setIcon(getDrawable(R.drawable.forgotpass_icon));
        builder.setBackground(getDrawable(R.drawable.dialog_bg));
        builder.setMessage("\nEnter registered email address.");

        LinearLayout recov_layout = new LinearLayout(context);
        recov_layout.setOrientation(LinearLayout.VERTICAL);
        recov_layout.setVerticalGravity(10);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(60,0,60,0);
        EditText et_recovEmail = new EditText(context);
        et_recovEmail.setBackground(getResources().getDrawable(R.drawable.tf_background));
        et_recovEmail.setPadding(30,0,0,0);
        et_recovEmail.setHint("Email Address");
        et_recovEmail.setTextSize(15);
        et_recovEmail.setLayoutParams(params);
        et_recovEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        recov_layout.addView(et_recovEmail);
        builder.setView(recov_layout);

        builder.setPositiveButton(Html.fromHtml("<b>"+"SUBMIT"+"</b>"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Check if field is empty
                String email = et_recovEmail.getText().toString();
                Pattern pattern = Pattern.compile("^(.+)@(.+)$");
                Matcher matcher = pattern.matcher(email);
                if(email.equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter your email.", Toast.LENGTH_SHORT).show();
                }
                else if (!matcher.matches()){
                    Toast.makeText(getApplicationContext(), "Please check the format of your email.", Toast.LENGTH_SHORT).show();
                }else{
                    //Check if email is registered
                    emailChecker(context, email);

                }
            }
        });
        builder.setNegativeButton(Html.fromHtml("<b>"+"CANCEL"+"</b>"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //codes here
            }
        });
        builder.show();
    }

    private void emailChecker(Context context, String email){
        //Check internet connection
        if(internetConnection()){
            //Check if email is registered
            mAuth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                        @Override
                        public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                            if (task.getResult().getSignInMethods().isEmpty()){
                                Toast.makeText(getApplicationContext(), "Email is not registered.", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                //Trigger Firebase to send instruction email
                                resetPassword(context, email);
                            }
                        }
                    });
        }
        else {
            Toast.makeText(getApplicationContext(), "Please check your internet connection.", Toast.LENGTH_SHORT).show();
        }

    }

    private void resetPassword(Context context, String email){

        //Send a password reset link to email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            //Show email instruction dialog
                            accountRecovDia(context);
                        }
                    }
                });
    }

    //method for Account Recovery Dialog
    public void accountRecovDia(Context context){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(Html.fromHtml("<b>"+"Account Recovery"+"</b>"));
        builder.setIcon(R.drawable.accrecovery_icon);
        builder.setBackground(getDrawable(R.drawable.dialog_bg));
        builder.setMessage(getResources().getString(R.string.accRecovMsg));

        builder.setPositiveButton(Html.fromHtml("<b>"+"OK"+"</b>"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Codes here
                //No codes here, no further action needed
            }
        });
        builder.show();
    }

    //for transpa status bar
    public static void setWindowFlag(Activity activity, final int bits, boolean on) {

        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private boolean internetConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo!=null){
            return true;
        }
        return false;
    }
}