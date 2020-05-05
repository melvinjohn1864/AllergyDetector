package com.example.allergydetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class DetectTextActivity extends AppCompatActivity {

    private Button takePictureButton;

    private Camera camera;
    private CameraPreview cameraPreview;
    private Camera.PictureCallback pictureCallback;

    private LinearLayout linearLayoutPreview;
    private View focusLayout;

    private ImageView ingredientImage;

    private Bitmap bitmapImage;

    private Button retake;
    private Button usePicture;

    private ConstraintLayout imageLayout;
    private ArrayList<String> allergies;
    private ConstraintLayout rectangleFocus;
    private ConstraintLayout greenAlert;
    private ConstraintLayout redAlert;
    private boolean allergyDetection = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_text1);
        linearLayoutPreview = findViewById(R.id.texture);
        focusLayout = findViewById(R.id.focusTexture);
        takePictureButton = findViewById(R.id.btn_scan);

        ingredientImage = findViewById(R.id.ingredientImage);
        retake = findViewById(R.id.retake);
        usePicture = findViewById(R.id.usePicture);

        imageLayout = findViewById(R.id.imageLayout);
        rectangleFocus = findViewById(R.id.rectangleFocus);
        redAlert = findViewById(R.id.red_alert);
        greenAlert = findViewById(R.id.green_alert);

        allergies = getIntent().getStringArrayListExtra("allergies");

        if (checkAndRequestPermissions()){
            setupPreview();

        }

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, pictureCallback);
            }
        });

        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageLayout.setVisibility(View.GONE);
                redAlert.setVisibility(View.GONE);
                greenAlert.setVisibility(View.GONE);
                cameraPreview.refreshCamera(camera);

            }
        });

        usePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanImage();
            }
        });

    }

    private boolean checkAndRequestPermissions() {
        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        int permissionReadStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 402);
            return false;
        }
        return true;
    }



    private void setupPreview() {
        camera = Camera.open();
        cameraPreview = new CameraPreview(getBaseContext(), camera);

        try {
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);
        }catch (Exception e){

        }

        linearLayoutPreview.addView(cameraPreview);
        camera.setDisplayOrientation(90);
        camera.startPreview();
        pictureCallback = getPictureCallback();
        cameraPreview.refreshCamera(camera);
    }


    @Override
    protected void onPause() {
        super.onPause();

        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        return new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if (data != null){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    //Bitmap croppedImage = Bitmap.createBitmap(bitmap, leftValue, topValue, focusWidth, focusHeight);
                    Bitmap croppedImage = cropImage(bitmap,linearLayoutPreview,focusLayout);
                    Bitmap rotatedBitmap = rotateImage(croppedImage,90);
                    ingredientImage.setImageBitmap(rotatedBitmap);
                    imageLayout.setVisibility(View.VISIBLE);
                    //scanImage();
                    //cameraPreview.refreshCamera(camera);
                }
            }
        };
    }

    private void scanImage() {
        BitmapDrawable drawable = (BitmapDrawable) ingredientImage.getDrawable();
        bitmapImage = drawable.getBitmap();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapImage);

        recognizeText(image);
    }

    private void recognizeText(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                processTextRecogniseResults(firebaseVisionText);
            }

        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void processTextRecogniseResults(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

        if (blocks.size() == 0){
            Toast.makeText(DetectTextActivity.this,"No Texts Detected",Toast.LENGTH_SHORT).show();
            return;
        }

        for (FirebaseVisionText.TextBlock block: blocks) {
            String text = block.getText().toLowerCase();
            checkForAllergy(text);
            }
    }

    private void checkForAllergy(String text) {
        for (int i = 0; i < allergies.size();i++){
            if(text.contains(allergies.get(i).toLowerCase())){
                Toast.makeText(DetectTextActivity.this, "Allergy Detected", Toast.LENGTH_SHORT).show();
                allergyDetection = true;
                break;

            }
        }

        if(allergyDetection){
            imageLayout.setVisibility(View.GONE);
            rectangleFocus.setVisibility(View.GONE);
            redAlert.setVisibility(View.VISIBLE);
            greenAlert.setVisibility(View.GONE);
            cameraPreview.refreshCamera(camera);
        }else {
            imageLayout.setVisibility(View.GONE);
            rectangleFocus.setVisibility(View.GONE);
            redAlert.setVisibility(View.GONE);
            greenAlert.setVisibility(View.VISIBLE);
            cameraPreview.refreshCamera(camera);
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }


    public static Bitmap cropImage(Bitmap bitmap, View frame, View reference){
        int heightOriginal = frame.getHeight();
        int widthOriginal = frame.getWidth();

        int heightFrame = reference.getHeight();
        int widthFrame = reference.getWidth();
        int leftFrame = reference.getLeft();
        int topFrame = reference.getTop();

        int heightReal = bitmap.getHeight();
        int widthReal = bitmap.getWidth();

        int widthFinal = (widthFrame * widthReal)/widthOriginal;
        int heightFinal = (heightFrame * heightReal)/heightOriginal;
        int leftFinal = (leftFrame * widthReal)/widthOriginal;
        int topFinal = (topFrame * heightReal)/heightOriginal;

        Bitmap bitmapfinal = Bitmap.createBitmap(bitmap,
                leftFinal, topFinal, widthFinal, heightFinal);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmapfinal.compress(Bitmap.CompressFormat.JPEG, 100, stream);

        byte[] outputArray = stream.toByteArray();

        Bitmap finalOutput = BitmapFactory.decodeByteArray(outputArray, 0, outputArray.length);

        return finalOutput;
    }


}
