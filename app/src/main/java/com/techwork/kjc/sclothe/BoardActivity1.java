package com.techwork.kjc.sclothe;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BoardActivity1 extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {
    private RecyclerView recyclerView1;
    private List<ClotheDTO> clotheDTOs = new ArrayList<>();
    private List<String> uidLists = new ArrayList<>();
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private int mode = 0;
    private EditText txtTagContent;
    private FirebaseAuth auth;
    BoardRecycleViewAdapter boardRecycleViewAdapter;
    TextView selbtn1;
    TextView selbtn2;
    TextView selbtn3;
    String mUid;

    NfcAdapter nfcAdapter;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board1);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        recyclerView1 = (RecyclerView)findViewById(R.id.recycleview1);
        recyclerView1.setLayoutManager(new LinearLayoutManager(this));
        boardRecycleViewAdapter = new BoardRecycleViewAdapter();
        recyclerView1.setAdapter(boardRecycleViewAdapter);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        txtTagContent = (EditText)findViewById(R.id.txtTagContent);
        selbtn1 = (TextView)findViewById(R.id.selbtn1);
        selbtn2 = (TextView)findViewById(R.id.selbtn2);
        selbtn3 = (TextView)findViewById(R.id.selbtn3);
        auth = FirebaseAuth.getInstance();
        mUid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},0);
        } // 권한
        database.getReference().child("Users").child(mUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                clotheDTOs.clear();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                ClotheDTO clotheDTO = snapshot.getValue(ClotheDTO.class);
                if(mode==0&&clotheDTO.check==1)
                    clotheDTOs.add(clotheDTO);
                else if(mode==1&&clotheDTO.count>=30)
                    clotheDTOs.add(clotheDTO);
                else if(mode==2)
                    clotheDTOs.add(clotheDTO);
            }
                boardRecycleViewAdapter.notifyDataSetChanged();

        }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter!=null && nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC available!", Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(this, "NFC not availble:(", Toast.LENGTH_LONG).show();
        }


    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefMessage ndefMessage = createNdefMessage(txtTagContent.getText()+"");


    }

    private NdefMessage createNdefMessage(String content){

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord });

        return ndefMessage;

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
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    class BoardRecycleViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_board, parent, false);

            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            ((CustomViewHolder)holder).textView.setText(clotheDTOs.get(position).title);
            ((CustomViewHolder)holder).textView2.setText(clotheDTOs.get(position).description);
            Glide.with(holder.itemView.getContext()).load(clotheDTOs.get(position).imageUrl).into(((CustomViewHolder)holder).imageView);
        }

        @Override
        public int getItemCount() {
            return clotheDTOs.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textView;
            TextView textView2;
            ImageView deleteBtn;

            public CustomViewHolder(View view) {
                super(view);
                imageView = (ImageView)view.findViewById(R.id.item_imageView);
                textView = (TextView)view.findViewById(R.id.item_textView);
                textView2 = (TextView)view.findViewById(R.id.item_textView2);
            }
        }
        private void delete_content(final int position){
            storage.getReference().child("images").child(clotheDTOs.get(position).imageName).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                       Toast.makeText(BoardActivity1.this, "삭제완료", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(BoardActivity1.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                }
            });

            database.getReference().child("images").child(uidLists.get(position)).setValue(null).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(BoardActivity1.this, "삭제가 완료 되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        }
    }
    public void changeMenu(TextView tv){
        selbtn1.setTextColor(getResources().getColor(R.color.colorMainYellow));
        selbtn2.setTextColor(getResources().getColor(R.color.colorMainYellow));
        selbtn3.setTextColor(getResources().getColor(R.color.colorMainYellow));
        selbtn1.setBackgroundColor(getResources().getColor(R.color.colorMainYellow2));
        selbtn2.setBackgroundColor(getResources().getColor(R.color.colorMainYellow2));
        selbtn3.setBackgroundColor(getResources().getColor(R.color.colorMainYellow2));

        tv.setTextColor(getResources().getColor(R.color.colorWhite));
        tv.setBackgroundColor(getResources().getColor(R.color.colorMainYellow));
    }
    public void onClick(View v){
        switch(v.getId()){
            case R.id.plus_but:
                startActivity(new Intent(BoardActivity1.this,MainList1.class));
                break;
            case R.id.selbtn1:
                mode=0;
                changeMenu(selbtn1);
                break;
            case R.id.selbtn2:
                mode=1;
                changeMenu(selbtn2);
                break;
            case R.id.selbtn3:
                mode=2;
                changeMenu(selbtn3);
                break;
            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(BoardActivity1.this,MainActivity.class));
                break;

            default:
                break;
        }
        database.getReference().child("Users").child(mUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                clotheDTOs.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ClotheDTO clotheDTO = snapshot.getValue(ClotheDTO.class);
                    if (mode == 0 && clotheDTO.check == 1)
                        clotheDTOs.add(clotheDTO);
                    else if (mode == 1 && clotheDTO.count >= 10)
                        clotheDTOs.add(clotheDTO);
                    else if (mode == 2)
                        clotheDTOs.add(clotheDTO);
                }
                boardRecycleViewAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void readTextFromMessage(NdefMessage ndefMessage) {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0){

            NdefRecord ndefRecord = ndefRecords[0];
            String tagContent = getTextFromNdefRecord(ndefRecord);
            txtTagContent.setText(tagContent);

        }else{
            Toast.makeText(this, "No NDEF records found!", Toast.LENGTH_SHORT).show();
        }
    }
    //태그읽기

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
    } //태그값 읽어오기

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
    private void enableForegroundDispatchSystem(){

        Intent intent = new Intent(this, MainList1.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        IntentFilter[] intentFilters = new IntentFilter[] {};
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }


    private void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
    }




}
