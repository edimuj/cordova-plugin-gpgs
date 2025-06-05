/*
 * cordova-plugin-gpgs
 * Copyright (C) 2025 Exelerus AB
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.exelerus.cordova.plugin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.images.ImageManager;

import com.google.android.gms.games.AchievementsClient;

import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.GamesSignInClient;

import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;

import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;

import com.google.android.gms.games.FriendsResolutionRequiredException;
import com.google.android.gms.games.PlayerBuffer;

import com.google.android.gms.games.stats.PlayerStats;

import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.games.event.Event;

import android.content.Intent;

import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;

/**
 * Google Play Games Services Plugin for Cordova
 * 
 * A modern implementation of Google Play Games Services v2 API for Cordova/PhoneGap applications.
 * This plugin provides comprehensive gaming features including authentication, leaderboards,
 * achievements, cloud saves, and more.
 *
 * @author Exelerus AB
 * @version 1.0.0
 * @see <a href="https://github.com/edimuj/cordova-plugin-gpgs">GitHub Repository</a>
 */
public class GPGS extends CordovaPlugin {

    private static final String TAG = "GOOGLE_PLAY_GAMES";
    private boolean debugMode = false;

    private static final int RC_ACHIEVEMENT_UI = 9003;
    private static final int RC_LEADERBOARD_UI = 9004;
    private static final int RC_LEADERBOARDS_UI = 9005;
    private static final int RC_SAVED_GAMES = 9009;
    private static final int RC_SHOW_PROFILE = 9010;
    private static final int RC_SHOW_PLAYER_SEARCH = 9011;
    private static final int SHOW_SHARING_FRIENDS_CONSENT = 1111;
    private static final int RC_SIGN_IN = 9012;

    private static final String EVENT_LOAD_SAVED_GAME_REQUEST = "loadSavedGameRequest";
    private static final String EVENT_SAVE_GAME_REQUEST = "saveGameRequest";
    private static final String EVENT_SAVE_GAME_CONFLICT = "saveGameConflict";
    private static final String EVENT_FRIENDS_LIST_REQUEST_SUCCESSFUL = "friendsListRequestSuccessful";
    private static final String EVENT_SIGN_IN = "gpgs.signin";
    private static final String EVENT_SIGN_OUT = "gpgs.signout";
    private static final String EVENT_AVAILABILITY = "gpgs.availability";

    private static final int ERROR_CODE_HAS_RESOLUTION = 1;
    private static final int ERROR_CODE_NO_RESOLUTION = 2;

    private RelativeLayout bannerContainerLayout;
    private CordovaWebView cordovaWebView;
    private ViewGroup parentLayout;
    private boolean wasSignedIn = false;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        debugLog("Executing action: " + action);

        if (action.equals("isGooglePlayServicesAvailable")) {
            this.isGooglePlayServicesAvailableAction(callbackContext);
            return true;
        }
        else if (action.equals("login")) {
            this.loginAction(args, callbackContext);
            return true;
        }
        else if (action.equals("isSignedIn")) {
            this.isSignedInAction(callbackContext);
            return true;
        }
        else if (action.equals("unlockAchievement")) {
            this.unlockAchievementAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("incrementAchievement")) {
            this.incrementAchievementAction(args.getString(0), args.getInt(1), callbackContext);
            return true;
        }

        else if (action.equals("showAchievements")) {
            this.showAchievementsAction(callbackContext);
            return true;
        }

        else if (action.equals("revealAchievement")) {
            this.revealAchievementAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("setStepsInAchievement")) {
            this.setStepsInAchievementAction(args.getString(0), args.getInt(1), callbackContext);
            return true;
        }

        else if (action.equals("updatePlayerScore")) {
            this.updatePlayerScoreAction(args.getString(0), args.getInt(1), callbackContext);
            return true;
        }

        else if (action.equals("loadPlayerScore")) {
            this.loadPlayerScoreAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("showLeaderboard")) {
            this.showLeaderboardAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("showAllLeaderboards")) {
            this.showAllLeaderboardsAction(callbackContext);
            return true;
        }

        else if (action.equals("showSavedGames")) {
            this.showSavedGamesAction(args.getString(0), args.getBoolean(1), args.getBoolean(2), args.getInt(3), callbackContext);
            return true;
        }

        else if (action.equals("saveGame")) {
            this.saveGameAction(args.getString(0), args.getString(1), args.getJSONObject(2), callbackContext);
            return true;
        }

        else if (action.equals("loadGameSave")) {
            this.loadGameSaveAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("getFriendsList")) {
            this.getFriendsListAction(callbackContext);
            return true;
        }

        else if (action.equals("showAnotherPlayersProfile")) {
            this.showAnotherPlayersProfileAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("showPlayerSearch")) {
            this.showPlayerSearchAction(callbackContext);
            return true;
        }

        else if (action.equals("getPlayer")) {
            this.getPlayerAction(args.getString(0), args.getBoolean(1), callbackContext);
            return true;
        }

        else if (action.equals("getCurrentPlayerStats")) {
            this.getCurrentPlayerStatsAction(callbackContext);
            return true;
        }

        else if (action.equals("incrementEvent")) {
            this.incrementEventAction(args.getString(0), args.getInt(1), callbackContext);
            return true;
        }

        else if (action.equals("getAllEvents")) {
            this.getAllEventsAction(callbackContext);
            return true;
        }

        else if (action.equals("getEvent")) {
            this.getEventAction(args.getString(0), callbackContext);
            return true;
        }

        return false;
    }

    /** --------------------------------------------------------------- */

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        bannerContainerLayout = new RelativeLayout(cordova.getActivity());
        parentLayout = (ViewGroup) webView.getEngine().getView().getParent();
        cordovaWebView = webView;
        super.initialize(cordova, webView);
        
        // Initialize debug mode from preferences
        debugMode = preferences.getBoolean("GPGS_DEBUG", false);
        debugLog("Initializing GPGS plugin with debug mode: " + debugMode);
        
        // Initialize Play Games SDK with modern v2 approach
        PlayGamesSdk.initialize(cordova.getActivity());

        // Check Google Play Services availability and attempt silent sign-in
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                    int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
                    
                    if (resultCode == ConnectionResult.SUCCESS) {
                        debugLog("Google Play Services are available, attempting silent sign-in");
                        // Attempt silent sign-in
                        PlayGamesSignInClient signInClient = PlayGames.getSignInClient(cordova.getActivity());
                        signInClient.silentSignIn().addOnCompleteListener(new OnCompleteListener<Player>() {
                            @Override
                            public void onComplete(@NonNull Task<Player> task) {
                                if (task.isSuccessful()) {
                                    debugLog("Silent sign-in successful during initialization");
                                    wasSignedIn = true;
                                    // Emit event for successful sign-in
                                    try {
                                        JSONObject data = new JSONObject();
                                        data.put("isSignedIn", true);
                                        emitWindowEvent(EVENT_SIGN_IN, data);
                                    } catch (JSONException e) {
                                        debugLog("Error creating sign-in event data: " + e.getMessage(), e);
                                    }
                                } else {
                                    debugLog("Silent sign-in failed during initialization: " + 
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                                    wasSignedIn = false;
                                    // Emit event for failed sign-in
                                    try {
                                        JSONObject data = new JSONObject();
                                        data.put("isSignedIn", false);
                                        if (task.getException() != null) {
                                            data.put("error", task.getException().getMessage());
                                        }
                                        emitWindowEvent(EVENT_SIGN_IN, data);
                                    } catch (JSONException e) {
                                        debugLog("Error creating sign-in event data: " + e.getMessage(), e);
                                    }
                                }
                            }
                        });
                    } else {
                        debugLog("Google Play Services are not available (result code: " + resultCode + ")");
                        // Emit event for Google Play Services unavailability
                        try {
                            JSONObject data = new JSONObject();
                            data.put("available", false);
                            data.put("errorCode", resultCode);
                            data.put("errorString", googleApiAvailability.getErrorString(resultCode));
                            data.put("isUserResolvable", googleApiAvailability.isUserResolvableError(resultCode));
                            emitWindowEvent(EVENT_AVAILABILITY, data);
                        } catch (JSONException e) {
                            debugLog("Error creating availability event data: " + e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    debugLog("Error during initialization: " + e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        
        // Check for background sign-out
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    PlayGamesSignInClient signInClient = PlayGames.getSignInClient(cordova.getActivity());
                    signInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            if (task.isSuccessful()) {
                                boolean isCurrentlySignedIn = task.getResult();
                                // If we were signed in but now we're not, emit sign-out event
                                if (wasSignedIn && !isCurrentlySignedIn) {
                                    debugLog("User signed out in background");
                                    try {
                                        JSONObject data = new JSONObject();
                                        data.put("isSignedIn", false);
                                        data.put("reason", "background_signout");
                                        emitWindowEvent(EVENT_SIGN_OUT, data);
                                    } catch (JSONException e) {
                                        debugLog("Error creating sign-out event data: " + e.getMessage(), e);
                                    }
                                }
                                wasSignedIn = isCurrentlySignedIn;
                            }
                        }
                    });
                } catch (Exception e) {
                    debugLog("Error checking sign-in status in onResume: " + e.getMessage(), e);
                }
            }
        });
    }

    /** ----------------------- UTILS --------------------------- */

    private void emitWindowEvent(final String event) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.loadUrl(String.format("javascript:cordova.fireWindowEvent('%s');", event));
            }
        });
    }

    private void emitWindowEvent(final String event, final JSONObject data) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.loadUrl(String.format("javascript:cordova.fireWindowEvent('%s', %s);", event, data.toString()));
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (intent != null) {
            if (requestCode == RC_SHOW_PLAYER_SEARCH) {
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<Player> snapshotMetadata =
                            intent.getParcelableArrayListExtra(PlayersClient.EXTRA_PLAYER_SEARCH_RESULTS);
                    Player player = snapshotMetadata.get(0);
                    this.showAnotherPlayersProfileAction(player.getPlayerId(), null);
                }
            }
            else if (requestCode == SHOW_SHARING_FRIENDS_CONSENT) {
                if (resultCode == Activity.RESULT_OK) {
                    Log.e(TAG, "Load friends: OK");
                    this.emitWindowEvent(EVENT_FRIENDS_LIST_REQUEST_SUCCESSFUL);
                } else {
                    Log.e(TAG, "Load friends: No access");
                }
            }
            else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                // Load a snapshot.
                SnapshotMetadata snapshotMetadata =
                        intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
                String mCurrentSaveName = snapshotMetadata.getUniqueName();
                try {
                    JSONObject result = new JSONObject();
                    result.put("id", mCurrentSaveName);
                    this.emitWindowEvent(EVENT_LOAD_SAVED_GAME_REQUEST, result);
                } catch (JSONException err) {
                    Log.d(TAG, "onActivityResult error", err);
                }
            } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                // Create a new snapshot named with a unique string
                this.emitWindowEvent(EVENT_SAVE_GAME_REQUEST);
            }
        }
    }

    /**
     * Silent sign-in as recommended in v2 migration guide
     */
    private void signInSilently() {
        debugLog("Attempting silent sign-in");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayGamesSignInClient signInClient = PlayGames.getSignInClient(cordova.getActivity());
                signInClient.silentSignIn().addOnCompleteListener(new OnCompleteListener<Player>() {
                    @Override
                    public void onComplete(@NonNull Task<Player> task) {
                        if (task.isSuccessful()) {
                            debugLog("Silent sign-in successful");
                            // Successfully signed in
                        } else {
                            debugLog("Silent sign-in failed: " + task.getException().getMessage(), task.getException());
                            // Failed to sign in silently
                        }
                    }
                });
            }
        });
    }

    /**
     * Login - Updated for v2 API
     */
    private void loginAction(JSONArray args, final CallbackContext callbackContext) {
        debugLog("Starting login action");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    debugLog("Attempting to sign in silently first");
                    signInSilently();
                } catch (Exception e) {
                    debugLog("Silent sign-in failed, showing sign-in UI", e);
                    PlayGamesSignInClient signInClient = PlayGames.getSignInClient(cordova.getActivity());
                    signInClient.signIn().addOnCompleteListener(new OnCompleteListener<Player>() {
                        @Override
                        public void onComplete(@NonNull Task<Player> task) {
                            if (task.isSuccessful()) {
                                debugLog("Sign-in successful");
                                callbackContext.success();
                            } else {
                                debugLog("Sign-in failed: " + task.getException().getMessage(), task.getException());
                                callbackContext.error("Sign-in failed: " + task.getException().getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * Unlock achievement
     */
    private void unlockAchievementAction(String achievementId, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).unlock(achievementId);
                callbackContext.success();
            }
        });
    }

    /**
     * Increment achievement
     */
    private void incrementAchievementAction(String achievementId, Integer count, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).increment(achievementId, count);
                callbackContext.success();
            }
        });
    }

    /**
     * Reveal achievement for current user
     */
    private void revealAchievementAction(String achievementId, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).reveal(achievementId);
                callbackContext.success();
            }
        });
    }

    /**
     * Show achievements
     */
    private void showAchievementsAction(final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                AchievementsClient client = PlayGames.getAchievementsClient(cordova.getActivity());
                client
                    .load(true)
                    .addOnSuccessListener((data) -> {
                       client
                           .getAchievementsIntent()
                           .addOnSuccessListener(new OnSuccessListener<Intent>() {
                               @Override
                               public void onSuccess(Intent intent) {
                                   cordova.setActivityResultCallback(self);
                                   cordova.getActivity().startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                                   callbackContext.success();
                               }
                           });
                    });
            }
        });
    }

    /**
     * Set steps in achievement
     */
    private void setStepsInAchievementAction(String achievementId, int count, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).setSteps(achievementId, count);
                callbackContext.success();
            }
        });
    }

    /**
     * Update player score
     */
    private void updatePlayerScoreAction(String leaderboardId, Integer score, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .submitScore(leaderboardId, score);
                callbackContext.success();
            }
        });
    }

    /**
     * Load player score
     */
    private void loadPlayerScoreAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                    .loadCurrentPlayerLeaderboardScore(leaderboardId, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC)
                    .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                        @Override
                        public void onSuccess(AnnotatedData<LeaderboardScore> score) {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("score", score.get().getRawScore());
                                result.put("displayScore", score.get().getDisplayScore());
                                result.put("scoreTag", score.get().getScoreTag());
                                callbackContext.success(result);
                            } catch (JSONException e) {
                                callbackContext.error("JSON error: " + e.getMessage());
                            }
                        }
                    });
            }
        });
    }

    /**
     * Show leaderboard
     */
    private void showLeaderboardAction(String leaderboardId, final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .getLeaderboardIntent(leaderboardId)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(self);
                                cordova.getActivity().startActivityForResult(intent, RC_LEADERBOARD_UI);
                                callbackContext.success();
                            }
                        });
            }
        });
    }

    /**
     * Show all leaderboards
     */
    private void showAllLeaderboardsAction(final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                LeaderboardsClient client = PlayGames.getLeaderboardsClient(cordova.getActivity());
                client
                        .loadLeaderboardMetadata(true)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<LeaderboardBuffer> data) {
                                client.getAllLeaderboardsIntent()
                                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                                            @Override
                                            public void onSuccess(Intent intent) {
                                                cordova.setActivityResultCallback(self);
                                                cordova.getActivity().startActivityForResult(intent, RC_LEADERBOARDS_UI);
                                                callbackContext.success();
                                            }
                                        });
                            }
                        });
            }
        });
    }

    /**
     * Show saved games
     */
    private void showSavedGamesAction(String title, Boolean allowAddButton, Boolean allowDelete, Integer numberOfSavedGames, final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                SnapshotsClient snapshotsClient =
                        PlayGames.getSnapshotsClient(cordova.getActivity());
                int maxNumberOfSavedGamesToShow = numberOfSavedGames;

                snapshotsClient
                    .getSelectSnapshotIntent(title, allowAddButton, allowDelete, maxNumberOfSavedGamesToShow)
                    .addOnSuccessListener(
                        new OnSuccessListener<Intent>(){
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(self);
                                cordova.getActivity().startActivityForResult(intent, RC_SAVED_GAMES);
                                callbackContext.success();
                            }
                        }
                    );
            }
        });
    }

    /**
     * Save game
     */
    private void saveGameAction(String snapshotName, String snapshotDescription, JSONObject snapshotContents, final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;
                SnapshotsClient snapshotsClient =
                        PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient
                    .open(snapshotName, true, conflictResolutionPolicy)
                    .addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>(){
                        @Override
                        public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> dataOrConflict) {
                            if (dataOrConflict.isConflict()) {
                                // Send conflict id
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("conflictId", dataOrConflict.getConflict().getConflictId());
                                    self.emitWindowEvent(EVENT_SAVE_GAME_CONFLICT, result);
                                } catch (JSONException err) {
                                    callbackContext.error(err.getMessage());
                                }
                                return;
                            }
                            // Set the data payload for the snapshot
                            dataOrConflict.getData().getSnapshotContents().writeBytes(snapshotContents.toString().getBytes(StandardCharsets.UTF_8));

                            // Create the change operation
                            SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                    .setDescription(snapshotDescription)
                                    .build();

                            // Commit the operation
                            snapshotsClient.commitAndClose(dataOrConflict.getData(), metadataChange);
                            callbackContext.success();
                        }
                    });
            }
        });
    }

    /**
     * Save game
     */
    private void loadGameSaveAction(String snapshotName, final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Log.e(TAG, "READ START");
                SnapshotsClient snapshotsClient =
                        PlayGames.getSnapshotsClient(cordova.getActivity());
                // In the case of a conflict, the most recently modified version of this snapshot will be used.
                int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

                // Open the saved game using its name.
                snapshotsClient
                    .open(snapshotName, true, conflictResolutionPolicy)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Error while opening Snapshot.", e);
                            callbackContext.error(e.getMessage());
                        }
                    })
                    .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
                        @Override
                        public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                            Snapshot snapshot = task.getResult().getData();
                            // Opening the snapshot was a success and any conflicts have been resolved.
                            try {
                                // Extract the raw data from the snapshot.
                                return snapshot.getSnapshotContents().readFully();
                            } catch (IOException e) {
                                Log.e(TAG, "Error while reading Snapshot.", e);
                                callbackContext.error(e.getMessage());
                            }
                            return null;
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<byte[]>() {
                        @Override
                        public void onComplete(@NonNull Task<byte[]> task) {
                            task.addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    try {
                                        Log.e(TAG, "READ SUCCESS");
                                        callbackContext.success(new JSONObject(new String(bytes, StandardCharsets.UTF_8)));
                                    } catch (JSONException e) {
                                        callbackContext.error(e.getMessage());
                                    }
                                }
                            });
                        }
                    });
            }
        });
    }

    /**
     * Get friends list
     */
    private void getFriendsListAction(final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                int pageSize = 200;
                PlayGames.getPlayersClient(cordova.getActivity())
                        .loadFriends(pageSize,false)
                        .addOnSuccessListener(
                            new OnSuccessListener<AnnotatedData<PlayerBuffer>>() {
                                @Override
                                public void onSuccess(AnnotatedData<PlayerBuffer> data) {
                                    PlayerBuffer playerBuffer = data.get();

                                    JSONArray players = new JSONArray();
                                    try {
                                        AtomicInteger currentCount = new AtomicInteger();
                                        int totalCount = playerBuffer.getCount();
                                        for (int i = 0; i < playerBuffer.getCount(); i++) {
                                            JSONObject player = new JSONObject();
                                            player.put("id", playerBuffer.get(i).getPlayerId());
                                            player.put("name", playerBuffer.get(i).getDisplayName());
                                            player.put("title", playerBuffer.get(i).getTitle());
                                            player.put("retrievedTimestamp", playerBuffer.get(i).getRetrievedTimestamp());
                                            if (playerBuffer.get(i).getBannerImageLandscapeUri() != null) {
                                                player.put("bannerImageLandscapeUri", playerBuffer.get(i).getBannerImageLandscapeUri().toString());
                                            }
                                            if (playerBuffer.get(i).getBannerImagePortraitUri() != null) {
                                                player.put("bannerImagePortraitUri", playerBuffer.get(i).getBannerImagePortraitUri().toString());
                                            }
                                            if (playerBuffer.get(i).hasIconImage()) {
                                                player.put("iconImageUri", playerBuffer.get(i).getIconImageUri().toString());
                                            }
                                            if (playerBuffer.get(i).hasHiResImage()) {
                                                player.put("hiResImageUri", playerBuffer.get(i).getHiResImageUri().toString());
                                            }
                                            if (playerBuffer.get(i).getLevelInfo() != null) {
                                                JSONObject levelInfo = new JSONObject();
                                                levelInfo.put("currentLevel", playerBuffer.get(i).getLevelInfo().getCurrentLevel().getLevelNumber());
                                                levelInfo.put("maxXp", playerBuffer.get(i).getLevelInfo().getCurrentLevel().getMaxXp());
                                                levelInfo.put("minXp", playerBuffer.get(i).getLevelInfo().getCurrentLevel().getMinXp());
                                                levelInfo.put("hashCode", playerBuffer.get(i).getLevelInfo().getCurrentLevel().hashCode());
                                                player.put("levelInfo", levelInfo);
                                            }
                                            if (playerBuffer.get(i).getCurrentPlayerInfo() != null) {
                                                JSONObject currentPlayerInfo = new JSONObject();
                                                currentPlayerInfo.put("friendsListVisibilityStatus", playerBuffer.get(i).getCurrentPlayerInfo().getFriendsListVisibilityStatus());
                                                player.put("currentPlayerInfo", currentPlayerInfo);
                                            }
                                            if (playerBuffer.get(i).getRelationshipInfo() != null) {
                                                player.put("friendStatus", playerBuffer.get(i).getRelationshipInfo().getFriendStatus());
                                            }

                                            if (playerBuffer.get(i).hasIconImage()) {
                                                ImageManager mgr = ImageManager.create(cordova.getContext());
                                                mgr.loadImage((uri, drawable, isRequestedDrawable) -> {
                                                    if (isRequestedDrawable) {
                                                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                                                        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                                        try {
                                                            player.put("iconImageBase64", "data:image/png;base64, " + encoded);
                                                            players.put(player);
                                                            currentCount.addAndGet(1);
                                                            if (currentCount.intValue() >= totalCount) {
                                                                callbackContext.success(players);
                                                            }
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Error while getting base64 for user from friend list.");
                                                        }
                                                    }
                                                }, Objects.requireNonNull(playerBuffer.get(i).getIconImageUri()));
                                            } else {
                                                players.put(player);
                                                currentCount.addAndGet(1);
                                                if (currentCount.intValue() >= totalCount) {
                                                    callbackContext.success(players);
                                                }
                                            }
                                        }
                                    } catch (JSONException err) {
                                        try {
                                            JSONObject error = new JSONObject();
                                            error.put("code", ERROR_CODE_NO_RESOLUTION);
                                            error.put("message", "Error while retrieving friends list: " + err.getMessage());
                                            callbackContext.error(error);
                                        } catch (JSONException e) {
                                            callbackContext.error("Error while retrieving friends list: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                        )
                        .addOnFailureListener(exception -> {
                            if (exception instanceof FriendsResolutionRequiredException) {
                                PendingIntent pendingIntent =
                                        ((FriendsResolutionRequiredException) exception).getResolution();
                                try {
                                    cordova.setActivityResultCallback(self);
                                    cordova.getActivity().startIntentSenderForResult(
                                            pendingIntent.getIntentSender(),
                                            /* requestCode */ SHOW_SHARING_FRIENDS_CONSENT,
                                            /* fillInIntent */ null,
                                            /* flagsMask */ 0,
                                            /* flagsValues */ 0,
                                            /* extraFlags */ 0,
                                            /* options */ null);
                                    try {
                                        JSONObject error = new JSONObject();
                                        error.put("code", ERROR_CODE_HAS_RESOLUTION);
                                        error.put("message", "Waiting user give permission for fetch friends list, check events.");
                                        callbackContext.error(error);
                                    } catch (JSONException e) {
                                        callbackContext.error("Error while retrieving friends list: " + e.getMessage());
                                    }
                                } catch (IntentSender.SendIntentException err) {
                                    try {
                                        JSONObject error = new JSONObject();
                                        error.put("code", ERROR_CODE_NO_RESOLUTION);
                                        error.put("message", "Error while asking permission for retrieving friends list: " + err.getMessage());
                                        callbackContext.error(error);
                                    } catch (JSONException e) {
                                        callbackContext.error("Error while retrieving friends list: " + e.getMessage());
                                    }
                                }
                            }
                        });
            }
        });
    }

    /**
     * Show another player profile, used also for player search
     */
    private void showAnotherPlayersProfileAction(String playerId, @Nullable final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getPlayersClient(cordova.getActivity())
                    .getCompareProfileIntent(playerId)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent  intent) {
                            cordova.setActivityResultCallback(self);
                            cordova.getActivity().startActivityForResult(intent, RC_SHOW_PROFILE);
                            if (callbackContext != null) {
                                callbackContext.success();
                            }
                        }});
            }
        });
    }

    /**
     * Show player search default window
     */
    private void showPlayerSearchAction(final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getPlayersClient(cordova.getActivity())
                        .getPlayerSearchIntent()
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent  intent) {
                                cordova.setActivityResultCallback(self);
                                cordova.getActivity().startActivityForResult(intent, RC_SHOW_PLAYER_SEARCH);
                                callbackContext.success();
                            }});
            }
        });
    }

    private void getPlayerAction(String id, Boolean forceReload, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getPlayersClient(cordova.getActivity())
                    .loadPlayer(id, forceReload)
                    .addOnSuccessListener((data) -> {
                        Player player = data.get();
                        JSONObject result = new JSONObject();
                        if (player == null) {
                            callbackContext.success(result);
                            return;
                        }
                        try {
                            result.put("id", player.getPlayerId());
                            result.put("name", player.getDisplayName());
                            result.put("title", player.getTitle());
                            result.put("retrievedTimestamp", player.getRetrievedTimestamp());
                            if (player.getBannerImageLandscapeUri() != null) {
                                result.put("bannerImageLandscapeUri", player.getBannerImageLandscapeUri().toString());
                            }
                            if (player.getBannerImagePortraitUri() != null) {
                                result.put("bannerImagePortraitUri", player.getBannerImagePortraitUri().toString());
                            }
                            if (player.hasIconImage()) {
                                result.put("iconImageUri", player.getIconImageUri().toString());
                            }
                            if (player.hasHiResImage()) {
                                result.put("hiResImageUri", player.getHiResImageUri().toString());
                            }
                            if (player.getLevelInfo() != null) {
                                JSONObject levelInfo = new JSONObject();
                                levelInfo.put("currentLevel", player.getLevelInfo().getCurrentLevel().getLevelNumber());
                                levelInfo.put("maxXp", player.getLevelInfo().getCurrentLevel().getMaxXp());
                                levelInfo.put("minXp", player.getLevelInfo().getCurrentLevel().getMinXp());
                                levelInfo.put("hashCode", player.getLevelInfo().getCurrentLevel().hashCode());
                                result.put("levelInfo", levelInfo);
                            }

                            if (player.hasIconImage()) {
                                ImageManager mgr = ImageManager.create(cordova.getContext());
                                mgr.loadImage((uri, drawable, isRequestedDrawable) -> {
                                    if (isRequestedDrawable) {
                                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                                        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                        try {
                                            result.put("iconImageBase64", "data:image/png;base64, " + encoded);
                                            callbackContext.success(result);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error while getting base64 for user from friend list.");
                                        }
                                    }
                                }, Objects.requireNonNull(player.getIconImageUri()));
                            } else {
                                callbackContext.success(result);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error while getting player.");
                        }
                    });
            }
        });
    }

    /**
     * Returns current player stats
     */
    private void getCurrentPlayerStatsAction(final CallbackContext callbackContext) {
        PlayGames.getPlayerStatsClient(cordova.getActivity())
            .loadPlayerStats(true)
            .addOnCompleteListener(new OnCompleteListener<AnnotatedData<PlayerStats>>() {
                @Override
                public void onComplete(@NonNull Task<AnnotatedData<PlayerStats>> task) {
                    if (task.isSuccessful()) {
                        // Check for cached data.
                        if (task.getResult().isStale()) {
                            Log.d(TAG, "Using cached data");
                        }
                        PlayerStats stats = task.getResult().get();
                        JSONObject resultStats = new JSONObject();
                        if (stats != null) {
                            Log.d(TAG, "Player stats loaded");
                            try {
                                resultStats.put("averageSessionLength", stats.getAverageSessionLength());
                                resultStats.put("daysSinceLastPlayed", stats.getDaysSinceLastPlayed());
                                resultStats.put("numberOfPurchases", stats.getNumberOfPurchases());
                                resultStats.put("numberOfSessions", stats.getNumberOfSessions());
                                resultStats.put("sessionPercentile", stats.getSessionPercentile());
                                resultStats.put("spendPercentile", stats.getSpendPercentile());
                                callbackContext.success(resultStats);
                            } catch (JSONException e) {
                                callbackContext.error("Error while creating player stats result object. Error: " + e.getMessage());
                            }
                        }
                    } else {
                        int status = CommonStatusCodes.DEVELOPER_ERROR;
                        if (task.getException() instanceof ApiException) {
                            status = ((ApiException) task.getException()).getStatusCode();
                        }
                        if (task.getException() != null) {
                            callbackContext.error("Failed to fetch Stats Data status: " + status + ": " + task.getException().getMessage());
                        }
                        Log.d(TAG, "Failed to fetch Stats Data status: " + status + ": " + task.getException());
                    }
                }
            });
    }

    /**
     * Get all events (user custom stats)
     */
    private void getAllEventsAction(final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getEventsClient(cordova.getActivity())
                    .load(true)
                    .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                        @Override
                        public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                            if (task.isSuccessful()) {
                                // Process all the events.
                                JSONArray events = new JSONArray();
                                AtomicInteger currentCount = new AtomicInteger();
                                int totalCount = task.getResult().get().getCount();
                                if (totalCount == 0) {
                                    callbackContext.success(events);
                                }
                                for (Event event : task.getResult().get()) {
                                    Log.d(TAG, "loaded event " + event.getName() + ", " + event.getEventId() + ", " + event.getDescription() + ", " + event.getIconImageUri() + ", " + event.getFormattedValue() + ", " + event.getValue() + ", " + event.getPlayer().getPlayerId() + ", " + event.isVisible());
                                    try {
                                        JSONObject resultEvent = new JSONObject();
                                        resultEvent.put("id", event.getEventId());
                                        resultEvent.put("name", event.getName());
                                        resultEvent.put("description", event.getDescription());
                                        resultEvent.put("iconImageUri", event.getIconImageUri());
                                        resultEvent.put("formattedValue", event.getFormattedValue());
                                        resultEvent.put("value", event.getValue());

                                        JSONObject player = new JSONObject();
                                        player.put("id", event.getPlayer().getPlayerId());
                                        player.put("name", event.getPlayer().getDisplayName());
                                        player.put("title", event.getPlayer().getTitle());
                                        player.put("retrievedTimestamp", event.getPlayer().getRetrievedTimestamp());
                                        if (event.getPlayer().getBannerImageLandscapeUri() != null) {
                                            player.put("bannerImageLandscapeUri", event.getPlayer().getBannerImageLandscapeUri().toString());
                                        }
                                        if (event.getPlayer().getBannerImagePortraitUri() != null) {
                                            player.put("bannerImagePortraitUri", event.getPlayer().getBannerImagePortraitUri().toString());
                                        }
                                        if (event.getPlayer().hasIconImage()) {
                                            player.put("iconImageUri", event.getPlayer().getIconImageUri().toString());
                                        }
                                        if (event.getPlayer().hasHiResImage()) {
                                            player.put("hiResImageUri", event.getPlayer().getHiResImageUri().toString());
                                        }
                                        if (event.getPlayer().getLevelInfo() != null) {
                                            JSONObject levelInfo = new JSONObject();
                                            levelInfo.put("currentLevel", event.getPlayer().getLevelInfo().getCurrentLevel().getLevelNumber());
                                            levelInfo.put("maxXp", event.getPlayer().getLevelInfo().getCurrentLevel().getMaxXp());
                                            levelInfo.put("minXp", event.getPlayer().getLevelInfo().getCurrentLevel().getMinXp());
                                            levelInfo.put("hashCode", event.getPlayer().getLevelInfo().getCurrentLevel().hashCode());
                                            player.put("levelInfo", levelInfo);
                                        }

                                        if (event.getPlayer().hasIconImage()) {
                                            ImageManager mgr = ImageManager.create(cordova.getContext());
                                            mgr.loadImage((uri, drawable, isRequestedDrawable) -> {
                                                if (isRequestedDrawable) {
                                                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                                                    String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                                    try {
                                                        player.put("iconImageBase64", "data:image/png;base64, " + encoded);
                                                        resultEvent.put("player", player);
                                                        events.put(resultEvent);
                                                        currentCount.addAndGet(1);
                                                        if (currentCount.intValue() >= totalCount) {
                                                            callbackContext.success(events);
                                                        }
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error while getting base64 for user from friend list.");
                                                    }
                                                }
                                            }, Objects.requireNonNull(event.getPlayer().getIconImageUri()));
                                        } else {
                                            resultEvent.put("player", player);
                                            events.put(resultEvent);
                                            currentCount.addAndGet(1);
                                            if (currentCount.intValue() >= totalCount) {
                                                callbackContext.success(events);
                                            }
                                        }
                                    } catch (JSONException e) {
                                        Log.d(TAG, "Error while receiving event: " + e.getMessage());
                                    }
                                }
                            } else {
                                // Handle Error
                                Exception exception = task.getException();
                                int statusCode = CommonStatusCodes.DEVELOPER_ERROR;
                                if (exception instanceof ApiException) {
                                    ApiException apiException = (ApiException) exception;
                                    statusCode = apiException.getStatusCode();
                                }
                                Log.d(TAG, "event error " + statusCode);
                            }
                        }
                    });
            }
        });
    }

    /**
     * Get current user event
     */
    private void getEventAction(String id, final CallbackContext callbackContext) {
        GPGS self = this;
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getEventsClient(cordova.getActivity())
                    .loadByIds(true, id)
                    .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                        @Override
                        public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                            if (task.isSuccessful()) {
                                // Process all the events.
                                for (Event event : task.getResult().get()) {
                                    Log.d(TAG, "loaded event " + event.getName() + ", " + event.getEventId() + ", " + event.getDescription() + ", " + event.getIconImageUri() + ", " + event.getFormattedValue() + ", " + event.getValue() + ", " + event.getPlayer().getPlayerId() + ", " + event.isVisible());
                                    try {
                                        JSONObject resultEvent = new JSONObject();
                                        resultEvent.put("id", event.getEventId());
                                        resultEvent.put("name", event.getName());
                                        resultEvent.put("description", event.getDescription());
                                        resultEvent.put("iconImageUri", event.getIconImageUri());
                                        resultEvent.put("formattedValue", event.getFormattedValue());
                                        resultEvent.put("value", event.getValue());

                                        JSONObject player = new JSONObject();
                                        player.put("id", event.getPlayer().getPlayerId());
                                        player.put("name", event.getPlayer().getDisplayName());
                                        player.put("title", event.getPlayer().getTitle());
                                        player.put("retrievedTimestamp", event.getPlayer().getRetrievedTimestamp());
                                        if (event.getPlayer().getBannerImageLandscapeUri() != null) {
                                            player.put("bannerImageLandscapeUri", event.getPlayer().getBannerImageLandscapeUri().toString());
                                        }
                                        if (event.getPlayer().getBannerImagePortraitUri() != null) {
                                            player.put("bannerImagePortraitUri", event.getPlayer().getBannerImagePortraitUri().toString());
                                        }
                                        if (event.getPlayer().hasIconImage()) {
                                            player.put("iconImageUri", event.getPlayer().getIconImageUri().toString());
                                        }
                                        if (event.getPlayer().hasHiResImage()) {
                                            player.put("hiResImageUri", event.getPlayer().getHiResImageUri().toString());
                                        }
                                        if (event.getPlayer().getLevelInfo() != null) {
                                            JSONObject levelInfo = new JSONObject();
                                            levelInfo.put("currentLevel", event.getPlayer().getLevelInfo().getCurrentLevel().getLevelNumber());
                                            levelInfo.put("maxXp", event.getPlayer().getLevelInfo().getCurrentLevel().getMaxXp());
                                            levelInfo.put("minXp", event.getPlayer().getLevelInfo().getCurrentLevel().getMinXp());
                                            levelInfo.put("hashCode", event.getPlayer().getLevelInfo().getCurrentLevel().hashCode());
                                            player.put("levelInfo", levelInfo);
                                        }

                                        if (event.getPlayer().hasIconImage()) {
                                            ImageManager mgr = ImageManager.create(cordova.getContext());
                                            mgr.loadImage((uri, drawable, isRequestedDrawable) -> {
                                                if (isRequestedDrawable) {
                                                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                                                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                                                    String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                                    try {
                                                        player.put("iconImageBase64", "data:image/png;base64, " + encoded);
                                                        resultEvent.put("player", player);
                                                        callbackContext.success(resultEvent);
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error while getting base64 for user from friend list.");
                                                    }
                                                }
                                            }, Objects.requireNonNull(event.getPlayer().getIconImageUri()));
                                        } else {
                                            resultEvent.put("player", player);
                                            callbackContext.success(resultEvent);
                                        }
                                    } catch (JSONException e) {
                                        Log.d(TAG, "Error while receiving event: " + e.getMessage());
                                    }
                                }
                            } else {
                                // Handle Error
                                Exception exception = task.getException();
                                int statusCode = CommonStatusCodes.DEVELOPER_ERROR;
                                if (exception instanceof ApiException) {
                                    ApiException apiException = (ApiException) exception;
                                    statusCode = apiException.getStatusCode();
                                }
                                Log.d(TAG, "event error " + statusCode);
                            }
                        }
                    });
            }
        });
    }

    /**
     * Set value for user event
     */
    private void incrementEventAction(String id, int amount, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getEventsClient(cordova.getActivity()).increment(id, amount);
                callbackContext.success();
            }
        });
    }

    /**
     * Check if signed in - Updated for v2 API
     */
    private void isSignedInAction(final CallbackContext callbackContext) {
        debugLog("Checking sign-in status");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GamesSignInClient signInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    signInClient.isAuthenticated().addOnCompleteListener(task -> {
                        try {
                            boolean isSignedIn = task.isSuccessful() && task.getResult().isAuthenticated();
                            debugLog("Sign-in status: " + isSignedIn);
                            callbackContext.success(isSignedIn);
                        } catch (Exception e) {
                            debugLog("Error creating result: " + e.getMessage(), e);
                            callbackContext.error("Error checking sign-in status: " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    debugLog("Error checking sign-in status: " + e.getMessage(), e);
                    callbackContext.error("Error checking sign-in status: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Check if Google Play Services are available
     */
    private void isGooglePlayServicesAvailableAction(final CallbackContext callbackContext) {
        debugLog("Checking Google Play Services availability");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                    int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
                    
                    boolean isAvailable = resultCode == ConnectionResult.SUCCESS;
                    debugLog("Google Play Services availability: " + isAvailable + " (result code: " + resultCode + ")");
                    
                    if (isAvailable) {
                        callbackContext.success(true);
                    } else {
                        // If services are not available, we can provide more detailed information
                        JSONObject result = new JSONObject();
                        result.put("available", false);
                        result.put("errorCode", resultCode);
                        result.put("errorString", googleApiAvailability.getErrorString(resultCode));
                        result.put("isUserResolvable", googleApiAvailability.isUserResolvableError(resultCode));
                        callbackContext.success(result);
                    }
                } catch (Exception e) {
                    debugLog("Error checking Google Play Services availability: " + e.getMessage(), e);
                    callbackContext.error("Error checking Google Play Services availability: " + e.getMessage());
                }
            }
        });
    }

    private void debugLog(String message) {
        if (debugMode) {
            Log.d(TAG, message);
        }
    }

    private void debugLog(String message, Throwable throwable) {
        if (debugMode) {
            Log.d(TAG, message, throwable);
        }
    }

    private void handleError(Exception e, CallbackContext callbackContext) {
        debugLog("Error occurred: " + e.getMessage(), e);
        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            debugLog("API Exception status code: " + apiException.getStatusCode());
            callbackContext.error(apiException.getStatusCode());
        } else {
            callbackContext.error(e.getMessage());
        }
    }
}
