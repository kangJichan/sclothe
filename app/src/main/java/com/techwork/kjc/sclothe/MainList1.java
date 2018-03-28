package com.techwork.kjc.sclothe;

import android.Manifest;
import android.app.PendingIntent;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainList1 extends AppCompatActivity {
    private static final int GALLERY_CODE = 10;
    public static final int REQ_CODE = 11111;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private FirebaseDatabase database;
    private ImageView imageView;
    private EditText title;
    private EditText description;
    private Button uploadBtn;
    private EditText txtTagContent;
    private String imagePath;
    private String tagContent;
    private List<ClotheDTO> clotheDTOs = new ArrayList<>();
    ToggleButton tglReadWrite;
    NfcAdapter nfcAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main_list1);
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        imageView = (ImageView)findViewById(R.id.imageView);
        title = (EditText)findViewById(R.id.title);
        description = (EditText)findViewById(R.id.description);
        uploadBtn = (Button)findViewById(R.id.uploadBtn);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        tglReadWrite = (ToggleButton) findViewById(R.id.tglReadWrite);
        txtTagContent = (EditText) findViewById(R.id.txtTagContent);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();





        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter!=null && nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC available!", Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(this, "NFC not availble:(", Toast.LENGTH_LONG).show();
        }


//        /*권한*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }


        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imagePath==null){ //사진미등록시 등록실패
                    Toast.makeText(MainList1.this, "등록실패", Toast.LENGTH_SHORT).show();
                }else
                upload(imagePath);
                Intent intent = new Intent(getApplicationContext(), BoardActivity1.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(requestCode == GALLERY_CODE){

                imagePath = getPath(data.getData());
                File f = new File(imagePath);
                imageView.setImageURI(Uri.fromFile(f));

                super.onActivityResult(requestCode, resultCode, data);
                if(requestCode == REQ_CODE){

                    if(requestCode == RESULT_OK){
                        String name = data.getExtras().getString("name");

                    }
                }
            }
    }

    public String getPath(Uri uri){
        String [] proj = {MediaStore.Images.Media.DATA};
        CursorLoader cursorLoader = new CursorLoader(this,uri,proj,null,null,null);
        Cursor cursor = cursorLoader.loadInBackground();
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(index);
    }

    public void upload(String uri) {
        Log.w("ttest1",uri);
        StorageReference storageReference = storage.getReferenceFromUrl("gs://sclothe-c9cf8.appspot.com");
        final Uri file = Uri.fromFile(new File(uri));

        StorageReference riversRef = storageReference.child("images/"+file.getLastPathSegment());
        UploadTask uploadTask = riversRef.putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetad ta() contains file metadata such as size, content-type, and download URL.

                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                ClotheDTO clotheDTO = new ClotheDTO();
                clotheDTO.imageUrl = downloadUrl.toString();
                clotheDTO.title = title.getText().toString();
                clotheDTO.description = description.getText().toString();
                clotheDTO.uid = auth.getCurrentUser().getUid();
                clotheDTO.userId = auth.getCurrentUser().getEmail();
                clotheDTO.coltheCode = txtTagContent.getText().toString();
                clotheDTO.imageName = file.getLastPathSegment();
                clotheDTO.check = 1;
                clotheDTO.count =0;

                database.getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).push().setValue(clotheDTO);
            }
        });


    }
    protected void noNewIntent(Intent intent){

        super.onNewIntent(intent);
        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)){
            Toast.makeText(this, "NfcIntent!", Toast.LENGTH_LONG).show();
        }

    }

    protected void onResume() {
        Intent intent = new Intent(this, MainList1.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,0);
        IntentFilter[] intentFilters = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        super.onResume();
        enableForegroundDispatchSystem();
    }

    protected void onPause() {

        nfcAdapter.disableForegroundNdefPush(this);
        disableForegroundDispatchSystem();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)) {

            Toast.makeText(this, "NfcIntent", Toast.LENGTH_SHORT).show();


            if (tglReadWrite.isChecked()) {
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (parcelables != null && parcelables.length > 0) {
                    readTextFromMessage((NdefMessage)parcelables[0]);
                }else{
                    Toast.makeText(this, "No NDEF message found!", Toast.LENGTH_SHORT).show();
                }

            } else {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText() + "");
                writeNdefMessage(tag, ndefMessage);
            }

        }


    } // 태그쓰기

    private void readTextFromMessage(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();
        if (ndefRecords != null && ndefRecords.length > 0) {
            NdefRecord ndefRecord = ndefRecords[0];
            tagContent = getTextFromNdefRecord(ndefRecord);
            txtTagContent.setText(tagContent);

            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // 데이터전체
                        Log.w("ttest1",tagContent);
                        Log.w("ttest1",snapshot.getValue(ClotheDTO.class).coltheCode);

                        if(snapshot.getValue(ClotheDTO.class).coltheCode.equals(tagContent)){

                            int temp = snapshot.getValue(ClotheDTO.class).check==1?0:1;
                            Log.w("ttest2",snapshot.getKey());
                            int cnt = snapshot.getValue(ClotheDTO.class).count + 1;
                            String tit = snapshot.getValue(ClotheDTO.class).title;
                            title.setText(tit);
                            String des = snapshot.getValue(ClotheDTO.class).description;
                            description.setText(des);

                            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(snapshot.getKey()).child("count").setValue(cnt);
                            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(snapshot.getKey()).child("check").setValue(temp);
                        }

                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }


    private String getTextFromNdefRecord(NdefRecord ndefRecord) {
        String tagContent = null;
        try{
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize -1, textEncoding);

        }catch(UnsupportedEncodingException e){
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }
        return tagContent;
    }


    private void enableForegroundDispatchSystem(){

        Intent intent = new Intent(this, MainList1.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        IntentFilter[] intentFilters = new IntentFilter[] {};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    private void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage){
        try{
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable == null){
                Toast.makeText(this, "Tag is not ndef formatable!", Toast.LENGTH_SHORT).show();

                ndefFormatable.connect();
                ndefFormatable.format(ndefMessage);
                ndefFormatable.close();
                Toast.makeText(this, "Tag is writen!", Toast.LENGTH_SHORT).show();
                return;
            }
        }catch (Exception e){
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage){

        try{
            if(tag == null)
            {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }


            Ndef ndef = Ndef.get(tag);
            if(ndef == null)
            {
                // format tag with the ndef format and writes the message
                formatTag(tag, ndefMessage);
            }else
            {
                ndef.connect();
                if(!ndef.isWritable()){
                    Toast.makeText(this, "tag is not writable!", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                Toast.makeText(this, "Tag is writen!", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Log.e("Write tag!", e.getMessage());
        }

    }

    private NdefRecord createTextRecord(String content){
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");
            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch(UnsupportedEncodingException e){
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }

    private NdefMessage createNdefMessage(String content){

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord });

        return ndefMessage;

    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.imageView:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, GALLERY_CODE);
                break;
            default:
                break;
        }
    }
    public void tglReadWriteOnClick(View view){
        txtTagContent.setText("");
    }


}
