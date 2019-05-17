package com.example.activitease;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import static com.example.activitease.MainActivity.getCurrentDate;
import static com.example.activitease.MainActivity.myDB;

public class Interest_Fragment extends Fragment {
    // EditText interestName, periodFrequency, basePeriodSpan, activityLength, numNotifications;
    MyGLSurfaceView glSurfaceView;
    static boolean timerRunning;
    private String buttonText;

    private static long START_TIME_MILLIS ;
    private static long mTimeLeftInMillis;
    private static double timeRemaining;
    private static int numIterations;

    private static TextView textViewCountdown, streakCount;
    private static CountDownTimer countDownTimer;

    private static Button delete, editInterestBn, doneBTN, startStop;

    private EditText activityAmount, activityLength, numNotifications;
    private Spinner periodSpanInput;
    private Switch editInterestSwitch;
    private static Context context;

    private int pSpanInput, numNotif;
    private static int count = 0;


    private static String iName;
    static Interest thisInterest;

    static FragmentManager fragmentManage;
    private GLRenderer clockRenderer = new GLRenderer();



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.interest_page, container, false);
        TextView mytextview = view.findViewById(R.id.InterestName);
        String[] periodSpanTypes =
                {"Day", "Week", "Month", "Year"};

        fragmentManage = this.getFragmentManager(); //Begin fragment operations
        glSurfaceView = view.findViewById(R.id.openGLView);
        context = this.getContext();
        // Builds the period Span Spinner.
        periodSpanInput = view.findViewById(R.id.periodSpanInput);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item, periodSpanTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        periodSpanInput.setAdapter(adapter);

        streakCount = view.findViewById(R.id.streakCount);

        doneBTN = view.findViewById(R.id.doneButton);
        startStop = view.findViewById(R.id.startStop);
        if(thisInterest.getActivityActive()) {
          doneBTN.setVisibility(View.VISIBLE);
          startStop.setText("Pause");
          glSurfaceView.onResume();
        }
        else{
            doneBTN.setVisibility(View.GONE);
            startStop.setText("Start Activity");
            glSurfaceView.onPause();
        }

        activityAmount = view.findViewById(R.id.activityAmount);
        activityLength = view.findViewById(R.id.activityLength);
        numNotifications = view.findViewById(R.id.numNotifications);
        textViewCountdown = view.findViewById(R.id.text_view_countdown);
        // Initializes the interest page with set variables from the MainActivity call.
        mytextview.setText(iName);
        activityLength.setText(Integer.toString(thisInterest.getActivityLength()));
        activityAmount.setText(Integer.toString(thisInterest.getPeriodFreq()));
        numNotifications.setText(Integer.toString(thisInterest.getNumNotifications()));

        final int spanInput;
        if(thisInterest.getBasePeriodSpan() == 1)
            spanInput = 0;
        else if(thisInterest.getBasePeriodSpan() == 7)
            spanInput = 1;
        else if(thisInterest.getBasePeriodSpan() == 30)
            spanInput = 2;
        else
            spanInput = 3;

        periodSpanInput.setSelection(spanInput);

        String streakCountString = "Streak Count: " + thisInterest.getStreakCt();
        streakCount.setText(streakCountString);

        updateCountDownText();


        //Stuff past here is for deleting an interest
        // Finds the submit button, and an onClick method submits the data into the database.
        view.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
            @Override
            public void onSwipeLeft() {
                Toast.makeText(getActivity(), "Swiped left", Toast.LENGTH_LONG).show();

                int intrPos = MainActivity.getInterestPos(thisInterest);

                // the last interest in the interest table cannot access a later interest.
                if (intrPos == 0) {
                    Toast.makeText(getActivity(), "No more interests", Toast.LENGTH_LONG).show();
                }
                else {
                    Interest prevInterest = myDB.myDao().getInterests().get(intrPos-1);
                    swipeNextInterest(prevInterest);
                }
            }

            public void onSwipeRight() {
                Toast.makeText(getActivity(), "Swiped right", Toast.LENGTH_LONG).show();

                int intrPos = MainActivity.getInterestPos(thisInterest);

                // the last interest in the interest table cannot access a later interest.
                if (intrPos+1 == myDB.myDao().getInterestCt()) {
                    Toast.makeText(getActivity(), "No more interests", Toast.LENGTH_LONG).show();
                }
                else {
                    Interest nextInterest = myDB.myDao().getInterests().get(intrPos+1);
                    swipeNextInterest(nextInterest);
                }
            }
        });

        final LinearLayout linearLayout = view.findViewById(R.id.linearLayout);
        editInterestSwitch = view.findViewById(R.id.toggleEditInterest);
        editInterestSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    linearLayout.setVisibility(View.VISIBLE);
                else
                    linearLayout.setVisibility(View.GONE);
            }
        });
        return view;
    }

    public void swipeNextInterest(Interest nextInterest) {
        startStop.setText("Start Activity");
        initializeInterest(nextInterest.getInterestName());

        activityLength.setText(Integer.toString(nextInterest.getActivityLength()));
        activityAmount.setText(Integer.toString(nextInterest.getPeriodFreq()));
        numNotifications.setText(Integer.toString(nextInterest.getNumNotifications()));

        int currSpanInput;
        if(nextInterest.getBasePeriodSpan() == 1)
            currSpanInput = 0;
        else if(nextInterest.getBasePeriodSpan() == 7)
            currSpanInput = 1;
        else if(nextInterest.getBasePeriodSpan() == 30)
            currSpanInput = 2;
        else
            currSpanInput = 3;

        periodSpanInput.setSelection(currSpanInput);

        MainActivity.currentInterestName = nextInterest.getInterestName();

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.detach(Interest_Fragment.this);
        fragmentTransaction.attach(Interest_Fragment.this);
        fragmentTransaction.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }
    @Override
    public void onPause(){
        super.onPause();
        glSurfaceView.onPause();
    }

    private static void updateCountDownText() {
        int minutes = (int) mTimeLeftInMillis / 1000 / 60;
        int seconds = (int) mTimeLeftInMillis / 1000 % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        textViewCountdown.setText(timeLeftFormatted);

    }

    public void startTimer() {
        thisInterest.setActivityActive(true);
        MainActivity.myDB.myDao().updateInterest(thisInterest);
        clockRenderer.setTimerRunning(true);
        clockRenderer.setActivityLength(START_TIME_MILLIS);
        String CHANNEL_ID = "Time Remaining";

        Intent intent = new Intent(context, Interest_Fragment.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 1, intent, 0);

        final NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.mipmap.ic_launcher;
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentIntent(pIntent)
                //.setSmallIcon(android.R.drawable.arrow_up_float)setSmallIcon(icon);
                .setSmallIcon(icon)
                .setContentTitle(thisInterest.getInterestName() + " progress")
                .setWhen(System.currentTimeMillis());
        mNM.notify(1, builder.build());
        final int totalTimeMillis = thisInterest.getActivityLength() * 60000;
        builder.setProgress(totalTimeMillis,0, false);
        mNM.notify(1, builder.build());
        countDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                numIterations = calculateIterations(thisInterest.getActivityLength() * 60000, mTimeLeftInMillis);
                count++;
                mNM.notify(1, builder.build());

                if(clockRenderer.getNumIterations() != numIterations)
                {
                    clockRenderer.setNumIterations(numIterations);
                }
                timeRemaining = (float) mTimeLeftInMillis /60000;


                thisInterest.setTimeRemaining(timeRemaining);
                thisInterest.setNumIterations(numIterations);

                myDB.myDao().updateInterest(thisInterest);
                mTimeLeftInMillis = millisUntilFinished;
                long PROGRESS_CURRENT = totalTimeMillis - millisUntilFinished;
                builder.setProgress(totalTimeMillis, (int) PROGRESS_CURRENT, false);

                updateCountDownText();
            }

            @Override
            public void onFinish() {  //When analog timer finishes
                resetTimer();
                doneBTN.setVisibility(View.GONE);
                startStop.setText("Start Activity");
                builder.setContentText("Activity Complete!")
                        .setProgress(0,0,false)
                        .setAutoCancel(true);
                mNM.notify(1, builder.build());


                if (!thisInterest.getStreakCTBool()) {
                    thisInterest.decPeriodRemaining();

                    if (thisInterest.getPeriodRemaining() == 0) {
                        thisInterest.setStreakCTBool(true);
                        thisInterest.setStreakCt(thisInterest.getStreakCt() + 1);
                    }
                }
                clockRenderer.setTimerRunning(false);
                clockRenderer.setNumIterations(0);
                clockRenderer.drawInitialTimer(true);
            }
        }.start();
    }

    public void pauseTimer()
    {
        countDownTimer.cancel();
        clockRenderer.setTimerRunning(false);
        thisInterest.setActivityActive(false);
        MainActivity.myDB.myDao().updateInterest(thisInterest);
    }

    public void resetTimer()
    {
        countDownTimer.cancel();
        thisInterest.setTimeRemaining(thisInterest.getActivityLength());
        thisInterest.setActivityActive(false);
        thisInterest.setNumIterations(0);
        thisInterest.setLastDate(getCurrentDate());
        clockRenderer.setTimerRunning(false);
        clockRenderer.setNumIterations(0);
        clockRenderer.drawInitialTimer(true);
        mTimeLeftInMillis = thisInterest.getActivityLength() * 60000; //Reset static timer variables
        START_TIME_MILLIS = mTimeLeftInMillis;
        updateCountDownText(); //Update the digital clock display
        MainActivity.myDB.myDao().updateInterest(thisInterest);

    }

    public int calculateIterations(long startTimeMillis, long timeLeftMillis)
    {
        int iterationTime = (int) Math.round ((startTimeMillis/91) / 4.00);  //Total time divided by number of indices in clock animation divided by coordinates per indice.
        int iterations = 365 - (int) timeLeftMillis / iterationTime; // Total iterations minus the number of iterations left gets current iteration.
        return iterations; // Return the iterations
    }


    // Getters and setters for the variables that will inflate the interest page.
    public void initializeInterest (String iName) {
        this.iName = iName;
        thisInterest = MainActivity.myDB.myDao().loadInterestByName(iName);
        START_TIME_MILLIS = Math.round(thisInterest.getTimeRemaining() * 60 * 1000);
        clockRenderer.setNumIterations(thisInterest.getNumIterations());
        mTimeLeftInMillis = START_TIME_MILLIS;

        if(thisInterest.getActivityActive())
        {
            numIterations = calculateIterations(thisInterest.getActivityLength() * 60000, mTimeLeftInMillis);
            clockRenderer.setNumIterations(numIterations);
        }

    }
    public void setButtonText(String btnText) {this.buttonText = btnText; }
    public void setpSpanPtr(int pSpanPtr) { this.pSpanInput = pSpanPtr; }
    public void setNumNotif(int numNotif) { this.numNotif = numNotif; }
}
