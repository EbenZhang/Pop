package com.ezhang.pop.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ezhang.pop.R;
import com.ezhang.pop.settings.SettingsActivity;

/**
 * Created by eben on 13-6-10.
 */
public class HelpActivity extends Activity {
    private final static String[][][] HELP_TABLE = new String[][][]{
            {{"Custom Location", "You can customise your location by touching the current address being shown on the top\nOr touch here to change", CustomLocationActivity.class.getName()}},
            {{"Voucher", "You can change your vouchers in settings\nOr touch here to change", SettingsActivity.class.getName()}},
            {{"Price of Tomorrow", "Slide left/right to see the price of yesterday/tomorrow", ""}},
            {{"Map&Navigation", "Touch the price item to show map/navigation", ""}},
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity);
        ViewGroup layout = (ViewGroup) this.findViewById(R.id.idHelpActivity);
        int index = 0;
        CreateItem(layout, index);
        ++index;
        while (index < HELP_TABLE.length)
        {
            layout.addView(CreateSpace());
            CreateItem(layout, index);
            ++index;
        }
    }

    private void CreateItem(ViewGroup layout, final int index) {
        layout.addView(CreateTitle(HELP_TABLE[index][0][0]));
        layout.addView(CreateSeparator());
        View text = CreateHelpText(HELP_TABLE[index][0][1]);
        layout.addView(text);
        if(!HELP_TABLE[index][0][2].equals("")){
            text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setClassName(getApplicationContext().getPackageName(), HELP_TABLE[index][0][2]);
                    HelpActivity.this.startActivity(intent);
                }
            });
        }
    }

    private View CreateHelpText(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextAppearance(this, android.R.attr.textAppearanceLarge);
        return t;
    }

    private View CreateTitle(String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextAppearance(this, android.R.attr.textAppearanceSmall);
        t.setTypeface(null, Typeface.BOLD_ITALIC);
        return t;
    }

    private View CreateSeparator() {
        View v = new View(this);
        ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        v.setLayoutParams(param);
        v.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        return v;
    }

    private View CreateSpace() {
        View v = new View(this);
        ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6);
        v.setLayoutParams(param);
        return v;
    }
}