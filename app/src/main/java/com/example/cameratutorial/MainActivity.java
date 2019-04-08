package com.example.cameratutorial;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    final Context context = this;

    private View btnGallery;
    private ImageView imageView;

    private File tempFile;

    private static final int CAMERA_PICK = 1;
    private static final int PICK_FROM_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGallery = findViewById(R.id.btn_gallery);

        imageView = (ImageView) findViewById(R.id.image);

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        if(permissionCheck == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_GALLERY);
            }
        });
    }

    public void ClickPicture(View v) {
        final CharSequence[] items = { "사진 촬영", "앨범에서 사진 선택"};
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // 제목셋팅
        alertDialogBuilder.setTitle("사진 가져오기");
        alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        takePhoto();
                        break;
                    case 1:
                        takeGallery();
                        break;
                }
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PICK && resultCode == RESULT_OK) {
            rotate_getImage();
        } else if (requestCode == PICK_FROM_GALLERY && resultCode == RESULT_OK) {
            getImage(data);
        }
    }

    private void rotate_getImage(){
        ImageView imageView = findViewById(R.id.image);

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap originalBm = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), options);

        Bitmap rotatedBitmap = null;
        ExifInterface ei = null;
        if (originalBm != null) {
            try {
                ei = new ExifInterface(tempFile.getAbsolutePath());
            }
            catch (IOException e){
                e.printStackTrace();
            }
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            rotatedBitmap = rotate(originalBm, exifOrientationToDegrees(orientation));
        }
        imageView.setImageBitmap(rotatedBitmap);
    }

    private void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        tempFile = null;
        try {
            tempFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

        if (tempFile != null) {
            Uri photoUri = Uri.fromFile(tempFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(cameraIntent, CAMERA_PICK);
        }

        //미디어 스캐닝
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(tempFile));
        sendBroadcast(intent);
    }

    private File createImageFile() throws IOException {
        // 이미지 파일 이름 ( {시간}_ )
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        // String imageFileName = timeStamp;

        // 이미지가 저장될 폴더 이름 ( Questrip )
        File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Questrip/");
        if (!storageDir.exists())
            storageDir.mkdirs();

        // 빈 파일 생성
        File image = File.createTempFile(timeStamp, ".jpg", storageDir);

        return image;
    }

    private void takeGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }

    private void getImage(Intent data){
        Uri photoUri = data.getData();

        Cursor cursor = null;

        try {
            String[] proj = { MediaStore.Images.Media.DATA };

            assert photoUri != null;
            cursor = getContentResolver().query(photoUri, proj, null, null, null);
            assert cursor != null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();

            tempFile = new File(cursor.getString(column_index));

            //미디어 스캐닝
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(tempFile));
            sendBroadcast(intent);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        rotate_getImage();
    }

    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Bitmap rotate(Bitmap bitmap, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}