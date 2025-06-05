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
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapDrawable;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.stats.PlayerStats;
import com.google.android.gms.games.stats.PlayerStatsClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Google Play Games Services Plugin for Cordova
 * 
 * A modern implementation of Google Play Games Services v2 API for Cordova/PhoneGap applications.
 * This plugin provides comprehensive gaming features including authentication, leaderboards,
 * achievements, cloud saves, and more.
 *
 * IMPORTANT: This plugin exclusively uses Google Play Games Services v2 API.
 * It does not support or use any v1 API features or classes.
 * 
 * Key v2 API Features:
 * - Modern authentication using GamesSignInClient
 * - Simplified API structure with direct client access
 * - Improved error handling and callbacks
 * - Better performance and reliability
 * - Support for latest Google Play Games features
 *
 * Java Compatibility:
 * - Compatible with Java 7 and above
 * - Does not use Java 8 features (lambdas, streams, etc.)
 * - Uses traditional anonymous classes for callbacks
 * - Maintains backward compatibility with older Android versions
 *
 * Dependencies:
 * - com.google.android.gms:play-services-games-v2
 * - com.google.android.gms:play-services-auth
 *
 * @author Exelerus AB
 * @version 1.0.0
 * @see <a href="https://github.com/edimuj/cordova-plugin-gpgs">GitHub Repository</a>
 * @see <a href="https://developers.google.com/games/services/android/v2">Google Play Games Services v2 Documentation</a>
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

    /**
     * Initialize the plugin with Google Play Games Services v2.
     * This method sets up the Play Games SDK and attempts silent sign-in.
     * Uses the modern v2 initialization approach with PlayGamesSdk.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
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
                        GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                        gamesSignInClient.signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthenticationResult> task) {
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
                    GamesSignInClient signInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    signInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
                            if (task.isSuccessful()) {
                                boolean isCurrentlySignedIn = task.getResult().isAuthenticated();
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
                GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                gamesSignInClient.signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthenticationResult> task) {
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
     * Login using Google Play Games Services v2.
     * This method uses the modern GamesSignInClient for authentication.
     * First attempts silent sign-in, then falls back to UI sign-in if needed.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
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
                    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    gamesSignInClient.signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
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
     * Unlock an achievement using v2 API.
     * Uses the modern Achievements client with proper success/failure callbacks.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
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
     * Increment an achievement
     * Note: This is a fire-and-forget operation in v2.
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
     * Show achievements
     */
    private void showAchievementsAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AchievementsClient achievementsClient = PlayGames.getAchievementsClient(cordova.getActivity());
                achievementsClient.getAchievementsIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_ACHIEVEMENT_UI);
                            callbackContext.success();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error showing achievements: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Reveal an achievement
     * Note: This is a fire-and-forget operation in v2.
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
     * Set steps in an achievement
     * Note: This is a fire-and-forget operation in v2.
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
     * Submit a score to a leaderboard using v2 API.
     * Uses the modern Leaderboards client.
     * Note: This is a fire-and-forget operation in v2.
     */
    private void updatePlayerScoreAction(String leaderboardId, Integer score, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity()).submitScore(leaderboardId, score);
                callbackContext.success();
            }
        });
    }

    /**
     * Load player score
     */
    private void loadPlayerScoreAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(cordova.getActivity());
                leaderboardsClient.loadCurrentPlayerLeaderboardScore(leaderboardId, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC)
                    .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                        @Override
                        public void onSuccess(AnnotatedData<LeaderboardScore> scoreData) {
                            try {
                                LeaderboardScore score = scoreData.get();
                                if (score != null) {
                                    JSONObject result = new JSONObject();
                                    result.put("score", score.getRawScore());
                                    result.put("displayScore", score.getDisplayScore());
                                    result.put("rank", score.getRank());
                                    result.put("displayRank", score.getDisplayRank());
                                    callbackContext.success(result);
                                } else {
                                    callbackContext.error("Score not found.");
                                }
                            } catch (JSONException e) {
                                callbackContext.error("Error creating result: " + e.getMessage());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error loading score: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Show leaderboard
     */
    private void showLeaderboardAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(cordova.getActivity());
                leaderboardsClient.getLeaderboardIntent(leaderboardId)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_LEADERBOARD_UI);
                            callbackContext.success();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error showing leaderboard: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Show all leaderboards
     */
    private void showAllLeaderboardsAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(cordova.getActivity());
                leaderboardsClient.getAllLeaderboardsIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_LEADERBOARDS_UI);
                            callbackContext.success();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error showing leaderboards: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Show saved games
     */
    private void showSavedGamesAction(String title, Boolean allowAddButton, Boolean allowDelete, Integer numberOfSavedGames, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient.getSelectSnapshotIntent(title, allowAddButton, allowDelete, numberOfSavedGames)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_SAVED_GAMES);
                            callbackContext.success();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error showing saved games: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Save game data using v2 API.
     * Uses the modern Snapshots client with simplified conflict resolution.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
    private void saveGameAction(String snapshotName, String snapshotDescription, JSONObject snapshotContents, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

                snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
                    .addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                        @Override
                        public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> dataOrConflict) {
                            Snapshot snapshot = dataOrConflict.getData();
                            if (snapshot == null) {
                                callbackContext.error("Error opening snapshot: snapshot is null");
                                return;
                            }
                            // Set the data payload for the snapshot
                            snapshot.getSnapshotContents().writeBytes(snapshotContents.toString().getBytes(StandardCharsets.UTF_8));
                            // Create the change operation
                            SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                    .setDescription(snapshotDescription)
                                    .build();
                            // Commit the operation
                            snapshotsClient.commitAndClose(snapshot, metadataChange)
                                .addOnSuccessListener(new OnSuccessListener<SnapshotMetadata>() {
                                    @Override
                                    public void onSuccess(SnapshotMetadata snapshotMetadata) {
                                        callbackContext.success();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        callbackContext.error("Error committing and closing snapshot: " + e.getMessage());
                                    }
                                });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error opening snapshot: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Save game
     */
    private void loadGameSaveAction(String snapshotName, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;
                snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
                    .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
                        @Override
                        public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                            Snapshot snapshot = task.getResult().getData();
                            if (snapshot == null) {
                                throw new IOException("Snapshot is null");
                            }
                            return snapshot.getSnapshotContents().readFully();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
                        @Override
                        public void onComplete(@NonNull Task<byte[]> task) {
                            if(task.isSuccessful()){
                                try {
                                    callbackContext.success(new JSONObject(new String(task.getResult(), StandardCharsets.UTF_8)));
                                } catch (JSONException e) {
                                    callbackContext.error("Error creating JSON from saved game: " + e.getMessage());
                                }
                            } else {
                                callbackContext.error("Error loading saved game: " + task.getException().getMessage());
                            }
                        }
                    });
            }
        });
    }

    /**
     * Get friends list
     */
    private void getFriendsListAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.loadFriends(100, false)
                    .addOnSuccessListener(new OnSuccessListener<AnnotatedData<PlayerBuffer>>() {
                        @Override
                        public void onSuccess(AnnotatedData<PlayerBuffer> data) {
                            try {
                                PlayerBuffer playerBuffer = data.get();
                                if (playerBuffer == null) {
                                    callbackContext.error("Player buffer is null");
                                    return;
                                }
                                JSONArray result = new JSONArray();
                                for (Player player : playerBuffer) {
                                    JSONObject playerObj = new JSONObject();
                                    playerObj.put("id", player.getPlayerId());
                                    playerObj.put("displayName", player.getDisplayName());
                                    playerObj.put("title", player.getTitle());
                                    result.put(playerObj);
                                }
                                playerBuffer.release();
                                callbackContext.success(result);
                            } catch (JSONException e) {
                                callbackContext.error("Error creating result: " + e.getMessage());
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error getting friends list: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Show another player profile, used also for player search
     */
    private void showAnotherPlayersProfileAction(String playerId, @Nullable final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.getCompareProfileIntent(playerId)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_SHOW_PROFILE);
                            if (callbackContext != null) {
                                callbackContext.success();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (callbackContext != null) {
                                callbackContext.error("Error showing player profile: " + e.getMessage());
                            }
                        }
                    });
            }
        });
    }

    /**
     * Show player search default window
     */
    private void showPlayerSearchAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.getPlayerSearchIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            cordova.startActivityForResult(GPGS.this, intent, RC_SHOW_PLAYER_SEARCH);
                            callbackContext.success();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error showing player search: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Get player information using v2 API.
     * Uses the modern PlayersClient with simplified data access.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
    private void getPlayerAction(String id, Boolean forceReload, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.loadPlayer(id, forceReload)
                    .addOnSuccessListener(new OnSuccessListener<AnnotatedData<Player>>() {
                        @Override
                        public void onSuccess(AnnotatedData<Player> data) {
                           Player player = data.get();
                           if (player == null) {
                               callbackContext.success(new JSONObject());
                               return;
                           }
                           try {
                               JSONObject result = new JSONObject();
                               result.put("id", player.getPlayerId());
                               result.put("displayName", player.getDisplayName());
                               result.put("title", player.getTitle());
                               callbackContext.success(result);
                           } catch (JSONException e) {
                               callbackContext.error("Error creating result: " + e.getMessage());
                           }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callbackContext.error("Error loading player: " + e.getMessage());
                        }
                    });
            }
        });
    }

    /**
     * Get current player stats using v2 API.
     * Uses the modern PlayerStatsClient with improved data access.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
    private void getCurrentPlayerStatsAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayerStatsClient playerStatsClient = PlayGames.getPlayerStatsClient(cordova.getActivity());
                playerStatsClient.loadPlayerStats(true)
                    .addOnCompleteListener(new OnCompleteListener<AnnotatedData<PlayerStats>>() {
                        @Override
                        public void onComplete(@NonNull Task<AnnotatedData<PlayerStats>> task) {
                            if (task.isSuccessful()) {
                                AnnotatedData<PlayerStats> playerStatsData = task.getResult();
                                PlayerStats stats = playerStatsData.get();
                                if (stats != null) {
                                    try {
                                        JSONObject result = new JSONObject();
                                        result.put("averageSessionLength", stats.getAverageSessionLength());
                                        result.put("daysSinceLastPlayed", stats.getDaysSinceLastPlayed());
                                        result.put("numberOfPurchases", stats.getNumberOfPurchases());
                                        result.put("numberOfSessions", stats.getNumberOfSessions());
                                        result.put("sessionPercentile", stats.getSessionPercentile());
                                        result.put("spendPercentile", stats.getSpendPercentile());
                                        callbackContext.success(result);
                                    } catch (JSONException e) {
                                        callbackContext.error("Error creating result: " + e.getMessage());
                                    }
                                } else {
                                    callbackContext.error("Player stats not found.");
                                }
                            } else {
                                callbackContext.error("Error loading player stats: " + task.getException().getMessage());
                            }
                        }
                    });
            }
        });
    }

    /**
     * Get all events using v2 API.
     * Uses the modern Events client with simplified data access.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
    private void getAllEventsAction(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventsClient eventsClient = PlayGames.getEventsClient(cordova.getActivity());
                eventsClient.load(true)
                    .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                        @Override
                        public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                            if (task.isSuccessful()) {
                                AnnotatedData<EventBuffer> eventBufferData = task.getResult();
                                EventBuffer events = eventBufferData.get();
                                try {
                                    JSONArray result = new JSONArray();
                                    for (Event event : events) {
                                        JSONObject eventObj = new JSONObject();
                                        eventObj.put("id", event.getEventId());
                                        eventObj.put("name", event.getName());
                                        eventObj.put("description", event.getDescription());
                                        eventObj.put("value", event.getValue());
                                        eventObj.put("formattedValue", event.getFormattedValue());
                                        result.put(eventObj);
                                    }
                                    events.release();
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    callbackContext.error("Error creating result: " + e.getMessage());
                                }
                            } else {
                                callbackContext.error("Error loading events: " + task.getException().getMessage());
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
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventsClient eventsClient = PlayGames.getEventsClient(cordova.getActivity());
                eventsClient.loadByIds(true, id)
                    .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                        @Override
                        public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                            if (task.isSuccessful()) {
                                AnnotatedData<EventBuffer> eventBufferData = task.getResult();
                                EventBuffer events = eventBufferData.get();
                                if (events.getCount() > 0) {
                                    Event event = events.get(0);
                                    try {
                                        JSONObject result = new JSONObject();
                                        result.put("id", event.getEventId());
                                        result.put("name", event.getName());
                                        result.put("description", event.getDescription());
                                        result.put("value", event.getValue());
                                        result.put("formattedValue", event.getFormattedValue());
                                        callbackContext.success(result);
                                    } catch (JSONException e) {
                                        callbackContext.error("Error creating result: " + e.getMessage());
                                    }
                                } else {
                                    callbackContext.error("Event not found.");
                                }
                                events.release();
                            } else {
                                callbackContext.error("Error loading event: " + task.getException().getMessage());
                            }
                        }
                    });
            }
        });
    }

    /**
     * Increment an event
     * Note: This is a fire-and-forget operation in v2.
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
     * Check if signed in using v2 API.
     * Uses the modern GamesSignInClient for authentication status.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
     */
    private void isSignedInAction(final CallbackContext callbackContext) {
        debugLog("Checking sign-in status");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GamesSignInClient signInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    signInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
                            try {
                                boolean isSignedIn = task.isSuccessful() && task.getResult().isAuthenticated();
                                debugLog("Sign-in status: " + isSignedIn);
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("isSignedIn", isSignedIn);
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    callbackContext.error("Error creating response: " + e.getMessage());
                                }
                            } catch (Exception e) {
                                debugLog("Error creating result: " + e.getMessage(), e);
                                callbackContext.error("Error checking sign-in status: " + e.getMessage());
                            }
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
     * Check if Google Play Services are available.
     * Uses the modern GoogleApiAvailability for service checks.
     * 
     * Note: Uses Java 7 compatible code with anonymous classes for callbacks.
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
                        try {
                            JSONObject result = new JSONObject();
                            result.put("available", true);
                            callbackContext.success(result);
                        } catch (JSONException e) {
                            callbackContext.error("Error creating response: " + e.getMessage());
                        }
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
