package com.webbions.textrecognization;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudDocumentTextDetector;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.webbions.textrecognization.GraphicOverlay.Graphic;

public class MainActivity extends AppCompatActivity{
    FirebaseVisionImage image;

    private static final String TAG = "MainActivity";
    private ImageView mImageView;
    private Button mButton;
    private Button mCloudButton,mGetText,mOpenCamera;
    private Bitmap mSelectedImage;
    private GraphicOverlay mGraphicOverlay;
    private Integer mImageMaxWidth;
    private Integer mImageMaxHeight;
    String dataString="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestStoragePermission();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        mImageView = findViewById(R.id.image_view);

        mButton = findViewById(R.id.button_text);
        mCloudButton = findViewById(R.id.button_cloud_text);
        mGetText=findViewById(R.id.getText);
        mOpenCamera=findViewById(R.id.openCamera);

        mGraphicOverlay = findViewById(R.id.graphic_overlay);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataString="";
                if(mSelectedImage!=null)
                    runTextRecognition();
                else
                    Toast.makeText(MainActivity.this,"File not selected",Toast.LENGTH_LONG).show();
//                 runCloudTextRecognition();

            }
        });
        mCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //runCloudTextRecognition();
                showFileChooser();
            }
        });
        mGetText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mSelectedImage!=null){
                    LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                    final View deleteDialogView = factory.inflate(R.layout.mylayout, null);
                    final AlertDialog deleteDialog = new AlertDialog.Builder(MainActivity.this).create();
                    deleteDialog.setView(deleteDialogView);
                    deleteDialogView.findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteDialog.dismiss();
                        }
                    });
                    TextView et=deleteDialogView.findViewById(R.id.myText);
                    et.setText(dataString);
                    deleteDialog.show();
                }
                else
                    Toast.makeText(MainActivity.this,"File not selected",Toast.LENGTH_LONG).show();
            }
        });

        mOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = new File(getApplication().getExternalCacheDir(),
                        String.valueOf("textRecognizer" + ".jpg"));
                Uri fileUri = Uri.fromFile(file);
               // Uri photoURI = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".my.package.name.provider", createImageFile());
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
               // getApplicationContext().startActivityForResult(intent, RC_TAKE_PHOTO);
                startActivityForResult(i,2012);
            }
        });

    }

    private void runTextRecognition() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        mButton.setEnabled(false);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                mButton.setEnabled(true);
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                mButton.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.Block> blocks = texts.getBlocks();
        if (blocks.size() == 0) {
            showToast("No text found");
            return;
        }
        mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            dataString+=blocks.get(i).getText();

            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);
                }
            }
        }
        System.out.println("---------------------------- " + dataString);
    }

    private void runCloudTextRecognition() {
        FirebaseVisionCloudDetectorOptions options =
                new FirebaseVisionCloudDetectorOptions.Builder()
                        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                        .setMaxResults(15)
                        .build();
        mCloudButton.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mSelectedImage);
        FirebaseVisionCloudDocumentTextDetector detector = FirebaseVision.getInstance()
                .getVisionCloudDocumentTextDetector(options);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionCloudText>() {
                            @Override
                            public void onSuccess(FirebaseVisionCloudText texts) {
                                mCloudButton.setEnabled(true);
                                processCloudTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                mCloudButton.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }

    private void processCloudTextRecognitionResult(FirebaseVisionCloudText text) {
        // Task completed successfully
        if (text == null) {
            showToast("No text found");
            return;
        }
        mGraphicOverlay.clear();
        List<FirebaseVisionCloudText.Page> pages = text.getPages();
        for (int i = 0; i < pages.size(); i++) {
            FirebaseVisionCloudText.Page page = pages.get(i);
            List<FirebaseVisionCloudText.Block> blocks = page.getBlocks();
            for (int j = 0; j < blocks.size(); j++) {
                List<FirebaseVisionCloudText.Paragraph> paragraphs = blocks.get(j).getParagraphs();
                for (int k = 0; k < paragraphs.size(); k++) {
                    FirebaseVisionCloudText.Paragraph paragraph = paragraphs.get(k);
                    List<FirebaseVisionCloudText.Word> words = paragraph.getWords();
                    for (int l = 0; l < words.size(); l++) {
                        Graphic cloudTextGraphic = new CloudTextGraphic(mGraphicOverlay, words
                                .get(l));
                        //System.out.println("---------------------------- " + words.get(l));
                        mGraphicOverlay.add(cloudTextGraphic);
                    }
                }
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Integer getImageMaxWidth() {
        if (mImageMaxWidth == null) {
            mImageMaxWidth = mImageView.getWidth();
        }

        return mImageMaxWidth;
    }

    private Integer getImageMaxHeight() {
        if (mImageMaxHeight == null) {
            mImageMaxHeight =
                    mImageView.getHeight();
        }

        return mImageMaxHeight;
    }

    private Pair<Integer, Integer> getTargetedWidthHeight() {
        int targetWidth;
        int targetHeight;
        int maxWidthForPortraitMode = getImageMaxWidth();
        int maxHeightForPortraitMode = getImageMaxHeight();
        targetWidth = maxWidthForPortraitMode;
        targetHeight = maxHeightForPortraitMode;
        return new Pair<>(targetWidth, targetHeight);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    2020);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 2020:
                if (resultCode == RESULT_OK) {
                    mGraphicOverlay.clear();
                    Uri pickedImage = data.getData();
                    // Let's read picked image path using content resolver

                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), pickedImage);
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                    setBitmap(bitmap);
                    System.out.println("---------------------------------- Data : " + pickedImage);
                    mImageView.setImageBitmap(bitmap);
                    break;
                }
            case 2012:
                if (resultCode == RESULT_OK) {
                    File file = new File(getApplication().getExternalCacheDir(),
                            String.valueOf("textRecognizer" + ".jpg"));

                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(),bmOptions);
                    setBitmap(bitmap);
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return;

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA}, 2020);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if (requestCode == 2020) {

            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Displaying a toast
                Toast.makeText(this, "Permission granted now you can read the storage", Toast.LENGTH_LONG).show();
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setBitmap(Bitmap bitmap) {
        mSelectedImage = bitmap;
        mImageView.setImageBitmap(bitmap);

        if (mSelectedImage != null) {
            // Get the dimensions of the View
            Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

            int targetWidth = targetedSize.first;
            int maxHeight = targetedSize.second;

            // Determine how much to scale down the image
            float scaleFactor =
                    Math.max(
                            (float) mSelectedImage.getWidth() / (float) targetWidth,
                            (float) mSelectedImage.getHeight() / (float) maxHeight);

            Bitmap resizedBitmap =
                    Bitmap.createScaledBitmap(
                            mSelectedImage,
                            (int) (mSelectedImage.getWidth() / scaleFactor),
                            (int) (mSelectedImage.getHeight() / scaleFactor),
                            true);

            mImageView.setImageBitmap(resizedBitmap);
            mSelectedImage = resizedBitmap;
        }else{
            System.out.println("----------------------------- Null Bitmap Found");
        }
    }
}
