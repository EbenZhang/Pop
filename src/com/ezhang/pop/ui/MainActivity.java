package com.ezhang.pop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.ezhang.pop.R;
import com.ezhang.pop.core.ICallable;
import com.ezhang.pop.core.LocationService;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.network.RequestManager;
import com.ezhang.pop.settings.AppSettings;
import com.ezhang.pop.settings.SettingsActivity;
import com.ezhang.pop.ui.FuelStateMachine.EmEvent;
import com.ezhang.pop.ui.FuelStateMachine.EmState;
import com.ezhang.pop.utils.PriceDate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends android.support.v7.app.ActionBarActivity implements Observer, IGestureHandler {
    /**
     * There's no need to continue if switched to another activity
     */
    enum EmIndication {
        EmContinue,
        EmStop
    }

    /**
     * Manage requests that query data from remote service.
     */
    private RequestManager m_reqManager;
    private LocationManager m_locationManager;
    private FuelStateMachine m_fuelStateMachine;
    private final ArrayList<FuelDistanceItem> m_fuelInfoList = new ArrayList<FuelDistanceItem>();

    private AppSettings m_settings = null;
    private AlertDialog m_networkAlertDlg = null;
    private AlertDialog m_locationAccessDlg = null;
    private AlertDialog m_gpsOrCustomLocationDlg = null;
    private GestureDetector m_gestureDetector;
    private TabHost m_tabCurDate;

    private ListViewFragment m_listViewFragment;
    private GMapFragment m_gmapFragment;
    private Fragment m_curFragment;

    private ProgressDialog m_progressBar;
    private ProgressDialog m_progressSwitchingView;
    /**
     * The user will be required to choose the location provider type: location service or set the location manually.
     * This variable is to prevent the selection dialog appearing again on again as the app might be resumed several times during the first session.
     */
    private boolean m_locationTypeSelected = false;
    private Toast m_toast;
    private Bundle m_savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState != null) {
            m_savedInstanceState = savedInstanceState;
        }

        InitDateSelector();

        m_reqManager = RequestManager.from(this);

        m_settings = new AppSettings(this);

        m_gestureDetector = new GestureDetector(this, new GestureListener(this));

        InitProgressBar();

        m_progressSwitchingView = new ProgressDialog(this);
        m_progressSwitchingView.setMessage("Changing View");

        m_listViewFragment = new ListViewFragment(new ICallable<Object, Object>() {
            @Override
            public Object Call(Object input) {
                OnListFragmentReady();
                return null;
            }
        }, m_fuelInfoList);

        m_gmapFragment = new GMapFragment(new ICallable<Object, Object>() {
            @Override
            public Object Call(Object input) {
                OnGMapReady();
                return null;
            }
        });
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        m_curFragment = m_listViewFragment;
        if(m_settings.LastViewIsMap()){
            m_curFragment = m_gmapFragment;
        }
        transaction.add(R.id.fragmentPlaceholder, m_curFragment);
        transaction.commit();
    }

    private void InitProgressBar() {
        m_progressBar = new ProgressDialog(this);
        m_progressBar.setCancelable(true);
        m_progressBar.setMessage("Waiting for location info...");
        m_progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_progressBar.setProgress(0);
        m_progressBar.setMax(100);
    }

    private void UpdateProgress(int progress, String text, boolean show){
        m_progressBar.setProgress(progress);
        m_progressBar.setMessage(text);
        if(show){
            m_progressBar.show();
        }else{
            m_progressBar.dismiss();
        }
    }

    private void InitDateSelector() {
        m_tabCurDate = (TabHost) this.findViewById(R.id.tabHost);
        m_tabCurDate.setup();
        m_tabCurDate.addTab(m_tabCurDate.newTabSpec("tabToday").setContent(R.id.tabToday).setIndicator("Today"));
        m_tabCurDate.addTab(m_tabCurDate.newTabSpec("tabTomorrow").setContent(R.id.tabTomorrow).setIndicator("Tomorrow"));
        m_tabCurDate.addTab(m_tabCurDate.newTabSpec("tabYesterday").setContent(R.id.tabYesterday).setIndicator("Yesterday"));

        m_tabCurDate.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                UpdateTabColor(m_tabCurDate);
                if (m_fuelStateMachine == null) {
                    return;
                }

                int curTab = m_tabCurDate.getCurrentTab();
                if (curTab == PriceDate.Tomorrow.ordinal()) {
                    // tomorrow's price is available after 2pm (+8)
                    Calendar cal = Calendar.getInstance();
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    if (hour < 14) {
                        ShowStatusText("Price for tomorrow is only available after 2pm");
                        m_fuelStateMachine.ClearFuelInfo();
                        m_fuelInfoList.clear();
                        m_listViewFragment.UpdateViewForModel();
                        m_gmapFragment.Clear();
                        return;
                    }
                }
                m_fuelStateMachine.SetDateOfFuel(PriceDate.values()[curTab]);
                m_fuelStateMachine.Refresh();
            }
        });
        m_tabCurDate.setCurrentTab(0);
    }

    private void OnGMapReady() {
        m_progressSwitchingView.dismiss();
        m_gmapFragment.UpdateModel(m_fuelInfoList);
    }

    private void OnListFragmentReady() {
        m_progressSwitchingView.dismiss();
    }

    public void OnRefreshClicked(View v) {
        if (this.m_fuelStateMachine != null) {
            this.m_fuelStateMachine.Refresh();
        }
    }

    public void OnSettingsClicked(View v) {
        MarkRefreshRequired();
        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    public void OnChangeLocationClicked(View v) {
        MarkRefreshRequired();
        Intent intent = new Intent(this, CustomLocationActivity.class);
        this.startActivity(intent);
    }

    public void OnHelpClicked(View v) {
        MarkRefreshRequired();
        Intent intent = new Intent(this, HelpActivity.class);
        this.startActivity(intent);
    }

    private void MarkRefreshRequired() {
        m_fuelInfoList.clear();
        m_listViewFragment.UpdateViewForModel();
        if(m_fuelStateMachine != null){
            m_fuelStateMachine.ClearFuelInfo();
        }
    }

    private boolean NeedRefreshFromModel(){
        return m_fuelInfoList.isEmpty();
    }

    public void OnChangeViewClicked(View v) {
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        boolean listViewVisible = m_curFragment == m_listViewFragment;
        if (listViewVisible) {
            m_curFragment = m_gmapFragment;
            m_progressSwitchingView.show();
        } else {
            m_curFragment = m_listViewFragment;
        }
        transaction.replace(R.id.fragmentPlaceholder, m_curFragment);
        boolean switchedToMapView = listViewVisible;
        m_settings.SetLastViewType(switchedToMapView);
        transaction.commit();

        m_gmapFragment.setUserVisibleHint(listViewVisible);
        m_listViewFragment.setUserVisibleHint(!listViewVisible);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (m_fuelStateMachine != null) {
            m_fuelStateMachine.SaveInstanceState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_savedInstanceState = savedInstanceState;
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        m_settings.LoadDiscountSettings(new ICallable<Object, Object>() {
            public Object Call(Object o) {
                OnDiscountInfoLoaded();
                return o;
            }
        });
    }

    private void OnDiscountInfoLoaded() {
        if (m_locationManager == null) {
            m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        PromptEnableNetwork();

        if (PromptLocationTypeSelection() == EmIndication.EmStop) {
            return;
        }

        OnLocationTypeSelected();
    }

    private EmIndication PromptLocationTypeSelection() {
        boolean isFirstRun = this.m_settings.IsFirstRun();
        if (isFirstRun && !m_locationTypeSelected) {
            CreateGPSOrCustomLocationAlertDlg();
            if (!m_gpsOrCustomLocationDlg.isShowing()) {
                m_gpsOrCustomLocationDlg.show();
            }
            return EmIndication.EmStop;
        }
        return EmIndication.EmContinue;
    }

    private void OnLocationTypeSelected() {
        boolean useGPS = m_settings.UseGPSAsLocation();
        if (useGPS) {
            if (PromptEnableLocationService() == EmIndication.EmStop) {
                return;
            }
        }

        if (m_fuelStateMachine == null) {
            CreateStateMachine();
        } else if (NeedRefreshFromModel()) {
            StartStateMachine();
        }
    }

    private void RestoreFromSavedInstance() {
        if (m_savedInstanceState != null && m_fuelStateMachine != null) {
            m_fuelStateMachine.RestoreFromSaveInstanceState(m_savedInstanceState);
            m_savedInstanceState = null;
        }
    }

    private void CreateStateMachine() {
        m_fuelStateMachine = new FuelStateMachine(m_reqManager,
                m_locationManager, m_settings);
        m_fuelStateMachine.addObserver(this);
        StartStateMachine();
        this.update(null, null);
    }

    private void StartStateMachine() {
        UpdateProgress(0, "Waiting For Location Information", true);
        RestoreFromSavedInstance();
        m_fuelStateMachine.ToggleGPS(m_settings.UseGPSAsLocation());
        m_fuelStateMachine.Refresh();
    }

    private void PromptEnableNetwork() {
        ConnectivityManager nInfo = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = nInfo.getActiveNetworkInfo();
        boolean hasNetwork = false;
        if (network != null) {
            hasNetwork = network.isConnectedOrConnecting();
        }
        if (!hasNetwork) {
            CreateNetworkAlertDlg();
            if (!m_networkAlertDlg.isShowing()) {
                m_networkAlertDlg.show();
            }
        }
    }

    private EmIndication PromptEnableLocationService() {
        boolean isGPSEnabled = m_locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkLocationEnabled = m_locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean isOnSimulator = false;//Build.FINGERPRINT.startsWith("generic");

        // network location is not supported by simulator.
        isNetworkLocationEnabled |= isOnSimulator;

        if (!isGPSEnabled || !isNetworkLocationEnabled) {
            CreateLocationAccessAlertDlg();
            if (!m_locationAccessDlg.isShowing()) {
                m_locationAccessDlg.show();
            }
        }

        String provider = LocationService.GetBestProvider(m_locationManager);

        if (provider == null) {
            if (!m_locationAccessDlg.isShowing()) {
                m_locationAccessDlg.show();
            }
            return EmIndication.EmStop;
        }
        return EmIndication.EmContinue;
    }

    private void CreateGPSOrCustomLocationAlertDlg() {
        if (m_gpsOrCustomLocationDlg != null) {
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Select Location Type");

        alertDialog.setCancelable(false);

        // Setting Dialog Message
        alertDialog
                .setMessage("You can use GPS to detect your location or set it yourself.");

        // On pressing Settings button
        alertDialog.setPositiveButton("Use GPS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        m_settings.UseGPSAsLocation(true);
                        m_locationTypeSelected = true;
                        OnLocationTypeSelected();
                    }
                });

        // On pressing Settings button
        alertDialog.setNegativeButton("Set it myself",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        m_settings.UseGPSAsLocation(true);
                        m_locationTypeSelected = true;
                        OnChangeLocationClicked(null);
                    }
                });

        // Showing Alert Message
        m_gpsOrCustomLocationDlg = alertDialog.create();
    }

    private void CreateLocationAccessAlertDlg() {
        if (m_locationAccessDlg != null) {
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Location Access Required");

        // Setting Dialog Message
        alertDialog
                .setMessage("Location Access is disabled. Press go to the settings menu and enable both \n * GPS satellites\n * Wi-Fi & mobile network");

        // On pressing Settings button
        alertDialog.setPositiveButton("Go",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                });

        // Showing Alert Message
        m_locationAccessDlg = alertDialog.create();
    }

    private void CreateNetworkAlertDlg() {
        if (m_networkAlertDlg != null) {
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialogBuilder.setTitle("Network Access Required");

        // Setting Dialog Message
        alertDialogBuilder
                .setMessage("Network is not enabled. Please either enable Wi-Fi or Mobile Network Data");

        // On pressing Settings button
        alertDialogBuilder.setPositiveButton("Wi-Fi",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_WIFI_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                });

        // On pressing Settings button
        alertDialogBuilder.setNegativeButton("MobileNetwork",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_DATA_ROAMING_SETTINGS);
                        // intent.setClassName("com.android.phone",
                        // "com.android.phone.Settings");
                        MainActivity.this.startActivity(intent);
                    }
                });

        // Showing Alert Message
        m_networkAlertDlg = alertDialogBuilder.create();
    }

    /**
     * Handles the notifications from StateMachine, then update the UI accordingly.
     *
     * @param arg0
     * @param arg1
     */
    @Override
    public void update(Observable arg0, Object arg1) {
        if (this.m_fuelStateMachine.GetCurState() == EmState.DistanceReceived) {
            OnDistanceInfoReceived(this.m_fuelStateMachine.m_fuelDistanceItems);
        }
        if (this.m_fuelStateMachine.GetCurState() == EmState.SuburbReceived) {
            OnSuburbReceived();
        }

        if (this.m_fuelStateMachine.GetCurState() == EmState.FuelInfoReceived) {
            OnFuelInfoReceived();
        }

        if (this.m_fuelStateMachine.GetCurState() == EmState.Start) {
            OnStateMachineStarted();
        }

        if (this.m_fuelStateMachine.GetCurState() == EmState.GeoLocationReceived) {
            OnLocationReceived();
        }

        if (this.m_fuelStateMachine.GetCurState() == EmState.Timeout) {
            OnTimeout();
        }
    }

    /**
     * Handles timeout event from the StateMachine.
     */
    private void OnTimeout() {
        if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.GeoLocationEvent) {
            UpdateProgress(0, "", false);
            ShowStatusText("Unable to get location. Probably because location access is disabled.");
            HideCurrentAddress();
        } else if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.SuburbEvent) {
            UpdateProgress(0, "", false);
            ShowStatusText("Unable to get suburb. Probably because network is disabled.");
        } else if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.FuelInfoEvent) {
            UpdateProgress(0, "", false);
            ShowStatusText("Unable to get fuel info. Probably because network is disabled.");
        }
    }

    private void OnStateMachineStarted() {
        UpdateProgress(0, "Waiting For Location Information", true);
        HideCurrentAddress();
    }

    private void OnLocationReceived() {
        UpdateProgress(25, "Location Information Received", true);
        HideCurrentAddress();
    }

    private void OnFuelInfoReceived() {
        UpdateProgress(75, "Fuel Price Info Received", true);
        ShowCurrentAddr();
        this.m_fuelInfoList.clear();
        m_listViewFragment.UpdateViewForModel();
        m_gmapFragment.Clear();
    }

    private void OnSuburbReceived() {
        UpdateProgress(50, "Suburb Received: "
                + this.m_fuelStateMachine.m_suburb, true);
        ShowCurrentAddr();
        this.m_fuelInfoList.clear();
        UpdateTabColor(m_tabCurDate);
    }

    private void OnDistanceInfoReceived(List<FuelDistanceItem> items) {
        ShowCurrentAddr();
        this.m_fuelInfoList.clear();
        this.m_fuelInfoList
                .addAll(items);
        Collections.sort(m_fuelInfoList, FuelDistanceItem.GetComparer());
        m_listViewFragment.UpdateViewForModel();
        m_gmapFragment.UpdateModel(m_fuelInfoList);

        if (this.m_fuelInfoList.size() == 0) {
            ShowStatusText("Unfortunately, no fuel info was found");
        }
        UpdateProgress(0, "", false);
    }

    private void ShowCurrentAddr() {
        if (this.m_fuelStateMachine.m_suburb != null
                && !m_fuelStateMachine.m_suburb.equals("")) {
            int indexOfSuburb = this.m_fuelStateMachine.m_address
                    .indexOf(this.m_fuelStateMachine.m_suburb);
            String addrWithoutSuburb = this.m_fuelStateMachine.m_address;
            if (indexOfSuburb > 0) {
                addrWithoutSuburb = this.m_fuelStateMachine.m_address
                        .substring(0, indexOfSuburb);
            }
            TextView v = ((TextView) this.findViewById(R.id.curAddressText));
            String addrText = String.format(Locale.ENGLISH,
                    "You're at:%s%s.Touch here to change if incorrect.",
                    addrWithoutSuburb, this.m_fuelStateMachine.m_suburb);
            v.setText(addrText);

            View curAddr = this.findViewById(R.id.layoutCurAddr);
            curAddr.setVisibility(View.VISIBLE);
        }
    }

    private void HideCurrentAddress() {
        View curAddr = this.findViewById(R.id.layoutCurAddr);
        curAddr.setVisibility(View.GONE);
    }

    public void OnShareWithFriendClicked(View v) {
        String url = String.format(
                "https://play.google.com/store/apps/details?id=%s",
                getApplicationContext().getPackageName());
        String content = "Hey, I'm using WaFuelFuel to help find the cheapest and nearest petrol station in Western Australia.\nCheck it out in GooglePlay: "
                + url;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                OnSettingsClicked(null);
                return true;
            case R.id.share_with_friend_menu:
                this.OnShareWithFriendClicked(null);
                return true;
            case R.id.action_change_view:
                String mapView = getResources().getString(R.string.map_view);
                String curTitle = item.getTitle().toString();
                if(curTitle.equals(mapView)){
                    item.setTitle(getResources().getString(R.string.list_view));
                    item.setIcon(getResources().getDrawable(R.drawable.collections_view_as_list));
                }else{
                    item.setTitle(getResources().getString(R.string.map_view));
                    item.setIcon(getResources().getDrawable(R.drawable.location_map));
                }
                this.OnChangeViewClicked(null);
                return true;
            case R.id.help_menu_item:
                OnHelpClicked(null);
                return true;
            case R.id.refresh_menu_item:
                OnRefreshClicked(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void ShowStatusText(String text) {
        if (m_toast == null) {
            m_toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        } else {
            m_toast.setText(text);
        }
        if (m_fuelStateMachine != null && m_fuelStateMachine.IsPaused()) {
            m_toast.cancel();
            return;
        }
        m_toast.show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean consumed = m_gestureDetector.onTouchEvent(event);
        if (!consumed) {
            return super.dispatchTouchEvent(event);
        }
        return consumed;
    }

    @Override
    public void GestureToLeft() {
        int curTab = m_tabCurDate.getCurrentTab();
        if (curTab < 2) {
            m_tabCurDate.setCurrentTab(curTab + 1);
        }
    }

    @Override
    public void GestureToRight() {
        int curTab = m_tabCurDate.getCurrentTab();
        if (curTab > 0) {
            m_tabCurDate.setCurrentTab(curTab - 1);
        }
    }

    @Override
    protected void onPause() {
        if (m_fuelStateMachine != null) {
            m_fuelStateMachine.Pause();
        }
        super.onPause();
    }

    //Change The Backgournd Color of Tabs
    public void UpdateTabColor(TabHost tabhost) {

        int cur = tabhost.getCurrentTab();
        for (int i = 0; i < tabhost.getTabWidget().getChildCount(); i++) {
            if (i == cur) {
                continue;
            }
            tabhost.getTabWidget().getChildAt(i).setBackgroundColor(Color.WHITE); //unselected
        }
        tabhost.getTabWidget().getChildAt(cur).setBackgroundColor(Color.LTGRAY);
    }
}
