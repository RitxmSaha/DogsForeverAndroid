package com.example.dogsforeverandroid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "LOGIN ERROR: ";
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private GoogleSignInClient client;
    private FirebaseUser user;
    private ActivityResultLauncher<Intent> someActivityResultLauncher = null;
    private ActivityResultLauncher<Intent> cameraLauncher = null;
    private ActivityResultLauncher<Intent> cameraLauncherForEdit = null;
    private long shelterID = 0;
    private int topDogID = 0;
    private Dog currentDog;
    private int layoutID;
    private boolean refreshPressed = false;
    private boolean showArchived;
    private Bitmap currBitmap;
    private ArrayList<Dog> dogs = new ArrayList<Dog>();
    static final int CAMERA_PERM_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("1002387948440-qvg56v6g5qqh610pqkv3ncbl6srie34k.apps.googleusercontent.com").requestEmail().build();
        client = GoogleSignIn.getClient(this, gso);
        System.out.println("i made it here");
        initializeActivityLauncher();
        initializeSignInListener();
        initializeCameraLauncher();
        initializeCameraLauncherForEdit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        user = currentUser;
        if(user == null) {
            updateUI(null);
            return;
        }
        getUserShelterID(user);
    }

    private void updateUI(FirebaseUser account) {
        if(layoutID == R.layout.activity_login) {
            if(account != null) {
                if (shelterID == 0) {
                    setContentView(R.layout.activity_postlogin);
                    initializeShelterEnterListener();
                    initializeSignOutListener();
                } else {
                    setContentView(R.layout.activity_shelterinfo);
                    ((Switch)findViewById(R.id.switch1)).setChecked(false);
                    showArchived = false;
                    initializeArchiveSwitch();
                    initializeDogArrayListener();
                    initializeSignOutListener();
                    initializeAddDogListener();
                    initializeRefreshButton();
                    ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                    updateListView();
                }
            }
            return;
        }
        if(layoutID == R.layout.activity_postlogin) {
            if(shelterID != 0) {
                setContentView(R.layout.activity_shelterinfo);
                ((Switch)findViewById(R.id.switch1)).setChecked(false);
                showArchived = false;
                initializeArchiveSwitch();
                initializeSignOutListener();
                initializeAddDogListener();
                initializeDogArrayListener();
                initializeRefreshButton();
                ((Switch)findViewById(R.id.switch1)).setChecked(false);
                ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                return;
            } else {
                setContentView(R.layout.activity_postlogin);
                initializeShelterEnterListener();
                initializeSignOutListener();
                return;
            }
        }

        if(layoutID == R.layout.activity_shelterinfo) {

            return;
        }
        if(layoutID == R.layout.activity_adddog) {

            return;
        }
    }




    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        this.layoutID = layoutResID;
    }

    public void initializeDogArrayListener() {
        dogs.clear();
        DatabaseReference myRef = database.getReference();
        myRef.child("ShelterID").orderByValue().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateArrayOfDogs();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void addNewUser(FirebaseUser user) {
        database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("users");
        this.user = user;
        Query firebaseIDQuery = myRef.orderByChild("UID").equalTo(user.getUid());
        myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue() != null) {
                    return;
                } else {
                    writeToDBUsers(user, user.getUid());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void getUserShelterID(FirebaseUser user) {
        database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("users");
        Query firebaseIDQuery = myRef.orderByChild("UID").equalTo(user.getUid());
        myRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    if(snapshot.hasChild("Shelter ID")) {
                        shelterID = (long)snapshot.child("Shelter ID").getValue();
                        updateUI(user);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public void writeToDBUsers(FirebaseUser user, String firebaseUserID) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference myRef = database.child("users/"+firebaseUserID);
        User databaseUser = new User(user.getDisplayName(),user.getEmail());
        myRef.setValue(databaseUser.getTaskMap());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    private void initializeActivityLauncher() {
        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleSignInResult(task);
                        } else {
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleSignInResult(task);
                        }
                    }
                });
    }




    private void askCameraPermissions() {
        if(androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                System.out.println("Camera permission is required to open camera");
            }
        }
    }

    public void initializeCamera() {
        findViewById(R.id.addImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askCameraPermissions();
            }
        });
    }

    private void initializeCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bundle bundle = result.getData().getExtras();
                            Bitmap bitmap = (Bitmap) bundle.get("data");
                            setImage(bitmap);
                        }
                    }
                });
    }

    public void setImage(Bitmap bm) {
        currBitmap = bm;
        ((ImageView)findViewById(R.id.imageView)).setImageBitmap(currBitmap);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }




    public void addDogToDB(int ID) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        if(((EditText)findViewById(R.id.editName)).getText().toString().length() == 0) {
            CharSequence text = "Name Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editMeds)).getText().toString().length() == 0) {
            CharSequence text = "Medication Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editFeedInstr)).getText().toString().length() == 0) {
            CharSequence text = "Feeding Instructions Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editHandlingInfo)).getText().toString().length() == 0) {
            CharSequence text = "Handling Information Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editMisc)).getText().toString().length() == 0) {
            CharSequence text = "Miscellaneous Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        Dog newDog = new Dog(((EditText)findViewById(R.id.editName)).getText().toString(),
                ((EditText)findViewById(R.id.editMeds)).getText().toString(),
                ((EditText)findViewById(R.id.editFeedInstr)).getText().toString(),
                ((EditText)findViewById(R.id.editHandlingInfo)).getText().toString(),
                ((EditText)findViewById(R.id.editMisc)).getText().toString(),ID,currBitmap);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference myRef = database.child("ShelterID/"+newDog.getDogUID());
        myRef.setValue(newDog.getTaskMap());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images");
        StorageReference dogRef = imagesRef.child(newDog.getDogUID()+"");


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = dogRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                setContentView(R.layout.activity_shelterinfo);
                ((Switch)findViewById(R.id.switch1)).setChecked(false);
                showArchived = false;
                initializeArchiveSwitch();
                initializeSignOutListener();
                initializeAddDogListener();
                initializeRefreshButton();
                ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
            }
        });
    }

    public void updateArrayOfDogs() {
        dogs.clear();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        myRef.child("ShelterID").orderByValue().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() != true) {
                    return;
                }
                for(DataSnapshot snap : snapshot.getChildren()) {
                    Dog dog = snap.getValue(Dog.class);
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();
                    StorageReference imagesRef = storageRef.child("images");
                    StorageReference dogRef = imagesRef.child(dog.getDogUID()+"");
                    final long ONE_MEGABYTE = 1024 * 1024;
                    dogRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            dog.setBitmap(bitmap);
                            dogs.add(dog);
                            System.out.println(dogs.size());
                            System.out.println(snapshot.getChildrenCount());
                            if(dogs.size() == snapshot.getChildrenCount()) {
                                updateListView();
                                return;
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void updateListView() {
        ListView lv = (ListView)findViewById(R.id.ListView);
        ArrayList<String> arrayList = new ArrayList<String>();
        Collections.sort(dogs, new ByID());
        ArrayList<Dog> dogsShown = new ArrayList<Dog>();
        for(Dog dog: dogs) {
            if(dog.isArchived() == showArchived) {
                dogsShown.add(dog);
                arrayList.add(dog.getListViewString());
            }
        }
        ArrayAdapter arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,arrayList);
        if(lv == null) return;
        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Dog dog = dogsShown.get(i);
                currentDog = dog;
                setContentView(R.layout.activity_viewdoginfo);
                ((ImageView)findViewById(R.id.displayImage)).setImageBitmap(dog.getDogPhoto());
                ((TextView)findViewById(R.id.displayName)).setText(dog.getName());
                ((TextView)findViewById(R.id.displayMedication)).setText(dog.getMedications());
                ((TextView)findViewById(R.id.displayFeedInstructions)).setText(dog.getFeedInstr());
                ((TextView)findViewById(R.id.displayHandlingInfo)).setText(dog.getHandleInfo());
                ((TextView)findViewById(R.id.displayMisc)).setText(dog.getMisc());
                findViewById(R.id.goBack).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setContentView(R.layout.activity_shelterinfo);
                        if(!currentDog.isArchived()) {
                            ((Switch)findViewById(R.id.switch1)).setChecked(false);
                            showArchived = false;
                        } else {
                            ((Switch)findViewById(R.id.switch1)).setChecked(true);
                            showArchived = true;
                        }
                        initializeArchiveSwitch();
                        initializeSignOutListener();
                        initializeAddDogListener();
                        ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                        updateListView();
                        return;
                    }
                });
                editInfoListener();
            }
        });
    }

    public void editInfoListener() {
        findViewById(R.id.editInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dog dog = currentDog;
                setContentView(R.layout.activity_editdoginfo);
                ((ImageView)findViewById(R.id.editImage)).setImageBitmap(dog.getDogPhoto());
                ((EditText)findViewById(R.id.editName)).setText(dog.getName());
                ((EditText)findViewById(R.id.editMedication)).setText(dog.getMedications());
                ((EditText)findViewById(R.id.editFeedInstructions)).setText(dog.getFeedInstr());
                ((EditText)findViewById(R.id.editHandleInfo)).setText(dog.getHandleInfo());
                ((EditText)findViewById(R.id.editMiscellaneous)).setText(dog.getMisc());
                ((Switch)findViewById(R.id.editArchive)).setChecked(dog.isArchived());
                findViewById(R.id.goBackToView).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Dog dog = currentDog;
                        setContentView(R.layout.activity_viewdoginfo);
                        ((ImageView)findViewById(R.id.displayImage)).setImageBitmap(dog.getDogPhoto());
                        ((TextView)findViewById(R.id.displayName)).setText(dog.getName());
                        ((TextView)findViewById(R.id.displayMedication)).setText(dog.getMedications());
                        ((TextView)findViewById(R.id.displayFeedInstructions)).setText(dog.getFeedInstr());
                        ((TextView)findViewById(R.id.displayHandlingInfo)).setText(dog.getHandleInfo());
                        ((TextView)findViewById(R.id.displayMisc)).setText(dog.getMisc());
                        findViewById(R.id.goBack).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                setContentView(R.layout.activity_shelterinfo);
                                ((Switch)findViewById(R.id.switch1)).setChecked(false);
                                showArchived = false;
                                initializeArchiveSwitch();
                                initializeSignOutListener();
                                initializeAddDogListener();
                                initializeDogArrayListener();
                                initializeRefreshButton();
                                ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                                updateListView();
                                return;
                            }
                        });
                        editInfoListener();
                    }
                });
                findViewById(R.id.saveInfo).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editDogInfoDatabase();
                    }
                });
                findViewById(R.id.editImageButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        editImageCameraOpen();
                    }
                });
            }
        });
    }

    private void editImageCameraOpen() {

        if(androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncherForEdit.launch(takePictureIntent);

        }
    }

    private void initializeCameraLauncherForEdit() {
        cameraLauncherForEdit = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bundle bundle = result.getData().getExtras();
                            Bitmap bitmap = (Bitmap) bundle.get("data");
                            ((ImageView)findViewById(R.id.editImage)).setImageBitmap(bitmap);
                        }
                    }
                });
    }

    public void editDogInfoDatabase() {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        if(((EditText)findViewById(R.id.editName)).getText().toString().length() == 0) {
            CharSequence text = "Name Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editMedication)).getText().toString().length() == 0) {
            CharSequence text = "Medication Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editFeedInstructions)).getText().toString().length() == 0) {
            CharSequence text = "Feeding Instructions Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editHandleInfo)).getText().toString().length() == 0) {
            CharSequence text = "Handling Information Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        if(((EditText)findViewById(R.id.editMiscellaneous)).getText().toString().length() == 0) {
            CharSequence text = "Miscellaneous Field is Empty";
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        Dog dog = currentDog;
        dog.setName(((EditText)findViewById(R.id.editName)).getText().toString());
        dog.setMedications(((EditText)findViewById(R.id.editMedication)).getText().toString());
        dog.setFeedInstr(((EditText)findViewById(R.id.editFeedInstructions)).getText().toString());
        dog.setHandleInfo(((EditText)findViewById(R.id.editHandleInfo)).getText().toString());
        dog.setMisc(((EditText)findViewById(R.id.editMiscellaneous)).getText().toString());
        dog.setArchived(((Switch)findViewById(R.id.editArchive)).isChecked());
        BitmapDrawable drawable = (BitmapDrawable)(((ImageView)findViewById(R.id.editImage)).getDrawable());
        dog.setBitmap(drawable.getBitmap());
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> dogValues = dog.getTaskMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(dog.getDogUID()+"",dogValues);
        myRef.child("ShelterID").updateChildren(childUpdates);


        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images");
        Bitmap bm = dog.getDogPhoto();
        StorageReference dogRef = imagesRef.child(dog.getDogUID()+"");


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = dogRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("upload success");
                setContentView(R.layout.activity_shelterinfo);
                ((Switch)findViewById(R.id.switch1)).setChecked(false);
                showArchived = false;
                initializeArchiveSwitch();
                initializeSignOutListener();
                initializeAddDogListener();
                initializeDogArrayListener();
                initializeRefreshButton();
                ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                updateListView();
            }
        });
    }

    private class ByID implements Comparator<Dog> {
        public int compare(Dog o1, Dog o2) {
            return Integer.compare(o1.getDogUID(),o2.getDogUID());
        }
    }


    public void getHighestCurrID() {
        DatabaseReference myRef = database.getReference();
        myRef.child("ShelterID").orderByValue().addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() != true) {
                    topDogID = 0;
                    return;
                }
                for(DataSnapshot snap : snapshot.getChildren()) {
                    Dog dog = snap.getValue(Dog.class);
                    if(topDogID <= dog.getDogUID()) topDogID = dog.getDogUID();
                }
                topDogID++;
                addDogToDB(topDogID);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public String getEnteredShelterID() {
        return ((EditText)findViewById(R.id.enterShelterCode)).getText().toString();
    }

    public void setUserShelterID() {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference myRef = database.child("users/"+user.getUid()+"/Shelter ID");
        myRef.setValue(shelterID);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            addNewUser(user);
                            getUserShelterID(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(get, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

//initialize buttons

    public void initializeAddDogButton() {
        findViewById(R.id.add_new_dog_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getHighestCurrID();

                //new Dog(String name, String age,String breed, String medicine, String misc, int dogUID, Bitmap dogPhoto)
            }
        });
    }

    public void initializeArchiveSwitch() {
        findViewById(R.id.switch1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showArchived = ((Switch) findViewById(R.id.switch1)).isChecked();
                updateListView();
                }
            });
    }

    public void initializeRefreshButton() {
        findViewById(R.id.refreshList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(refreshPressed) return;
                refreshPressed = true;
                updateListView();
                refreshPressed = false;
            }
        });
    }

    public void initializeSignInListener() {
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.sign_in_button:
                        Intent signInIntent = client.getSignInIntent();
                        someActivityResultLauncher.launch(signInIntent);
                        break;
                }
            }
        });
    }

    public void initializeSignOutListener() {
        findViewById(R.id.sign_out_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.sign_out_button:
                        FirebaseAuth.getInstance().signOut();
                        client.signOut();
                        setContentView(R.layout.activity_login);
                        initializeSignInListener();
                        break;
                }
            }
        });
    }

    public void initializeCancelAdd() {
        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.cancel_button:
                        setContentView(R.layout.activity_shelterinfo);
                        ((Switch)findViewById(R.id.switch1)).setChecked(false);
                        showArchived = false;
                        initializeArchiveSwitch();
                        initializeSignOutListener();
                        initializeAddDogListener();
                        initializeDogArrayListener();
                        initializeRefreshButton();
                        ((TextView)findViewById(R.id.hello_text)).setText("Hello "+user.getDisplayName()+"!");
                        updateListView();
                        break;
                }
            }
        });
    }

    public void initializeAddDogListener() {
        findViewById(R.id.add_new_dog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.add_new_dog:
                        setContentView(R.layout.activity_adddog);
                        initializeCancelAdd();
                        initializeCamera();
                        initializeAddDogButton();
                        break;
                }
            }
        });
    }

    public void initializeShelterEnterListener() {
        findViewById(R.id.shelterButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.shelterButton:
                        database = FirebaseDatabase.getInstance();
                        DatabaseReference myRef = database.getReference();
                        myRef.child("ShelterIDNumber").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if(snapshot.exists()) {
                                    System.out.println(snapshot.getValue());
                                    if(snapshot.getValue().toString().equals(getEnteredShelterID())) {
                                        shelterID = Long.parseLong(getEnteredShelterID());
                                        setUserShelterID();
                                        updateUI(user);
                                        return;
                                    }
                                    Context context = getApplicationContext();
                                    CharSequence text = "Incorrect Shelter ID";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                    //incorrect value
                                    return;
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                }
            }
        });
    }


}
