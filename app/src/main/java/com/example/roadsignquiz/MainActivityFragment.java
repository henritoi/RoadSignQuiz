package com.example.roadsignquiz;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "MainActivityFragment";

    private static final int SIGNS_IN_QUIZ = 10;
    private static final int ANIMATION_DURATION = 500;

    private List<String> fileNameList, quizSignsList;
    private Set<String> signGroupSet;
    private String correctAnswer;
    private int totalGuesses, correctAnswers, guessRows;
    static int staticTotalGuesses;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTextView, answerTextView;
    private ImageView signImageView;
    private LinearLayout[] guessLinearLayouts;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "MainActivityFragment: Created");
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        fileNameList = new ArrayList<>();
        quizSignsList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        // ladataan ravistus animaatio, jota käytetään oikeille vastauksille
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // toistetaan kolmesti

        // liitetään muuttujat käyttöliittymän komponentteihin
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        signImageView = (ImageView) view.findViewById(R.id.signImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // asetetaan painikkeille kuuntelijat
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // asetetaan kysymysten määrän teksti
        questionNumberTextView.setText(getString(R.string.question, 1, SIGNS_IN_QUIZ));

        return view; // palautetaan näkymä näytettäväksi
    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {

        String choises = sharedPreferences.getString(MainActivity.CHOICES, "4");
        Log.d(TAG, "Choises update: " + choises);
        guessRows  = Integer.parseInt(choises) / 2;

        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    public void updateSignGroup(SharedPreferences sharedPreferences) {
        signGroupSet = sharedPreferences.getStringSet(MainActivity.SIGN_GROUPS, null);
    }

    public void resetQuiz() {
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();

        try {
            for (String region : signGroupSet) {
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        }catch(IOException e) {
            Log.e(TAG, "resetQuiz: Error loading image file names", e);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizSignsList.clear();

        int signCounter = 1;
        int numberOfSigns = fileNameList.size();

        while (signCounter <= SIGNS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfSigns);
            String filename = fileNameList.get(randomIndex);
            if(!quizSignsList.contains(filename)) {
                quizSignsList.add(filename);
                ++signCounter;
            }
        }

        loadNextSign();
    }

    private void loadNextSign() {
        String nextImage = quizSignsList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), SIGNS_IN_QUIZ));

        String signGroup = nextImage.substring(0, nextImage.indexOf('-'));
        AssetManager assets = getActivity().getAssets();

        try(InputStream stream = assets.open(signGroup + "/" + nextImage + ".png")) {
            Drawable sign = Drawable.createFromStream(stream, nextImage);
            Log.d(TAG, "loadNextSign: " + sign);
            signImageView.setImageDrawable(sign);
            animate(false);
        }catch (IOException e) {
            Log.e(TAG, "loadNextFlag: Error loading" + nextImage, e);
        }

        Collections.shuffle(fileNameList);

        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        for (int row = 0; row < guessRows; row++) {
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getSignName(filename));
            }
        }

        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getSignName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    private void animate(boolean animateOut) {
        if (correctAnswers == 0)
            return;
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        if(animateOut) {
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0
            );
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextSign();
                        }
                    }
            );
        }else {
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius
            );
        }
        animator.setDuration(ANIMATION_DURATION);
        animator.start();
    }

    private String getSignName(String name) {
        if(name.startsWith("Additional_panels")) {
            String stringValue = name.replace('-', '_');
            int resId = getContext().getResources().getIdentifier(stringValue, "string", getContext().getPackageName());
            try {
                return getContext().getString(resId);
            }catch (Resources.NotFoundException ignored) {
                return "";
            }
        }
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);
            String guess = guessButton.getText().toString();
            String answer = getSignName(correctAnswer);
            ++totalGuesses;
            if(guess.equals(answer)) {
                ++correctAnswers;
                answerTextView.setText((answer + "!"));
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));
                disableButtons();

                if (correctAnswers == SIGNS_IN_QUIZ) {
                    staticTotalGuesses = totalGuesses;
                    DialogFragment quizResults = new MyAlertDialogFragment();
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                    resetQuiz();
                } else {

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000);
                }
            }else {
                signImageView.startAnimation(shakeAnimation);
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);
            }
        }
    };

    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i  = 0; i < guessRow.getChildCount(); i++) {
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setMessage(
                    getString(R.string.results,
                            MainActivityFragment.staticTotalGuesses,
                            (1000 / (double) staticTotalGuesses)));

            // Reset Quiz painike
            builder.setPositiveButton(R.string.reset_quiz,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //System.out.println(" Testi tuloksia keskella " + MainActivityFragment.staticTotalGuesses);
                        }
                    }
            );
            // System.out.println(" tuloksia alla " + MainActivityFragment.staticTotalGuesses);
            return builder.create();
        }

    }

}
