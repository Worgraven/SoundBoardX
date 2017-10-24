package io.github.xtonousou.soundboardx;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;

import java.util.List;

import petrov.kristiyan.colorpicker.ColorPicker;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    public static final String TAG = "MainActivity";

    static final int RC_WRITE_SETNGS_PERM_AFTER_M = 0x0;
    static final int RC_WRITE_SETNGS_PERM = 0x1;
    static final int RC_WRITE_EXST_PERM = 0x2;

    boolean withAnimations = true;

    SoundPlayer soundPlayer;
    Toolbar mToolbar;

    static InputMethodManager mInputManager;
    static String colorTitle = "#b71c1c";

    ColorPicker colorPicker;
    RecyclerView mView;
    Drawer mDrawer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        SharedPrefs.init(getPreferences(Context.MODE_PRIVATE));

        mInputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mView = (RecyclerView) findViewById(R.id.grid_view);
        mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                .getInteger(R.integer.num_cols),
                StaggeredGridLayoutManager.VERTICAL));
        mView.setAdapter(new SoundAdapter(SoundStore.getAllSounds(this), withAnimations));
        ((SoundAdapter) mView.getAdapter()).showAllSounds(getApplicationContext());

        beautifyToolbar();
        initFAB();
        initDrawer(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeSettingsPermissionAfterM();
            writeExternalStoragePermission();
        } else {
            writeSettingsPermission();
            writeExternalStoragePermission();
        }

        if (SharedPrefs.getInstance().isFirstTime()) {
            SharedPrefs.getInstance().setNotFirstTime("virgin", false);
            SharedPrefs.getInstance().setSelectedColor("color", Color.RED);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        soundPlayer = new SoundPlayer(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        soundPlayer.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.item, menu);
        initSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer != null && mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState = mDrawer.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void beautifyToolbar() {
        ShimmerTextView shimmerTextView = (ShimmerTextView) findViewById(R.id.shimmerTitle);
        Typeface font = Typeface.createFromAsset(shimmerTextView.getContext().getAssets(),
                "fonts/CaviarDreams.ttf");
        shimmerTextView.setTypeface(font);
        new Utils().paintThis(shimmerTextView);
        Shimmer shimmer = new Shimmer();
        if (new Utils().isGreenMode(MainActivity.this)) {
            shimmer.cancel();
        } else {
            if (shimmer.isAnimating()) {
                shimmer.cancel();
            } else {
                shimmer = new Shimmer();
                shimmer.start(shimmerTextView);
                shimmer.setDuration(3500)
                        .setStartDelay(1000)
                        .setDirection(Shimmer.ANIMATION_DIRECTION_LTR);
            }
        }
    }

    public void initDrawer(Bundle instance) {
        mDrawer = new DrawerBuilder()
                .withActivity(MainActivity.this)
                .withDisplayBelowStatusBar(true)
                .withScrollToTopAfterClick(true)
                .withDrawerWidthPx((new Utils().getScreenWidth(MainActivity.this)) - 225)
                .withSliderBackgroundColorRes(R.color.primary_dark)
                .addDrawerItems(
                        new SectionDrawerItem().withName(R.string.categories)
                                .withDivider(false)
                                .withTextColor(new Utils().getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.all)
                                .withSetSelected(true)
                                .withIcon(FontAwesome.Icon.faw_music)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.animals)
                                .withIcon(R.drawable.ic_pets_white_24dp)
                                .withIconTintingEnabled(true)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.funny)
                                .withIcon(R.drawable.ic_sentiment_very_satisfied_white_24dp)
                                .withIconTintingEnabled(true)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.games)
                                .withIcon(FontAwesome.Icon.faw_gamepad)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.movies)
                                .withIcon(FontAwesome.Icon.faw_video_camera)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.thug)
                                .withIcon(R.drawable.thug_white_24dp)
                                .withIconTintingEnabled(true)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.nsfw)
                                .withIcon(R.drawable.ic_wc_white_24dp)
                                .withIconTintingEnabled(true)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new PrimaryDrawerItem().withName(R.string.personal)
                                .withIcon(R.drawable.ic_person_white_24dp)
                                .withIconTintingEnabled(true)
                                .withSelectedColor(ContextCompat.getColor(getApplicationContext(),
                                        R.color.colorPrimaryDark))
                                .withSelectedTextColor(new Utils()
                                        .getSelectedColor())
                                .withSelectedIconColor(new Utils()
                                        .getSelectedColor()),
                        new SectionDrawerItem().withName(R.string.options)
                                .withDivider(false)
                                .withTextColor(new Utils().getSelectedColor()),
                        new SecondaryDrawerItem().withName(R.string.favorites)
                                .withIcon(FontAwesome.Icon.faw_star)
                                .withSelectable(false),
                        new SecondarySwitchDrawerItem().withName(R.string.particles)
                                .withIcon(FontAwesome.Icon.faw_eye)
                                .withSelectable(false)
                                .withChecked(true)
                                .withOnCheckedChangeListener(onCheckedChangeListener),
                        new SecondaryDrawerItem().withName(R.string.color)
                                .withIcon(FontAwesome.Icon.faw_paint_brush)
                                .withSelectable(false),
                        new SecondaryDrawerItem().withName(R.string.support)
                                .withIcon(FontAwesome.Icon.faw_hand_peace_o)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(final View view, int position, IDrawerItem drawerItem) {
                        switch (position) {
                            case 1:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getAllSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showAllSounds(MainActivity.this);
                                break;
                            case 2:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getAnimalsSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showAnimalsSounds(MainActivity.this);
                                break;
                            case 3:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getFunnySounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showFunnySounds(MainActivity.this);
                                break;
                            case 4:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getGamesSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showGamesSounds(MainActivity.this);
                                break;
                            case 5:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getMoviesSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showMoviesSounds(MainActivity.this);
                                break;
                            case 6:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getThugSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showThugSounds(MainActivity.this);
                                break;
                            case 7:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getNSFWSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showNSFWSounds(MainActivity.this);
                                break;
                            case 8:
                                mView.setLayoutManager(new StaggeredGridLayoutManager(getResources()
                                        .getInteger(R.integer.num_cols),
                                        StaggeredGridLayoutManager.VERTICAL));
                                mView.setAdapter(new SoundAdapter(SoundStore
                                        .getPersonalSounds(MainActivity.this), withAnimations));
                                ((SoundAdapter) mView.getAdapter()).showPersonalSounds(MainActivity.this);
                                break;
                            case 10:
                                if (!((SoundAdapter) mView.getAdapter()).isFavoritesOnly())
                                    ((SoundAdapter) mView.getAdapter()).onlyShowFavorites();
                                else
                                    normalize((SoundAdapter) mView.getAdapter());
                                break;
                            // case 11 is handled by listener, see builder
                            case 12:
                                colorPicker = new ColorPicker(MainActivity.this);
                                colorPicker.setTitle("Current color code: " + colorTitle);
                                colorPicker.setColors(R.array.rainbow);
                                colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                                    @Override
                                    public void onChooseColor(int position, int color) {
                                        if (position == -1)
                                            return;
                                        colorTitle = String.format("#%06X", 0xFFFFFF & color);
                                        SharedPrefs.getInstance().setSelectedColor("color", color);
                                        new Utils().restartActivity(MainActivity.this);
                                    }

                                    @Override
                                    public void onCancel() {
                                        SharedPrefs.getInstance().setSelectedColor("color", SharedPrefs.getInstance().getSelectedColor());
                                        new Utils().restartActivity(MainActivity.this);
                                    }

                                }).setRoundColorButton(true).show();
                                break;
                            case 13:
                                Intent intent = new Intent(MainActivity.this, SupportActivity.class);
                                startActivity(intent);
                                break;
                        }
                        return false;
                    }
                })
                .withSelectedItemByPosition(1)
                .withSavedInstance(instance)
                .build();

        if (new Utils().isGreenMode(MainActivity.this)) {
            ((SoundAdapter) mView.getAdapter()).setShowAnimations(false);
            mDrawer.removeItemByPosition(11);
            mDrawer.removeItemByPosition(13);
        }

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawer.getDrawerLayout(),
                mToolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                if (v != null && mInputManager.isActive()) {
                    mInputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                super.onDrawerOpened(v);
            }
        };

        mDrawer.getDrawerLayout().addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem
                , CompoundButton buttonView, boolean isChecked) {
            switch (mDrawer.getPosition(drawerItem)) {
                case 11:
                    if (((SoundAdapter) mView.getAdapter()).areAnimationsShown()) {
                        withAnimations = false;
                        ((SoundAdapter) mView.getAdapter()).setShowAnimations(false);
                    } else {
                        withAnimations = true;
                        ((SoundAdapter) mView.getAdapter()).setShowAnimations(true);
                    }
                    break;
            }
        }
    };

    private void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        SearchView.SearchAutoComplete searchViewText = (SearchView.SearchAutoComplete) searchView
                .findViewById(R.id.search_src_text);
        new Utils().paintThis(searchViewText);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((SoundAdapter) mView.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

    }

    private void initFAB() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setAlpha(0.8f);
        new Utils().paintThis(fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                soundPlayer.release();
                soundPlayer = new SoundPlayer(MainActivity.this);
            }
        });
    }

    public void normalize(SoundAdapter adapter) {
        switch (adapter.getCategory()) {
            default:
                Log.e(TAG, "Something went completely wrong. Check normalize() or its calls.");
                break;
            case 0:
                adapter.showAllSounds(getApplicationContext());
                break;
            case 1:
                adapter.showAnimalsSounds(getApplicationContext());
                break;
            case 2:
                adapter.showFunnySounds(getApplicationContext());
                break;
            case 3:
                adapter.showGamesSounds(getApplicationContext());
                break;
            case 4:
                adapter.showMoviesSounds(getApplicationContext());
                break;
            case 5:
                adapter.showThugSounds(getApplicationContext());
                break;
            case 6:
                adapter.showNSFWSounds(getApplicationContext());
                break;
            case 7:
                adapter.showPersonalSounds(getApplicationContext());
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            default:
                break;
            case EasyPermissions.SETTINGS_REQ_CODE:
                snackbarReturnedFromActivity();
                try {
                    Thread.sleep(1250);
                    new Utils().restartActivity(MainActivity.this);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case RC_WRITE_SETNGS_PERM_AFTER_M:
                snackbarReturnedFromActivity();
                try {
                    Thread.sleep(1250);
                    new Utils().restartActivity(MainActivity.this);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    // PERMISSIONS
    @AfterPermissionGranted(RC_WRITE_EXST_PERM)
    public void writeExternalStoragePermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Have permission, do the thing!
            snackbarPermissionGranted();
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_exst),
                    RC_WRITE_EXST_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @AfterPermissionGranted(RC_WRITE_SETNGS_PERM)
    public void writeSettingsPermission() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_SETTINGS)) {
            // Have permissions, do the thing!
            snackbarPermissionGranted();
        } else {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_settings),
                    RC_WRITE_SETNGS_PERM, Manifest.permission.WRITE_SETTINGS);
        }
    }

    @AfterPermissionGranted(RC_WRITE_SETNGS_PERM_AFTER_M)
    public void writeSettingsPermissionAfterM() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(MainActivity.this)) {
                final Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())); // don't change string in uri parse
                new AlertDialog.Builder(this, R.style.DialogTheme)
                        .setTitle("Need permission")
                        .setMessage(R.string.rationale_settings)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @SuppressLint("NewApi")
                            public void onClick(DialogInterface dialog, int which) {
                                startActivityForResult(intent, RC_WRITE_SETNGS_PERM_AFTER_M);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied permissions and checked NEVER ASK AGAIN.
        // This will display a dialog directing them to enable the permission in app settings.
        EasyPermissions.checkDeniedPermissionsNeverAskAgain(this,
                getString(R.string.rationale_ask_again),
                R.string.setting, R.string.cancel, null, perms);
    }

    private void snackbarReturnedFromActivity() {
        Snackbar sb = Snackbar
                .make(findViewById(R.id.coordinator),
                        R.string.returned_from_app_settings_to_activity, Snackbar.LENGTH_SHORT);
        View sbv = sb.getView();
        sbv.setBackgroundColor(ContextCompat.getColor(sbv.getContext(), R.color.colorPrimaryDark));
        TextView snackTV = (TextView) sbv.findViewById(android.support.design.R.id.snackbar_text);
        new Utils().paintThis(snackTV);
        sb.show();
    }

    private void snackbarPermissionGranted() {
        Snackbar sb = Snackbar
                .make(findViewById(R.id.coordinator), "Permission Granted", Snackbar.LENGTH_SHORT);
        View sbv = sb.getView();
        sbv.setBackgroundColor(ContextCompat.getColor(sbv.getContext(), R.color.colorPrimaryDark));
        TextView snackTV = (TextView) sbv.findViewById(android.support.design.R.id.snackbar_text);
        new Utils().paintThis(snackTV);
        sb.show();
    }
}
