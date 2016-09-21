package githubzzx752904457.com.rangesliderview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import githubzzx752904457.com.rangesliderview.widgets.RangeSliderView;

public class MainActivity extends AppCompatActivity {
    private RangeSliderView rsv;
    private TextView amount;
    private TextView tvReturn;

    private String[] amountStr = {"100.00", "10,000.00", "30,000.00", "50,000.00", "100,000.00"};
    private String[] returnStr = {"0.99", "98.81", "296.42", "494.03", "988.05"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rsv = (RangeSliderView) findViewById(R.id.rsv);

        amount = (TextView) findViewById(R.id.amount);
        tvReturn = (TextView) findViewById(R.id.tv_return);

        rsv.setInitialIndex(0);
        rsv.setIcon(R.mipmap.gold_coin);
        rsv.setOnSlideListener(new RangeSliderView.OnSlideListener() {
            @Override
            public void onSlide(int index) {
                amount.setText(amountStr[index]);
                tvReturn.setText(returnStr[index]);
            }
        });
    }
}
