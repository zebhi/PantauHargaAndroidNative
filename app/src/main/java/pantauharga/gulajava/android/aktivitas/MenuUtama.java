package pantauharga.gulajava.android.aktivitas;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import pantauharga.gulajava.android.Konstan;
import pantauharga.gulajava.android.R;
import pantauharga.gulajava.android.adapters.AdapterTabPager;
import pantauharga.gulajava.android.databases.RMJsonData;
import pantauharga.gulajava.android.databases.RMLogin;
import pantauharga.gulajava.android.fragments.FragmentListHarga;
import pantauharga.gulajava.android.fragments.FragmentPetaHarga;
import pantauharga.gulajava.android.internets.Apis;
import pantauharga.gulajava.android.internets.GsonRekuestArray;
import pantauharga.gulajava.android.internets.Volleys;
import pantauharga.gulajava.android.messagebus.MessageBusAktAkt;
import pantauharga.gulajava.android.modelgson.HargaKomoditasItem;
import pantauharga.gulajava.android.modelgson.KomoditasItem;
import pantauharga.gulajava.android.modelgsonkirim.HargaKomoditasCek;
import pantauharga.gulajava.android.parsers.Parseran;

/**
 * Created by Gulajava Ministudio on 11/5/15.
 */
public class MenuUtama extends BaseActivityLocation {

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    private ActionBar mActionBar;

    @Bind(R.id.layoutdrawer)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.nav_view)
    NavigationView mNavigationView;
    @Bind(R.id.viewpager)
    ViewPager mViewPager;
    @Bind(R.id.tabsbars)
    TabLayout mTabLayout;

    private ImageView gambarheaders;
    private View view_navigasi;

    private AdapterTabPager mAdapterTabPager;

    private Parseran mParseran;

    private boolean isAktJalan = false;
    private boolean isDrawerBuka = false;

    private FragmentListHarga mFragmentListHarga;
    private FragmentPetaHarga mFragmentPetaHarga;


    //realm db login
    private Realm mRealm;
    private RealmQuery<RMLogin> mRealmQueryLogin;
    private RealmResults<RMLogin> mRealmResultsLogin;
    private String username = "";
    private boolean isLogin = false;

    private RealmQuery<RMJsonData> mRealmQueryJson;
    private RealmResults<RMJsonData> mRealmResultsJson;
    private String jsonKomoditas = "";
    private List<KomoditasItem> listKomoditasItem;
    private List<String> listStrKomoditasItem;

    private String namakomoditas = "";
    private String radiuskm = "50";

    //lokasi pengguna
    private Location mLocationPengguna;
    private double longitudepengguna = 0;
    private double latitudepengguna = 0;
    private boolean isInternet = false;

    private boolean isDataAwalDiambil = false;

    //ambil dari server
    private ProgressDialog mProgressDialog;

    Runnable jedakirimdata = new Runnable() {
        @Override
        public void run() {

            //ambil db json komoditas
            ambilDbJson();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_menumain);
        ButterKnife.bind(MenuUtama.this);
        munculMenuAction(MenuUtama.this);

        isAktJalan = true;

        if (mToolbar != null) {
            MenuUtama.this.setSupportActionBar(mToolbar);
        }

        mActionBar = MenuUtama.this.getSupportActionBar();
        assert mActionBar != null;
        mActionBar.setTitle(R.string.app_name);
        mActionBar.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        mFragmentListHarga = new FragmentListHarga();
        mFragmentPetaHarga = new FragmentPetaHarga();

        mParseran = new Parseran(MenuUtama.this);
        listKomoditasItem = new ArrayList<>();
        listStrKomoditasItem = new ArrayList<>();

        //ambil acak gambar headers
        int kodegambar = mParseran.acakGambar();

        view_navigasi = mNavigationView.inflateHeaderView(R.layout.nav_header);
        gambarheaders = (ImageView) view_navigasi.findViewById(R.id.gambarheader);
        Glide.with(MenuUtama.this).load(kodegambar).into(gambarheaders);

        setelDrawerView(mViewPager);
        mNavigationView.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mNavigationView.setItemTextColor(MenuUtama.this.getResources().getColorStateList(R.color.menu_teks_color_selector));

        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabTextColors(MenuUtama.this.getResources().getColorStateList(R.color.tab_teks_color_selector));
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

        mDrawerLayout.setDrawerListener(mDrawerListener);



        //ambil data realm login
        mRealm = Realm.getInstance(MenuUtama.this);
        ambilDbLogin();

        Handler handler = new Handler();
        handler.postDelayed(jedakirimdata, 1000);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(MenuUtama.this)) {
            EventBus.getDefault().register(MenuUtama.this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (EventBus.getDefault().isRegistered(MenuUtama.this)) {
            EventBus.getDefault().unregister(MenuUtama.this);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        isAktJalan = false;
        mRealm.close();

        hentikanListenerLokasi();

        Volleys.getInstance(MenuUtama.this).cancelPendingRequestsNoTag();
        Volleys.getInstance(MenuUtama.this).clearVolleyCache();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuUtama.this.getMenuInflater().inflate(R.menu.menu_utama, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:

                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.action_segarkan:

                //cek status lokasi pengguna
                if (!isDataAwalDiambil) {
                    cekInternetKirimServer();
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * TERIMA PESAN DARI LISTENER LOKASI PENGGUNA
     **/
    public void onEvent(MessageBusAktAkt messageBusAktAkt) {

        int kodeperintah = messageBusAktAkt.getKode();
        switch (kodeperintah) {

            case Konstan.KODE_LOKASIAWAL:

                ambilLokasiPenggunaAwal();
                break;

            case Konstan.KODE_LOKASIBARU:

                ambilLokasiPenggunaUpdate();
                break;
        }
    }


    //AMBIL LOKASI AWAL PENGGUNA
    private void ambilLokasiPenggunaAwal() {

        MenuUtama.this.latitudepengguna = getLatitudesaya();
        MenuUtama.this.longitudepengguna = getLongitudesaya();

        isInternet = isInternet();
        mLocationPengguna = getLokasisaya();

        Log.w("LOKASI AWAL", "lokasi awal pengguna " + latitudepengguna + " , " + longitudepengguna);

        //ambil data dari server untuk lokasi gps dan marker
        cekInternetKirimServer();
    }


    //AMBIL LOKASI UPDATE DARI PENGGUNA
    private void ambilLokasiPenggunaUpdate() {

        MenuUtama.this.latitudepengguna = getLatitudesaya();
        MenuUtama.this.longitudepengguna = getLongitudesaya();

        cekInternet();
        isInternet = isInternet();
        mLocationPengguna = getLokasisaya();

        Log.w("LOKASI UPDATE", "lokasi update pengguna " + latitudepengguna + " , " + longitudepengguna);
    }


    //SETEL NAVIGATION DRAWER
    private void setelDrawerView(ViewPager pagers) {

        mAdapterTabPager = new AdapterTabPager(MenuUtama.this.getSupportFragmentManager());
        mAdapterTabPager.addFragment(mFragmentListHarga, "CEK HARGA");
        mAdapterTabPager.addFragment(mFragmentPetaHarga, "PETA HARGA");

        pagers.setAdapter(mAdapterTabPager);
        pagers.addOnPageChangeListener(mOnPageChangeListener);

    }


    ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            //indikator tab dipilih di atas
            TabLayout.Tab tab = mTabLayout.getTabAt(position);
            if (tab != null) {
                tab.select();
            }

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };


    //SETEL DRAWER VIEW
    NavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            return false;
        }
    };


    DrawerLayout.DrawerListener mDrawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(View drawerView) {

            isDrawerBuka = true;
        }

        @Override
        public void onDrawerClosed(View drawerView) {

            isDrawerBuka = false;
        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };


    //AMBIL DATABASE REALM LOGIN
    private void ambilDbLogin() {

        mRealmQueryLogin = mRealm.where(RMLogin.class);
        mRealmResultsLogin = mRealmQueryLogin.findAll();

        if (mRealmResultsLogin.size() > 0) {

            RMLogin rmLogin = mRealmResultsLogin.first();
            username = rmLogin.getUsername();

            isLogin = username.length() > 3;
        } else {
            isLogin = false;
        }
    }


    //AMBIL DATABASE JSON KOMODITAS DAN DI PARSE
    private void ambilDbJson() {


        mRealmQueryJson = mRealm.where(RMJsonData.class);
        mRealmResultsJson = mRealmQueryJson.findAll();

        if (mRealmResultsJson.size() > 0) {

            RMJsonData rmJsonData = mRealmResultsJson.first();
            jsonKomoditas = rmJsonData.getJsonkomoditas();

            if (jsonKomoditas.length() > 5) {

                //parse json
                parseJsonKomoditas();
            } else {
                munculSnackbar(R.string.toastgagaldata);
            }

        } else {
            munculSnackbar(R.string.toastgagaldata);
        }
    }


    private void tampilProgressDialog(String pesan) {

        //progress dialogs
        mProgressDialog = new ProgressDialog(MenuUtama.this);
        mProgressDialog.setMessage(pesan);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

    }


    /** ================================================================================================== **/
    /** =============================== AMBIL DATA DARI SERVER =========================================== **/
    /**
     * ==================================================================================================
     **/

    //PARSE DAFTAR KOMODITAS
    private void parseJsonKomoditas() {

        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                listKomoditasItem = mParseran.parseJsonKomoditas(jsonKomoditas);

                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {

                if (listKomoditasItem != null) {

                    listStrKomoditasItem = mParseran.getArrStringNamaKomoditas();

                    namakomoditas = listStrKomoditasItem.get(0);

                    //ambil data dari server
                    //cek permisission
                    cekPermissionLokasi();
                } else {
                    //gagal parse
                    munculSnackbar(R.string.toastgagaldata);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }


    private void cekInternetKirimServer() {

        if (isInternet) {
            isDataAwalDiambil = true;
            susunJsonKirimServer();
        } else {
            munculSnackbar(R.string.toastnointernet);
            isDataAwalDiambil = false;
        }
    }


    //AMBIL DATA DARI SERVER
    private void susunJsonKirimServer() {

        //set segarkan di fragment
        tampilProgressDialog("Mengambil data dari server...");

        //{"name":"Bawang Merah Kering","radius": "100","lat":-6.217,"lng":106.9}
        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {

                HargaKomoditasCek hargaKomoditasCek = new HargaKomoditasCek();
                hargaKomoditasCek.setName(namakomoditas);
                hargaKomoditasCek.setRadius(radiuskm);
                hargaKomoditasCek.setLat("" + latitudepengguna);
                hargaKomoditasCek.setLng("" + longitudepengguna);

                return mParseran.konversiPojoJsonCekKomoditas(hargaKomoditasCek);
            }
        }).continueWith(new Continuation<String, Object>() {
            @Override
            public Object then(Task<String> task) throws Exception {

                String hasiljsons = task.getResult();
                Log.w("HASIL PARSE CEK", "HASIL PARSE JSON CEK " + hasiljsons);

                //kirim ke server
                kirimJsonKeServer(hasiljsons);

                return null;
            }
        });
    }


    //AMBIL DATA KOMODITAS DAN CEK KE SERVER
    private void kirimJsonKeServer(String jsonbody) {

        String urls = Apis.getLinkHargaKomoditas();
        Map<String, String> headers = new HashMap<>();
        Map<String, String> parameters = new HashMap<>();
        headers.put(Konstan.TAG_HEADERCONTENTIPE, Konstan.HEADER_JSONTYPE);

        GsonRekuestArray<HargaKomoditasItem> gsonRekuestArray = Apis.postRequestHargaKomoditasSekitar(
                urls,
                headers,
                parameters,
                jsonbody,
                new Response.Listener<List<HargaKomoditasItem>>() {
                    @Override
                    public void onResponse(List<HargaKomoditasItem> response) {

                        Log.w("SUKSES", "SUKSES");

                        if (isAktJalan) {
                            cekHasilRespons(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.w("GAGAL", "GAGAL");

                        if (isAktJalan) {
                            cekHasilRespons(null);
                        }
                    }
                }
        );


        Volleys.getInstance(MenuUtama.this).addToRequestQueue(gsonRekuestArray);
    }


    //CEK HASIL RESPON
    private void cekHasilRespons(List<HargaKomoditasItem> listHargaServer) {

        if (listHargaServer != null && listHargaServer.size() > 0) {

            //kirim ke fragments


        } else {

            munculSnackbar(R.string.toastgagaldata);
        }

        isDataAwalDiambil = false;
        mProgressDialog.dismiss();
    }


    //MENAMPILKAN MENU ACTION BAR
    private void munculMenuAction(Context context) {

        try {
            ViewConfiguration config = ViewConfiguration.get(context);
            Field menuKey = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

            if (menuKey != null) {
                menuKey.setAccessible(true);
                menuKey.setBoolean(config, false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //MUNCUL SNACKBAR
    private void munculSnackbar(int resPesan) {

        Snackbar.make(mViewPager, resPesan, Snackbar.LENGTH_LONG).setAction("OK", listenersnackbar)
                .setActionTextColor(MenuUtama.this.getResources().getColor(R.color.kuning_indikator)).show();
    }

    View.OnClickListener listenersnackbar = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };


}
