package com.example.allergydetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements CustomDialogFragment.DialogListener,AllergyAdapter.OnRecyclerViewSelectItem<String> {

    private FloatingActionButton floatingActionButton;
    private FloatingActionButton scanPhoto;
    private RecyclerView allergyRecyclerView;
    private List<String> allergyList;
    private AllergyAdapter allergyAdapter;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private ImageView logout;
    private FloatingActionButton shareAllergy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        allergyList = new ArrayList<>();
        getAllergyDetails();

        floatingActionButton = findViewById(R.id.addAllergy);
        scanPhoto = findViewById(R.id.scanPhoto);
        allergyRecyclerView = findViewById(R.id.allergyRecyclerView);
        logout = findViewById(R.id.logout);
        shareAllergy = findViewById(R.id.shareAllergy);

        GridLayoutManager manager = new GridLayoutManager(HomeActivity.this, 1);
        allergyRecyclerView.setLayoutManager(manager);

        allergyAdapter = new AllergyAdapter(allergyList,this);
        allergyRecyclerView.setAdapter(allergyAdapter);


        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomDialogFragment dialogFragment = new CustomDialogFragment();

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
                if (prev != null) {
                    fragmentTransaction.remove(prev);
                }
                fragmentTransaction.addToBackStack(null);


                dialogFragment.show(fragmentTransaction, "dialog");
            }
        });

        scanPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, DetectTextActivity.class);
                intent.putStringArrayListExtra("allergies", (ArrayList<String>) allergyList);
                startActivity(intent);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutAlert();
            }
        });

        shareAllergy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String allergyShare = getTheAllergyCOntents();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Allergy Items");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, allergyShare);
                startActivity(Intent.createChooser(sharingIntent, "Share Using"));
            }
        });
    }

    private String getTheAllergyCOntents() {
        String textToSend = "";
        for (int i = 0;i < allergyList.size();i++){
            textToSend = textToSend + allergyList.get(i) + ", ";
        }
        return textToSend;
    }

    @Override
    public void onFinishEditDialog(String allergy) {
        Toast.makeText(HomeActivity.this,allergy,Toast.LENGTH_SHORT).show();
        //allergyList.add(allergy);
        sendAllergyDetailToFirebase(allergy);
    }

    private void sendAllergyDetailToFirebase(String allergy) {
        auth = FirebaseAuth.getInstance();
        String key = auth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("allergies " + key);
        databaseReference.child(allergy).setValue(allergy);
        getAllergyDetails();

    }

    private void getAllergyDetails(){
        String key = PreferenceController.getStringPreference(HomeActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("allergies " + key);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> list = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String allergy = ds.getValue(String.class);
                    list.add(allergy);
                }
                if (list.size() != 0){
                    allergyList.clear();
                    allergyList.addAll(list);
                    allergyAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("", databaseError.getMessage());
            }
        });
    }

    private void deleteAllergy(final String selectedAllergy){
        String key = PreferenceController.getStringPreference(HomeActivity.this, PreferenceController.PreferenceKeys.PREFERENCE_ID);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("allergies " + key);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String allergy = ds.getValue(String.class);
                    if (selectedAllergy.toLowerCase().equalsIgnoreCase(allergy.toLowerCase())){
                        final String key = ds.getKey();
                        databaseReference.child(key).removeValue();
                        allergyList.remove(allergy);
                        allergyAdapter.notifyDataSetChanged();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("", databaseError.getMessage());
            }
        });
    }

    @Override
    public void deleteAllergy(int position, String allergy) {
        allergyAdapter.notifyItemRemoved(position);
        deleteAllergy(allergy);
    }

    private void showLogoutAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this, R.style.todoDialogLight);
        builder.setMessage("Are you sure you want to logout?");

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                PreferenceController.clearWholeData(HomeActivity.this);
                Intent intent = new Intent(HomeActivity.this, SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
