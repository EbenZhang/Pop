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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.Toast;

import com.ezhang.pop.R;
import com.ezhang.pop.core.ICallable;
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
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends android.support.v7.app.ActionBarActivity implements Observer, IGestureHandler {
    /**
     * Manage requests that query data from remote service.
     */
    private RequestManager m_reqManager;
    private LocationManager m_locationManager;
    private FuelStateMachine m_fuelStateMachine;
    private final ArrayList<FuelDistanceItem> m_fuelInfoList = new ArrayList<FuelDistanceItem>();

    private AppSettings m_settings = null;
    private AlertDialog m_networkAlertDlg = null;
    private GestureDetector m_gestureDetector;
    private TabHost m_tabCurDate;

    private static ListViewFragment m_listViewFragment;
    private static GMapFragment m_gmapFragment;
    private static Fragment m_curFragment;

    private ProgressDialog m_progressBar;
    private ProgressDialog m_progressSwitchingView;

    private Spinner m_curAddrSpinner;

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

        m_curAddrSpinner = (Spinner)findViewById(R.id.cur_addr_spinner);
        m_curAddrSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                if (pos != -1) {
                    String selectedAddr = adapterView.getItemAtPosition(pos).toString();
                    if (selectedAddr.equals(getResources().getString(R.string.manage_addresses))) {
                        OnChangeLocationClicked();
                    }else{
                        List<String> addresses = m_settings.GetHistoryLocations();
                        int index = addresses.indexOf(selectedAddr);
                        addresses.remove(index);
                        addresses.add(0, selectedAddr);
                        m_settings.SaveLocations(addresses);
                        MarkRefreshRequired();
                        int curTab = m_tabCurDate.getCurrentTab();
                        if (curTab == PriceDate.Tomorrow.ordinal()) {
                            if (!CheckTimeForTomorrow()){
                                return;
                            }
                        }
                        m_fuelStateMachine.Refresh();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        InitDateSelector();

        m_reqManager = RequestManager.from(this);

        m_settings = new AppSettings(this);

        m_gestureDetector = new GestureDetector(this, new GestureListener(this));

        InitProgressBar();

        m_progressSwitchingView = new ProgressDialog(this);
        m_progressSwitchingView.setMessage("Changing View");
        if (m_listViewFragment == null) {
            m_listViewFragment = new ListViewFragment(new ICallable<Object, Object>() {
                @Override
                public Object Call(Object input) {
                    OnListFragmentReady();
                    return null;
                }
            }, m_fuelInfoList);
        }

        if(m_gmapFragment == null) {
            m_gmapFragment = new GMapFragment(new ICallable<Object, Object>() {
                @Override
                public Object Call(Object input) {
                    OnGMapReady();
                    return null;
                }
            }, m_settings);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        m_curFragment = m_listViewFragment;
        if (m_settings.LastViewIsMap()) {
            m_curFragment = m_gmapFragment;
        }
        transaction.replace(R.id.fragmentPlaceholder, m_curFragment);
        transaction.commit();
    }

    private void BindHistoryAddr() {
        List<String> historyAddrs = m_settings.GetHistoryLocations();
        historyAddrs.add(getResources().getString(R.string.manage_addresses));
        if(historyAddrs.size() != 0) {
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, historyAddrs);
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            m_curAddrSpinner.setAdapter(dataAdapter);
        }
    }

    private void InitProgressBar() {
        m_progressBar = new ProgressDialog(this);
        m_progressBar.setCancelable(true);
        m_progressBar.setMessage("Waiting For Fuel Info...");
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
                MarkRefreshRequired();
                int curTab = m_tabCurDate.getCurrentTab();
                m_fuelStateMachine.SetDateOfFuel(PriceDate.values()[curTab]);
                if (curTab == PriceDate.Tomorrow.ordinal()) {
                    if (!CheckTimeForTomorrow()) return;
                }
                m_fuelStateMachine.Refresh();
            }
        });
        m_tabCurDate.setCurrentTab(0);
    }

    private boolean CheckTimeForTomorrow() {
        // tomorrow's price is available after 2pm (+8)
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 14) {
            ShowStatusText("Price for tomorrow is only available after 2pm");
            m_fuelStateMachine.ClearFuelInfo();
            m_fuelInfoList.clear();
            m_listViewFragment.UpdateViewForModel();
            m_gmapFragment.Clear();
            return false;
        }
        return true;
    }

    private void OnGMapReady() {
        m_progressSwitchingView.dismiss();
        m_gmapFragment.UpdateModel(m_fuelInfoList);
        m_progressSwitchingView.dismiss();
    }

    private void OnListFragmentReady() {
        m_progressSwitchingView.dismiss();
        m_listViewFragment.UpdateViewForModel();
    }

    private void OnRefreshClicked() {
        if (this.m_fuelStateMachine != null) {
            this.m_fuelStateMachine.Refresh();
        }
    }

    private void OnSettingsClicked() {
        MarkRefreshRequired();
        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    private void OnChangeLocationClicked() {
        MarkRefreshRequired();
        Intent intent = new Intent(this, CustomLocationActivity.class);
        this.startActivity(intent);
    }

    private void OnHelpClicked() {
        MarkRefreshRequired();
        Intent intent = new Intent(this, HelpActivity.class);
        this.startActivity(intent);
    }

    private void MarkRefreshRequired() {
        m_fuelInfoList.clear();
        m_listViewFragment.UpdateViewForModel();
        m_gmapFragment.Clear();
        if(m_fuelStateMachine != null){
            m_fuelStateMachine.ClearFuelInfo();
        }
    }

    private boolean NeedRefreshFromModel(){
        return m_fuelInfoList.isEmpty();
    }

    private void OnChangeViewClicked() {
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
        MenuItem item = menu.findItem(R.id.change_view_menu_item);
        if (m_settings.LastViewIsMap()) {
            item.setTitle(getResources().getString(R.string.list_view));
            item.setIcon(getResources().getDrawable(R.drawable.collections_view_as_list));
        } else {
            item.setTitle(getResources().getString(R.string.map_view));
            item.setIcon(getResources().getDrawable(R.drawable.location_map));
        }
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

        if (m_settings.GetHistoryLocations().size() == 0){
            OnChangeLocationClicked();
            return;
        }

        BindHistoryAddr();

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
        int curTab = m_tabCurDate.getCurrentTab();
        if (curTab == PriceDate.Tomorrow.ordinal()) {
            if (!CheckTimeForTomorrow()){
                return;
            }
        }
        UpdateProgress(0, "Waiting For Fuel Info...", true);
        RestoreFromSavedInstance();
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

        if (this.m_fuelStateMachine.GetCurState() == EmState.FuelInfoReceived) {
            OnFuelInfoReceived();
        }

        if (this.m_fuelStateMachine.GetCurState() == EmState.Start) {
            OnStateMachineStarted();
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
        } else if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.SuburbEvent) {
            UpdateProgress(0, "", false);
            ShowStatusText("Unable to get suburb. Probably because network is disabled.");
        } else if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.FuelInfoEvent) {
            UpdateProgress(0, "", false);
            ShowStatusText("Unable to get fuel info. Probably because network is disabled.");
        }
    }

    private void OnStateMachineStarted() {
        UpdateProgress(0, "Waiting For Fuel Info...", true);
    }

    private void OnFuelInfoReceived() {
        UpdateTabColor(m_tabCurDate);
        UpdateProgress(50, "Fuel Info Received", true);
        this.m_fuelInfoList.clear();
        m_listViewFragment.UpdateViewForModel();
        m_gmapFragment.Clear();
    }

    private void OnDistanceInfoReceived(List<FuelDistanceItem> items) {
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

    private void OnShareWithFriendClicked() {
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
            case R.id.settings_menu_item:
                OnSettingsClicked();
                return true;
            case R.id.share_menu_item:
                this.OnShareWithFriendClicked();
                return true;
            case R.id.change_view_menu_item:
                String mapView = getResources().getString(R.string.map_view);
                String curTitle = item.getTitle().toString();
                if(curTitle.equals(mapView)){
                    item.setTitle(getResources().getString(R.string.list_view));
                    item.setIcon(getResources().getDrawable(R.drawable.collections_view_as_list));
                }else{
                    item.setTitle(getResources().getString(R.string.map_view));
                    item.setIcon(getResources().getDrawable(R.drawable.location_map));
                }
                this.OnChangeViewClicked();
                return true;
            case R.id.help_menu_item:
                OnHelpClicked();
                return true;
            case R.id.refresh_menu_item:
                OnRefreshClicked();
                return true;
            case R.id.manage_addr_menu_item:
                OnChangeLocationClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void OnDonateClicked() {
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
