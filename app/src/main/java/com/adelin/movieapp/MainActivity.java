package com.adelin.movieapp;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adelin.movieapp.listViewAdapters.MediaInfoArrayAdapter;
import com.adelin.movieapp.utils.CachedArrayList;
import com.adelin.movieapp.utils.MediaInfo;
import com.adelin.movieapp.utils.NetData;
import com.adelin.movieapp.utils.SaveUserData;
import com.adelintmovieapp.R;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String HISTORY_ARRAY = "app.com.adelin.testproject.HISTORY";
    public static final String MOVIE_TITLE = "app.com.adelin.testproject.MOVIE_TITLE";
    public static final String MOVIE_CODE = "app.com.adelin.testproject.MOVIE_CODE";
    public static final int PICK_FROM_FILE = 101;

    private SaveUserData userData;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private Button searchButton;
    private EditText movieTitle;
    private ListView list;
    private CachedArrayList<String> historyStringArrayList;
    private ArrayList<MediaInfo> mediaInfoArrayList;
    private MediaInfoArrayAdapter mediaInfoArrayAdapter;
    private TextView userName;
    private TextView userMail;
    private Button saveDialogButton;
    private ImageView userPhoto;

    private static Context context;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.context = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userData = new SaveUserData(this);
        historyStringArrayList = new CachedArrayList<>(this, "history");

        mediaInfoArrayList = new ArrayList<>();
        mediaInfoArrayAdapter = new MediaInfoArrayAdapter(this, R.layout.media_list_item, mediaInfoArrayList);

        userPhoto = findViewById(R.id.user_photo);
        searchButton = findViewById(R.id.searchButton);
        movieTitle = findViewById(R.id.movieInput);
        list = findViewById(R.id.list);

        list.setAdapter(mediaInfoArrayAdapter);

        searchButton.setOnClickListener((v) -> searchMovie());
        list.setOnItemClickListener(this::onMovieClickedHandler);
        list.setOnItemLongClickListener(this::onLongMovieClickedHandler);
        createSlidingMenu();
    }

    /**
     * creating the slide menu and getting references to its items
     */
    private void createSlidingMenu() {
        drawerLayout = findViewById(R.id.drawer);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        actionBarDrawerToggle.syncState();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        NavigationView navigationView = findViewById(R.id.nav_menu);
        navigationView.setNavigationItemSelectedListener(this);

        View header = navigationView.getHeaderView(0);

        userPhoto = header.findViewById(R.id.user_photo);
        userPhoto.setOnClickListener(this::imageChoose);
        if (userData.getUser().getPhoto() != null) {
            userPhoto.setImageBitmap(userData.getUser().getPhoto());
        }
        userName = header.findViewById(R.id.user_name);
        userMail = header.findViewById(R.id.user_email);
        userName.setText(userData.getUser().getUserName());
        userMail.setText(userData.getUser().getUserMail());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    /**
     * switching the buttons for the menu buttons
     *
     * @param item menuitem selected
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
            case R.id.historyButton:
                historyButtonHandler();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Toast.makeText(MainActivity.this, " was selected", Toast.LENGTH_LONG).show();
        return true;
    }

    /**
     * getting data when coming back from another intent
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            movieTitle.setText(Objects.requireNonNull(data).getStringExtra(MOVIE_TITLE));
            historyStringArrayList.clear();
            historyStringArrayList.addAll(data.getStringArrayListExtra(HISTORY_ARRAY));
        }
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_FROM_FILE) {
                Uri dataUri = Objects.requireNonNull(data).getData();

                try {
                    InputStream inputStream = getContentResolver().openInputStream(Objects.requireNonNull(dataUri));
                    Bitmap image = BitmapFactory.decodeStream(inputStream);

                    userPhoto.setImageBitmap(Bitmap.createScaledBitmap(image, 90, 90, false));
                    userData.updatePhoto(image);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * handling tap on a movie item on the search list
     *
     * @param adaptiveView
     * @param view
     * @param position
     * @param id
     */
    private void onMovieClickedHandler(View adaptiveView, View view, int position, long id) {
        Toast toast = Toast.makeText(this, mediaInfoArrayList.get(position).getTitle(), Toast.LENGTH_LONG);
        toast.show();
        Intent intent = new Intent(this, MediaDetails.class);
        intent.putExtra(MOVIE_CODE, mediaInfoArrayList.get(position).getIMDBCode());
        startActivityForResult(intent, 2);
    }

    /**
     * handling long tap on an item in the search list
     *
     * @param adaptiveView
     * @param view
     * @param position
     * @param id
     * @return
     */
    private boolean onLongMovieClickedHandler(View adaptiveView, View view, int position, long id) {
        Toast toast = Toast.makeText(this, mediaInfoArrayList.get(position).getTitle(), Toast.LENGTH_LONG);
        toast.show();
        return true;
    }

    /**
     * handler for history button
     * to change to the history page
     */
    private void historyButtonHandler() {
        Intent historyIntent = new Intent(this, HistoryActivity.class);
        historyIntent.putExtra(HISTORY_ARRAY, historyStringArrayList);
        startActivityForResult(historyIntent, 1);
    }

    public static Context getContext() {
        return context;
    }

    /**
     * extra methot to check the presence of an item in history array
     *
     * @param title string representing the search input
     * @return true if it is present in history, false otherwise
     */
    private boolean historyContainsTitle(String title) {
        for (String s : historyStringArrayList) {
            if (s.toLowerCase().equals(title.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void searchMovie() {
        movieTitle.setEnabled(false);
        movieTitle.setEnabled(true);
        StringBuilder stringBuilder = new StringBuilder();
        Thread thread = new Thread(() -> {
            try {
                stringBuilder.append(NetData.accesData(movieTitle.getText().toString().replaceAll(" ", "%20")));
                runOnUiThread(() -> {
                    if (!historyContainsTitle(movieTitle.getText().toString())) {
                        historyStringArrayList.add(movieTitle.getText().toString());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(e.getMessage()).setTitle("Alerta!");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                });
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                mediaInfoArrayList.clear();
                for (MediaInfo elem : NetData.getMediaInfos()) {
                    mediaInfoArrayList.add(new MediaInfo(elem.getTitle(), elem.getYear(), elem.getIMDBCode(), elem.getPosterLink()));
                }
                mediaInfoArrayAdapter.notifyDataSetChanged();
            });
        });
        thread.start();
    }

    /**
     * handling side menu buttons
     *
     * @param menuItem selected menu item from the side menu
     * @return always true
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.closeApp:
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                return true;
            case R.id.fav:
//                Toast.makeText(this, "favourites", Toast.LENGTH_SHORT).show();
                //TODO Faved list
                //TODO Time watched
                //TODO Watched List/Button
                return true;
            case R.id.about:
                //TODO: about
                return true;
            default:
                return true;
        }
    }

    /**
     * check if the side menu is open, then close
     * otherwise, call super
     */
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * handling dialog edit texts and setting save button handler
     *
     * @param v
     */
    public void onItemClick(View v) {
        Dialog thisDialog = new Dialog(this);
        thisDialog.setContentView(R.layout.dialog_change_name);
        thisDialog.show();

        EditText dialogName = thisDialog.findViewById(R.id.name_dialog);
        EditText dialogMail = thisDialog.findViewById(R.id.mail_dialog);
        if (!(userName.getText().toString().equals("User") && userMail.getText().toString().equals("E-mail"))) {
            dialogName.setText(userName.getText().toString());
            dialogMail.setText(userMail.getText().toString());
        }

        saveDialogButton = thisDialog.findViewById(R.id.save_doalog_button);
        saveDialogButton.setOnClickListener((vi) -> dialogSaveButtonHandler(dialogName, dialogMail, thisDialog));
    }

    /**
     * save button handler, making changes on side menu as well
     *
     * @param dialogName edit text on the dialog representing user's name
     * @param dialogMail edit text on the dialog representing user's mail
     * @param thisDialog the dialog in which the user inserts their data
     */
    private void dialogSaveButtonHandler(EditText dialogName, EditText dialogMail, Dialog thisDialog) {
        if (dialogName.getText().toString().equals("") || dialogMail.getText().toString().equals("")) {
            Toast.makeText(this, "Cannot be empty", Toast.LENGTH_LONG).show();
        } else {
            userName.setText(dialogName.getText().toString());
            userMail.setText(dialogMail.getText().toString());

            userData.update(userName.getText().toString(), userMail.getText().toString());

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
            thisDialog.cancel();
        }

    }

    /**
     * starting an action to pick an image from gallery
     * @param view
     */
    private void imageChoose(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Uri data = Uri.parse(pictureDir.getPath());
        intent.setDataAndType(data, "image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_FROM_FILE);
    }
}
