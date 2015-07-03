/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.policy.impl;

import com.android.internal.R;
import com.android.internal.telephony.IccCard;
import com.android.internal.widget.LockPatternUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;

/**
 * The host view for all of the screens of the pattern unlock screen.  There are
 * two {@link Mode}s of operation, lock and unlock.  This will show the appropriate
 * screen, and listen for callbacks via
 * {@link com.android.internal.policy.impl.KeyguardScreenCallback}
 * from the current screen.
 *
 * This view, in turn, communicates back to
 * {@link com.android.internal.policy.impl.KeyguardViewManager}
 * via its {@link com.android.internal.policy.impl.KeyguardViewCallback}, as appropriate.
 */
public class LockPatternKeyguardView extends KeyguardViewBase
        implements AccountManagerCallback<Account[]> {

    // intent action for launching emergency dialer activity.
    static final String ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL";

    private static final boolean DEBUG = false;
    private static final String TAG = "LockPatternKeyguardView";

    private final KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardWindowController mWindowController;

    private View mLockScreen;
    private View mUnlockScreen;

    private boolean mScreenOn = false;
    private boolean mEnableFallback = false; // assume no fallback UI until we know better


    /**
     * The current {@link KeyguardScreen} will use this to communicate back to us.
     */
    KeyguardScreenCallback mKeyguardScreenCallback;


    private boolean mRequiresSim;


    /**
     * Either a lock screen (an informational keyguard screen), or an unlock
     * screen (a means for unlocking the device) is shown at any given time.
     */
    enum Mode {
        LockScreen,
        UnlockScreen
    }

    /**
     * The different types screens available for {@link Mode#UnlockScreen}.
     * @see com.android.internal.policy.impl.LockPatternKeyguardView#getUnlockMode()
     */
    enum UnlockMode {

        /**
         * Unlock by drawing a pattern.
         */
        Pattern,

        /**
         * Unlock by entering a sim pin.
         */
        SimPin,

        /**
         * Unlock by entering an account's login and password.
         */
        Account
    }

    /**
     * The current mode.
     */
    private Mode mMode = Mode.LockScreen;

    /**
     * Keeps track of what mode the current unlock screen is (cached from most recent computation in
     * {@link #getUnlockMode}).
     */
    private UnlockMode mUnlockScreenMode;

    private boolean mForgotPattern;

    /**
     * If true, it means we are in the process of verifying that the user
     * can get past the lock screen per {@link #verifyUnlock()}
     */
    private boolean mIsVerifyUnlockOnly = false;


    /**
     * Used to lookup the state of the lock pattern
     */
    private final LockPatternUtils mLockPatternUtils;

    private boolean mIsPortrait;

    /**
     * @return Whether we are stuck on the lock screen because the sim is
     *   missing.
     */
    private boolean stuckOnLockScreenBecauseSimMissing() {
        return mRequiresSim
                && (!mUpdateMonitor.isDeviceProvisioned())
                && (mUpdateMonitor.getSimState() == IccCard.State.ABSENT);
    }

    public void run(AccountManagerFuture<Account[]> future) {
        // We err on the side of caution.
        // In case of error we assume we have a SAML account.
        boolean hasSAMLAccount = true;
        try {
            hasSAMLAccount = future.getResult().length > 0;
        } catch (OperationCanceledException e) {
        } catch (IOException e) {
        } catch (AuthenticatorException e) {
        }
        mEnableFallback = !hasSAMLAccount;

        if (mUnlockScreen == null) {
            Log.w(TAG, "no unlock screen when receiving AccountManager information");
        } else if (mUnlockScreen instanceof UnlockScreen) {
            ((UnlockScreen)mUnlockScreen).setEnableFallback(true);
        }
    }

    /**
     * @param context Used to inflate, and create views.
     * @param updateMonitor Knows the state of the world, and passed along to each
     *   screen so they can use the knowledge, and also register for callbacks
     *   on dynamic information.
     * @param lockPatternUtils Used to look up state of lock pattern.
     */
    public LockPatternKeyguardView(
            Context context,
            KeyguardUpdateMonitor updateMonitor,
            LockPatternUtils lockPatternUtils,
            KeyguardWindowController controller) {
        super(context);

        mEnableFallback = false;

        mRequiresSim =
                TextUtils.isEmpty(SystemProperties.get("keyguard.no_require_sim"));

        mUpdateMonitor = updateMonitor;
        mLockPatternUtils = lockPatternUtils;
        mWindowController = controller;

        mMode = getInitialMode();

        mKeyguardScreenCallback = new KeyguardScreenCallback() {

            public void goToLockScreen() {
                mForgotPattern = false;
                if (mIsVerifyUnlockOnly) {
                    // navigating away from unlock screen during verify mode means
                    // we are done and the user failed to authenticate.
                    mIsVerifyUnlockOnly = false;
                    getCallback().keyguardDone(false);
                } else {
                    updateScreen(Mode.LockScreen);
                }
            }

            public void goToUnlockScreen() {
                final IccCard.State simState = mUpdateMonitor.getSimState();
                if (stuckOnLockScreenBecauseSimMissing()
                         || (simState == IccCard.State.PUK_REQUIRED)){
                    // stuck on lock screen when sim missing or puk'd
                    return;
                }
                if (!isSecure()) {
                    getCallback().keyguardDone(true);
                } else {
                    updateScreen(Mode.UnlockScreen);
                }
            }

            public void forgotPattern(boolean isForgotten) {
                if (mEnableFallback) {
                    mForgotPattern = isForgotten;
                    updateScreen(Mode.UnlockScreen);
                }
            }

            public boolean isSecure() {
                return LockPatternKeyguardView.this.isSecure();
            }

            public boolean isVerifyUnlockOnly() {
                return mIsVerifyUnlockOnly;
            }

            public void recreateMe() {
                recreateScreens();
            }

            public void takeEmergencyCallAction() {
                Intent intent = new Intent(ACTION_EMERGENCY_DIAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                getContext().startActivity(intent);
            }

            public void pokeWakelock() {
                getCallback().pokeWakelock();
            }

            public void pokeWakelock(int millis) {
                getCallback().pokeWakelock(millis);
            }

            public void keyguardDone(boolean authenticated) {
                getCallback().keyguardDone(authenticated);
            }

            public void keyguardDoneDrawing() {
                // irrelevant to keyguard screen, they shouldn't be calling this
            }

            public void reportFailedPatternAttempt() {
                mUpdateMonitor.reportFailedAttempt();
                final int failedAttempts = mUpdateMonitor.getFailedAttempts();
                if (DEBUG) Log.d(TAG,
                    "reportFailedPatternAttempt: #" + failedAttempts +
                    " (enableFallback=" + mEnableFallback + ")");
                if (mEnableFallback && failedAttempts ==
                        (LockPatternUtils.FAILED_ATTEMPTS_BEFORE_RESET
                                - LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT)) {
                    showAlmostAtAccountLoginDialog();
                } else if (mEnableFallback
                        && failedAttempts >= LockPatternUtils.FAILED_ATTEMPTS_BEFORE_RESET) {
                    mLockPatternUtils.setPermanentlyLocked(true);
                    updateScreen(mMode);
                } else if ((failedAttempts % LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT)
                        == 0) {
                    showTimeoutDialog();
                }
            }

            public boolean doesFallbackUnlockScreenExist() {
                return mEnableFallback;
            }
        };

        /**
         * We'll get key events the current screen doesn't use. see
         * {@link KeyguardViewBase#onKeyDown(int, android.view.KeyEvent)}
         */
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        // wall paper background
        if (false) {
            final BitmapDrawable drawable = (BitmapDrawable) context.getWallpaper();
            setBackgroundDrawable(
                    new FastBitmapDrawable(drawable.getBitmap()));
        }

        // create both the lock and unlock screen so they are quickly available
        // when the screen turns on
        mLockScreen = createLockScreen();
        addView(mLockScreen);
        final UnlockMode unlockMode = getUnlockMode();
        if (DEBUG) Log.d(TAG,
            "LockPatternKeyguardView ctor: about to createUnlockScreenFor; mEnableFallback="
            + mEnableFallback);
        mUnlockScreen = createUnlockScreenFor(unlockMode);
        mUnlockScreenMode = unlockMode;

        // Ask the account manager if we have an account that can be used as a
        // fallback in case the user forgets his pattern. The response comes
        // back in run() below; don't bother asking until you've called
        // createUnlockScreenFor(), else the information will go unused.
        final boolean hasAccount = AccountManager.get(context).getAccounts().length > 0;
        if (hasAccount) {
            /* If we have a SAML account which requires web login we can not use the
             fallback screen UI to ask the user for credentials.
             For now we will disable fallback screen in this case.
             Ultimately we could consider bringing up a web login from GLS
             but need to make sure that it will work in the "locked screen" mode. */
            String[] features = new String[] {"saml"};
            AccountManager.get(context).getAccountsByTypeAndFeatures(
                    "com.google", features, this, null);
        }

        addView(mUnlockScreen);
        updateScreen(mMode);
    }


    // TODO:
    // This overloaded method was added to workaround a race condition in the framework between
    // notification for orientation changed, layout() and switching resources.  This code attempts
    // to avoid drawing the incorrect layout while things are in transition.  The method can just
    // be removed once the race condition is fixed. See bugs 2262578 and 2292713.
    @Override
    protected void dispatchDraw(Canvas canvas) {
        final int orientation = getResources().getConfiguration().orientation;
        if (mIsPortrait && Configuration.ORIENTATION_PORTRAIT != orientation
                || getResources().getBoolean(R.bool.lockscreen_isPortrait) != mIsPortrait) {
            // Make sure we redraw once things settle down.
            // Log.v(TAG, "dispatchDraw(): not drawing because state is inconsistent");
            postInvalidate();

            // In order to minimize flashing, draw the first child's background for now.
            ViewGroup view = (ViewGroup) (mMode == Mode.LockScreen ? mLockScreen : mUnlockScreen);
            if (view != null && view.getChildAt(0) != null) {
                Drawable background = view.getChildAt(0).getBackground();
                if (background != null) {
                    background.draw(canvas);
                }
            }
            return;
        }
        super.dispatchDraw(canvas);
    }

    @Override
    public void reset() {
        mIsVerifyUnlockOnly = false;
        mForgotPattern = false;
        updateScreen(getInitialMode());
    }

    @Override
    public void onScreenTurnedOff() {
        mScreenOn = false;
        mForgotPattern = false;
        if (mMode == Mode.LockScreen) {
           ((KeyguardScreen) mLockScreen).onPause();
        } else {
            ((KeyguardScreen) mUnlockScreen).onPause();
        }
    }

    @Override
    public void onScreenTurnedOn() {
        mScreenOn = true;
        if (mMode == Mode.LockScreen) {
           ((KeyguardScreen) mLockScreen).onResume();
        } else {
            ((KeyguardScreen) mUnlockScreen).onResume();
        }
    }


    private void recreateScreens() {
        if (mLockScreen.getVisibility() == View.VISIBLE) {
            ((KeyguardScreen) mLockScreen).onPause();
        }
        ((KeyguardScreen) mLockScreen).cleanUp();
        removeViewInLayout(mLockScreen);

        mLockScreen = createLockScreen();
        mLockScreen.setVisibility(View.INVISIBLE);
        addView(mLockScreen);

        if (mUnlockScreen.getVisibility() == View.VISIBLE) {
            ((KeyguardScreen) mUnlockScreen).onPause();
        }
        ((KeyguardScreen) mUnlockScreen).cleanUp();
        removeViewInLayout(mUnlockScreen);

        final UnlockMode unlockMode = getUnlockMode();
        mUnlockScreen = createUnlockScreenFor(unlockMode);
        mUnlockScreen.setVisibility(View.INVISIBLE);
        mUnlockScreenMode = unlockMode;
        addView(mUnlockScreen);

        updateScreen(mMode);
    }


    @Override
    public void wakeWhenReadyTq(int keyCode) {
        if (DEBUG) Log.d(TAG, "onWakeKey");
        if (keyCode == KeyEvent.KEYCODE_MENU && isSecure() && (mMode == Mode.LockScreen)
                && (mUpdateMonitor.getSimState() != IccCard.State.PUK_REQUIRED)) {
            if (DEBUG) Log.d(TAG, "switching screens to unlock screen because wake key was MENU");
            updateScreen(Mode.UnlockScreen);
            getCallback().pokeWakelock();
        } else {
            if (DEBUG) Log.d(TAG, "poking wake lock immediately");
            getCallback().pokeWakelock();
        }
    }

    @Override
    public void verifyUnlock() {
        if (!isSecure()) {
            // non-secure keyguard screens are successfull by default
            getCallback().keyguardDone(true);
        } else if (mUnlockScreenMode != UnlockMode.Pattern) {
            // can only verify unlock when in pattern mode
            getCallback().keyguardDone(false);
        } else {
            // otherwise, go to the unlock screen, see if they can verify it
            mIsVerifyUnlockOnly = true;
            updateScreen(Mode.UnlockScreen);
        }
    }

    @Override
    public void cleanUp() {
        ((KeyguardScreen) mLockScreen).onPause();
        ((KeyguardScreen) mLockScreen).cleanUp();
        ((KeyguardScreen) mUnlockScreen).onPause();
        ((KeyguardScreen) mUnlockScreen).cleanUp();
    }

    private boolean isSecure() {
        UnlockMode unlockMode = getUnlockMode();
        if (unlockMode == UnlockMode.Pattern) {
            return mLockPatternUtils.isLockPatternEnabled();
        } else if (unlockMode == UnlockMode.SimPin) {
            return mUpdateMonitor.getSimState() == IccCard.State.PIN_REQUIRED
                        || mUpdateMonitor.getSimState() == IccCard.State.PUK_REQUIRED;
        } else if (unlockMode == UnlockMode.Account) {
            return true;
        } else {
            throw new IllegalStateException("unknown unlock mode " + unlockMode);
        }
    }

    private void updateScreen(final Mode mode) {

        mMode = mode;

        final View goneScreen = (mode == Mode.LockScreen) ? mUnlockScreen : mLockScreen;
        final View visibleScreen = (mode == Mode.LockScreen)
                ? mLockScreen : getUnlockScreenForCurrentUnlockMode();

        // do this before changing visibility so focus isn't requested before the input
        // flag is set
        mWindowController.setNeedsInput(((KeyguardScreen)visibleScreen).needsInput());


        if (mScreenOn) {
            if (goneScreen.getVisibility() == View.VISIBLE) {
                ((KeyguardScreen) goneScreen).onPause();
            }
            if (visibleScreen.getVisibility() != View.VISIBLE) {
                ((KeyguardScreen) visibleScreen).onResume();
            }
        }

        goneScreen.setVisibility(View.GONE);
        visibleScreen.setVisibility(View.VISIBLE);


        if (!visibleScreen.requestFocus()) {
            throw new IllegalStateException("keyguard screen must be able to take "
                    + "focus when shown " + visibleScreen.getClass().getCanonicalName());
        }
    }

    View createLockScreen() {
        return new LockScreen(
                mContext,
                mLockPatternUtils,
                mUpdateMonitor,
                mKeyguardScreenCallback);
    }

    View createUnlockScreenFor(UnlockMode unlockMode) {
        // Capture the orientation this layout was created in.
        mIsPortrait = getResources().getBoolean(R.bool.lockscreen_isPortrait);

        if (unlockMode == UnlockMode.Pattern) {
            UnlockScreen view = new UnlockScreen(
                    mContext,
                    mLockPatternUtils,
                    mUpdateMonitor,
                    mKeyguardScreenCallback,
                    mUpdateMonitor.getFailedAttempts());
            if (DEBUG) Log.d(TAG,
                "createUnlockScreenFor(" + unlockMode + "): mEnableFallback=" + mEnableFallback);
            view.setEnableFallback(mEnableFallback);
            return view;
        } else if (unlockMode == UnlockMode.SimPin) {
            return new SimUnlockScreen(
                    mContext,
                    mUpdateMonitor,
                    mKeyguardScreenCallback);
        } else if (unlockMode == UnlockMode.Account) {
            try {
                return new AccountUnlockScreen(
                        mContext,
                        mKeyguardScreenCallback,
                        mLockPatternUtils);
            } catch (IllegalStateException e) {
                Log.i(TAG, "Couldn't instantiate AccountUnlockScreen"
                      + " (IAccountsService isn't available)");
                // TODO: Need a more general way to provide a
                // platform-specific fallback UI here.
                // For now, if we can't display the account login
                // unlock UI, just bring back the regular "Pattern" unlock mode.

                // (We do this by simply returning a regular UnlockScreen
                // here.  This means that the user will still see the
                // regular pattern unlock UI, regardless of the value of
                // mUnlockScreenMode or whether or not we're in the
                // "permanently locked" state.)
                return createUnlockScreenFor(UnlockMode.Pattern);
            }
        } else {
            throw new IllegalArgumentException("unknown unlock mode " + unlockMode);
        }
    }

    private View getUnlockScreenForCurrentUnlockMode() {
        final UnlockMode unlockMode = getUnlockMode();

        // if a screen exists for the correct mode, we're done
        if (unlockMode == mUnlockScreenMode) {
            return mUnlockScreen;
        }

        // remember the mode
        mUnlockScreenMode = unlockMode;

        // unlock mode has changed and we have an existing old unlock screen
        // to clean up
        if (mScreenOn && (mUnlockScreen.getVisibility() == View.VISIBLE)) {
            ((KeyguardScreen) mUnlockScreen).onPause();
        }
        ((KeyguardScreen) mUnlockScreen).cleanUp();
        removeViewInLayout(mUnlockScreen);

        // create the new one
        mUnlockScreen = createUnlockScreenFor(unlockMode);
        mUnlockScreen.setVisibility(View.INVISIBLE);
        addView(mUnlockScreen);
        return mUnlockScreen;
    }

    /**
     * Given the current state of things, what should be the initial mode of
     * the lock screen (lock or unlock).
     */
    private Mode getInitialMode() {
        final IccCard.State simState = mUpdateMonitor.getSimState();
        if (stuckOnLockScreenBecauseSimMissing() || (simState == IccCard.State.PUK_REQUIRED)) {
            return Mode.LockScreen;
        } else if (isSecure()) {
            return Mode.UnlockScreen;
        } else {
            return Mode.LockScreen;
        }
    }

    /**
     * Given the current state of things, what should the unlock screen be?
     */
    private UnlockMode getUnlockMode() {
        final IccCard.State simState = mUpdateMonitor.getSimState();
        if (simState == IccCard.State.PIN_REQUIRED || simState == IccCard.State.PUK_REQUIRED) {
            return UnlockMode.SimPin;
        } else {
            return (mForgotPattern || mLockPatternUtils.isPermanentlyLocked()) ?
                    UnlockMode.Account:
                    UnlockMode.Pattern;
        }
    }

    private void showTimeoutDialog() {
        int timeoutInSeconds = (int) LockPatternUtils.FAILED_ATTEMPT_TIMEOUT_MS / 1000;
        String message = mContext.getString(
                R.string.lockscreen_too_many_failed_attempts_dialog_message,
                mUpdateMonitor.getFailedAttempts(),
                timeoutInSeconds);
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(null)
                .setMessage(message)
                .setNeutralButton(R.string.ok, null)
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }
        dialog.show();
    }

    private void showAlmostAtAccountLoginDialog() {
        int timeoutInSeconds = (int) LockPatternUtils.FAILED_ATTEMPT_TIMEOUT_MS / 1000;
        String message = mContext.getString(
                R.string.lockscreen_failed_attempts_almost_glogin,
                LockPatternUtils.FAILED_ATTEMPTS_BEFORE_RESET
                - LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT,
                LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT,
                timeoutInSeconds);
        final AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setTitle(null)
                .setMessage(message)
                .setNeutralButton(R.string.ok, null)
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        if (!mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_sf_slowBlur)) {
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }
        dialog.show();
    }

    /**
     * Used to put wallpaper on the background of the lock screen.  Centers it
     * Horizontally and pins the bottom (assuming that the lock screen is aligned
     * with the bottom, so the wallpaper should extend above the top into the
     * status bar).
     */
    static private class FastBitmapDrawable extends Drawable {
        private Bitmap mBitmap;
        private int mOpacity;

        private FastBitmapDrawable(Bitmap bitmap) {
            mBitmap = bitmap;
            mOpacity = mBitmap.hasAlpha() ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(
                    mBitmap,
                    (getBounds().width() - mBitmap.getWidth()) / 2,
                    (getBounds().height() - mBitmap.getHeight()),
                    null);
        }

        @Override
        public int getOpacity() {
            return mOpacity;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getIntrinsicWidth() {
            return mBitmap.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mBitmap.getHeight();
        }

        @Override
        public int getMinimumWidth() {
            return mBitmap.getWidth();
        }

        @Override
        public int getMinimumHeight() {
            return mBitmap.getHeight();
        }
    }
}

