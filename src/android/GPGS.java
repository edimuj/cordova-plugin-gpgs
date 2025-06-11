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

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.AuthenticationResult;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayGamesSdk;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
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

import java.nio.charset.StandardCharsets;

public class GPGS extends CordovaPlugin {

    private static final String TAG = "GOOGLE_PLAY_GAMES";
    private boolean debugMode = false;

    private static final int RC_ACHIEVEMENT_UI = 9003;
    private static final int RC_LEADERBOARD_UI = 9004;
    private static final int RC_LEADERBOARDS_UI = 9005;
    private static final int RC_SAVED_GAMES = 9009;
    private static final int RC_SHOW_PROFILE = 9010;
    private static final int RC_SHOW_PLAYER_SEARCH = 9011;
    private static final int RC_SIGN_IN = 9012;

    private static final String EVENT_SIGN_IN = "gpgs.signin";
    private static final String EVENT_SIGN_OUT = "gpgs.signout";
    private static final String EVENT_AVAILABILITY = "gpgs.availability";

    private static final int ERROR_CODE_HAS_RESOLUTION = 1;
    private static final int ERROR_CODE_NO_RESOLUTION = 2;

    private CordovaWebView cordovaWebView;
    private boolean wasSignedIn = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        cordovaWebView = webView;

        // Initialize the SDK
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    PlayGamesSdk.initialize(cordova.getActivity());

                    // Check if signed in
                    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    gamesSignInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
                            if (task.isSuccessful()) {
                                boolean isAuthenticated = task.getResult().isAuthenticated();
                                if (isAuthenticated) {
                                    wasSignedIn = true;
                                    emitWindowEvent(EVENT_SIGN_IN);
                                    debugLog("GPGS - Already signed in.");
                                } else {
                                    wasSignedIn = false;
                                    debugLog("GPGS - Not signed in.");
                                }
                            } else {
                                wasSignedIn = false;
                                handleError(task.getException(), null);
                            }
                        }
                    });

                } catch (Exception e) {
                    handleError(e, null);
                }
            }
        });
    }


    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        // Try to sign in silently on resume.
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                GamesSignInClient signInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                signInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthenticationResult> task) {
                        if (task.isSuccessful()) {
                            boolean isAuthenticated = task.getResult().isAuthenticated();
                            if (!wasSignedIn && isAuthenticated) {
                                wasSignedIn = true;
                                emitWindowEvent(EVENT_SIGN_IN);
                                debugLog("GPGS - Signed in on resume.");
                            } else if (wasSignedIn && !isAuthenticated) {
                                wasSignedIn = false;
                                emitWindowEvent(EVENT_SIGN_OUT);
                                debugLog("GPGS - Signed out on resume.");
                            }
                        } else {
                            handleError(task.getException(), null);
                        }
                    }
                });
            }
        });
    }

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

        else if (action.equals("loadAchievements")) {
            this.loadAchievementsAction(args.getBoolean(0), callbackContext);
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

        else if (action.equals("loadTopScores")) {
            this.loadTopScoresAction(args.getString(0), args.getInt(1), args.getInt(2), args.getInt(3), callbackContext);
            return true;
        }

        else if (action.equals("loadPlayerCenteredScores")) {
            this.loadPlayerCenteredScoresAction(args.getString(0), args.getInt(1), args.getInt(2), args.getInt(3), callbackContext);
            return true;
        }

        else if (action.equals("loadLeaderboardMetadata")) {
            if (args.length() > 0) {
                this.loadLeaderboardMetadataAction(args.getString(0), callbackContext);
            } else {
                this.loadAllLeaderboardsMetadataAction(callbackContext);
            }
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

        else if (action.equals("deleteSnapshot")) {
            this.deleteSnapshotAction(args.getString(0), callbackContext);
            return true;
        }

        else if (action.equals("loadAllSnapshots")) {
            this.loadAllSnapshotsAction(args.getBoolean(0), callbackContext);
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
            this.getPlayerAction(args.getString(0), args.length() > 1 && args.getBoolean(1), callbackContext);
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

        else if (action.equals("initialize")) {
            this.initializeAction(callbackContext);
            return true;
        }

        return false;
    }

    private void emitWindowEvent(final String event) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.loadUrl("javascript:cordova.fireWindowEvent('" + event + "');");
            }
        });
    }

    private void emitWindowEvent(final String event, final JSONObject data) {
        final CordovaWebView view = this.webView;
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.loadUrl("javascript:cordova.fireWindowEvent('" + event + "'," + data.toString() + ");");
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Handle results from Play Games UI activities
    }

    private void signInSilently() {
        // Sign-in client.
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                gamesSignInClient.signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthenticationResult> task) {
                        if (task.isSuccessful()) {
                            wasSignedIn = true;
                            emitWindowEvent(EVENT_SIGN_IN);
                            debugLog("GPGS - Sign in successful (silently).");
                        } else {
                            Exception e = task.getException();
                            if (e instanceof ApiException && ((ApiException) e).getStatusCode() == com.google.android.gms.common.api.CommonStatusCodes.SIGN_IN_REQUIRED) {
                                debugLog("GPGS - Silent sign in failed, needs manual sign in.");
                            } else {
                                handleError(e, null);
                            }
                        }
                    }
                });
            }
        });
    }

    private void loginAction(JSONArray args, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GamesSignInClient gamesSignInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    gamesSignInClient.signIn().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
                            if (task.isSuccessful()) {
                                wasSignedIn = true;
                                emitWindowEvent(EVENT_SIGN_IN);
                                callbackContext.success();
                            } else {
                                handleError(task.getException(), callbackContext);
                            }
                        }
                    });
                } catch (Exception e) {
                    handleError(e, callbackContext);
                }
            }
        });
    }

    private void unlockAchievementAction(String achievementId, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).unlock(achievementId);
                callbackContext.success();
            }
        });
    }

    private void incrementAchievementAction(String achievementId, Integer count, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).increment(achievementId, count);
                callbackContext.success();
            }
        });
    }

    private void showAchievementsAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity())
                        .getAchievementsIntent()
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                                callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void revealAchievementAction(String achievementId, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).reveal(achievementId);
                callbackContext.success();
            }
        });
    }

    private void setStepsInAchievementAction(String achievementId, int count, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity()).setSteps(achievementId, count);
                callbackContext.success();
            }
        });
    }

    private void loadAchievementsAction(boolean forceReload, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getAchievementsClient(cordova.getActivity())
                        .load(forceReload)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<AchievementBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<AchievementBuffer> data) {
                                AchievementBuffer achievementBuffer = data.get();
                                if (achievementBuffer == null) {
                                    callbackContext.success(new JSONArray());
                                    return;
                                }
                                try {
                                    JSONArray result = new JSONArray();
                                    for (Achievement achievement : achievementBuffer) {
                                        result.put(convertAchievementToJson(achievement));
                                    }
                                    achievementBuffer.release();
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void updatePlayerScoreAction(String leaderboardId, Integer score, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity()).submitScore(leaderboardId, score);
                callbackContext.success();
            }
        });
    }

    private void loadPlayerScoreAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .loadCurrentPlayerLeaderboardScore(leaderboardId, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                            @Override
                            public void onSuccess(AnnotatedData<LeaderboardScore> scoreData) {
                                if (scoreData == null || scoreData.get() == null) {
                                    callbackContext.error("No score found.");
                                    return;
                                }
                                try {
                                    LeaderboardScore score = scoreData.get();
                                    JSONObject result = new JSONObject();
                                    result.put("player_score", score.getRawScore());
                                    result.put("player_rank", score.getRank());
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void showLeaderboardAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .getLeaderboardIntent(leaderboardId)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_LEADERBOARD_UI);
                                callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void showAllLeaderboardsAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .getAllLeaderboardsIntent()
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_LEADERBOARDS_UI);
                                callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void showSavedGamesAction(String title, Boolean allowAddButton, Boolean allowDelete, Integer numberOfSavedGames, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient.getSelectSnapshotIntent(title, allowAddButton, allowDelete, numberOfSavedGames)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_SAVED_GAMES);
                                callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void saveGameAction(String snapshotName, String snapshotDescription, JSONObject snapshotContents, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient.open(snapshotName, true)
                        .addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                            @Override
                            public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> dataOrConflict) {
                                if (dataOrConflict.isConflict()) {
                                    // Handle conflict
                                    callbackContext.error("Snapshot conflict.");
                                    return;
                                }
                                Snapshot snapshot = dataOrConflict.getData();
                                snapshot.getSnapshotContents().writeBytes(snapshotContents.toString().getBytes(StandardCharsets.UTF_8));
                                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                                        .setDescription(snapshotDescription)
                                        .build();
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
                                                handleError(e, callbackContext);
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void loadGameSaveAction(String snapshotName, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient.open(snapshotName, false)
                        .continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
                            @Override
                            public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                                Snapshot snapshot = task.getResult().getData();
                                return snapshot.getSnapshotContents().readFully();
                            }
                        })
                        .addOnCompleteListener(new OnCompleteListener<byte[]>() {
                            @Override
                            public void onComplete(@NonNull Task<byte[]> task) {
                                if (task.isSuccessful()) {
                                    try {
                                        JSONObject result = new JSONObject(new String(task.getResult(), StandardCharsets.UTF_8));
                                        callbackContext.success(result);
                                    } catch (JSONException e) {
                                        handleError(e, callbackContext);
                                    }
                                } else {
                                    handleError(task.getException(), callbackContext);
                                }
                            }
                        });
            }
        });
    }

    private void getFriendsListAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.loadFriends(100, false)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<PlayerBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<PlayerBuffer> data) {
                                PlayerBuffer playerBuffer = data.get();
                                if (playerBuffer == null) {
                                    callbackContext.error("No friends found.");
                                    return;
                                }
                                try {
                                    JSONArray friends = new JSONArray();
                                    for (Player player : playerBuffer) {
                                        JSONObject friend = new JSONObject();
                                        friend.put("id", player.getPlayerId());
                                        friend.put("displayName", player.getDisplayName());
                                        friends.put(friend);
                                    }
                                    playerBuffer.release();
                                    callbackContext.success(friends);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void showAnotherPlayersProfileAction(String playerId, @Nullable final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.getCompareProfileIntent(playerId)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_SHOW_PROFILE);
                                if (callbackContext != null)
                                    callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (callbackContext != null)
                                    handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void showPlayerSearchAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.getPlayerSearchIntent()
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                cordova.setActivityResultCallback(GPGS.this);
                                cordova.getActivity().startActivityForResult(intent, RC_SHOW_PLAYER_SEARCH);
                                callbackContext.success();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }


    private void getPlayerAction(String id, Boolean forceReload, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayersClient playersClient = PlayGames.getPlayersClient(cordova.getActivity());
                playersClient.loadPlayer(id, forceReload)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<Player>>() {
                            @Override
                            public void onSuccess(AnnotatedData<Player> data) {
                                Player player = data.get();
                                if (player == null) {
                                    callbackContext.error("Player not found.");
                                    return;
                                }
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("id", player.getPlayerId());
                                    result.put("displayName", player.getDisplayName());
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void getAllEventsAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                EventsClient eventsClient = PlayGames.getEventsClient(cordova.getActivity());
                eventsClient.load(true)
                        .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                            @Override
                            public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                                if (task.isSuccessful()) {
                                    AnnotatedData<EventBuffer> eventData = task.getResult();
                                    EventBuffer eventBuffer = eventData.get();
                                    if (eventBuffer == null) {
                                        callbackContext.error("No events found.");
                                        return;
                                    }
                                    try {
                                        JSONArray events = new JSONArray();
                                        for (Event event : eventBuffer) {
                                            JSONObject eventJson = new JSONObject();
                                            eventJson.put("id", event.getEventId());
                                            eventJson.put("name", event.getName());
                                            eventJson.put("description", event.getDescription());
                                            eventJson.put("value", event.getValue());
                                            events.put(eventJson);
                                        }
                                        eventBuffer.release();
                                        callbackContext.success(events);
                                    } catch (JSONException e) {
                                        handleError(e, callbackContext);
                                    }
                                } else {
                                    handleError(task.getException(), callbackContext);
                                }
                            }
                        });
            }
        });
    }

    private void getEventAction(String id, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                EventsClient eventsClient = PlayGames.getEventsClient(cordova.getActivity());
                eventsClient.loadByIds(true, id)
                        .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
                            @Override
                            public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
                                if (task.isSuccessful()) {
                                    AnnotatedData<EventBuffer> eventData = task.getResult();
                                    EventBuffer eventBuffer = eventData.get();
                                    if (eventBuffer == null || eventBuffer.getCount() == 0) {
                                        callbackContext.error("Event not found.");
                                        if (eventBuffer != null)
                                            eventBuffer.release();
                                        return;
                                    }
                                    try {
                                        Event event = eventBuffer.get(0);
                                        JSONObject eventJson = new JSONObject();
                                        eventJson.put("id", event.getEventId());
                                        eventJson.put("name", event.getName());
                                        eventJson.put("description", event.getDescription());
                                        eventJson.put("value", event.getValue());
                                        eventBuffer.release();
                                        callbackContext.success(eventJson);
                                    } catch (JSONException e) {
                                        handleError(e, callbackContext);
                                    }
                                } else {
                                    handleError(task.getException(), callbackContext);
                                }
                            }
                        });
            }
        });
    }

    private void incrementEventAction(String id, int amount, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                PlayGames.getEventsClient(cordova.getActivity()).increment(id, amount);
                callbackContext.success();
            }
        });
    }

    private void isSignedInAction(final CallbackContext callbackContext) {
        // Check if the user is signed in.
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GamesSignInClient signInClient = PlayGames.getGamesSignInClient(cordova.getActivity());
                    signInClient.isAuthenticated().addOnCompleteListener(new OnCompleteListener<AuthenticationResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthenticationResult> task) {
                            if (task.isSuccessful()) {
                                boolean isAuthenticated = task.getResult().isAuthenticated();
                                try {
                                    JSONObject result = new JSONObject();
                                    result.put("isSignedIn", isAuthenticated);
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            } else {
                                handleError(task.getException(), callbackContext);
                            }
                        }
                    });
                } catch (Exception e) {
                    handleError(e, callbackContext);
                }
            }
        });
    }

    private void isGooglePlayServicesAvailableAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                    int status = apiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
                    JSONObject result = new JSONObject();
                    result.put("status", status);
                    result.put("isAvailable", status == ConnectionResult.SUCCESS);
                    callbackContext.success(result);
                } catch (Exception e) {
                    handleError(e, callbackContext);
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
        if (callbackContext == null) {
            debugLog("GPGS Error: " + e.getMessage(), e);
            return;
        }

        try {
            JSONObject error = new JSONObject();
            error.put("message", e.getMessage());

            if (e instanceof com.google.android.gms.common.api.ApiException) {
                com.google.android.gms.common.api.ApiException apiException = (com.google.android.gms.common.api.ApiException) e;
                error.put("statusCode", apiException.getStatusCode());
            }

            callbackContext.error(error);
        } catch (JSONException jsonException) {
            callbackContext.error("{\"message\": \"" + e.getMessage() + "\", \"originalException\": \"" + jsonException.getMessage() + "\"}");
        }

        debugLog("GPGS Error: " + e.getMessage(), e);
    }

    private JSONObject convertAchievementToJson(Achievement achievement) throws JSONException {
        if (achievement == null) return null;
        JSONObject json = new JSONObject();
        json.put("achievementId", achievement.getAchievementId());
        json.put("name", achievement.getName());
        json.put("description", achievement.getDescription());
        json.put("type", achievement.getType());
        json.put("state", achievement.getState());
        json.put("xpValue", achievement.getXpValue());
        json.put("lastUpdatedTimestamp", achievement.getLastUpdatedTimestamp());
        json.put("revealedImageUri", achievement.getRevealedImageUri() != null ? achievement.getRevealedImageUri().toString() : null);
        json.put("unlockedImageUri", achievement.getUnlockedImageUri() != null ? achievement.getUnlockedImageUri().toString() : null);
        if (achievement.getType() == Achievement.TYPE_INCREMENTAL) {
            json.put("currentSteps", achievement.getCurrentSteps());
            json.put("totalSteps", achievement.getTotalSteps());
        }
        return json;
    }

    private JSONObject convertLeaderboardToJson(Leaderboard leaderboard) throws JSONException {
        if (leaderboard == null) return null;
        JSONObject json = new JSONObject();
        json.put("leaderboardId", leaderboard.getLeaderboardId());
        json.put("displayName", leaderboard.getDisplayName());
        json.put("iconImageUri", leaderboard.getIconImageUri() != null ? leaderboard.getIconImageUri().toString() : null);
        json.put("scoreOrder", leaderboard.getScoreOrder());
        return json;
    }

    private JSONObject convertLeaderboardScoreToJson(LeaderboardScore score) throws JSONException {
        if (score == null) return null;
        JSONObject json = new JSONObject();
        json.put("rank", score.getRank());
        json.put("displayRank", score.getDisplayRank());
        json.put("rawScore", score.getRawScore());
        json.put("displayScore", score.getDisplayScore());
        json.put("timestampMillis", score.getTimestampMillis());
        if (score.getScoreHolder() != null) {
            json.put("scoreHolder", convertPlayerToJson(score.getScoreHolder()));
        }
        return json;
    }

    private JSONObject convertLoadScoresResultToJson(Leaderboards.LoadScoresResult result) throws JSONException {
        if (result == null) return null;
        JSONObject json = new JSONObject();
        json.put("leaderboard", convertLeaderboardToJson(result.getLeaderboard()));
        JSONArray scores = new JSONArray();
        LeaderboardScoreBuffer buffer = result.getScores();
        if (buffer != null) {
            for (LeaderboardScore score : buffer) {
                scores.put(convertLeaderboardScoreToJson(score));
            }
            buffer.release();
        }
        json.put("scores", scores);
        return json;
    }

    private JSONObject convertSnapshotMetadataToJson(SnapshotMetadata metadata) throws JSONException {
        if (metadata == null) return null;
        JSONObject json = new JSONObject();
        json.put("snapshotId", metadata.getSnapshotId());
        json.put("uniqueName", metadata.getUniqueName());
        json.put("title", metadata.getGame().getDisplayName());
        json.put("description", metadata.getDescription());
        json.put("lastModifiedTimestamp", metadata.getLastModifiedTimestamp());
        json.put("playedTime", metadata.getPlayedTime());
        json.put("coverImageUri", metadata.getCoverImageUri() != null ? metadata.getCoverImageUri().toString() : null);
        return json;
    }

    private JSONObject convertPlayerToJson(Player player) throws JSONException {
        if (player == null) return null;
        JSONObject json = new JSONObject();
        json.put("id", player.getPlayerId());
        json.put("displayName", player.getDisplayName());
        json.put("iconImageUri", player.getIconImageUri() != null ? player.getIconImageUri().toString() : null);
        json.put("hiResImageUri", player.getHiResImageUri() != null ? player.getHiResImageUri().toString() : null);
        json.put("title", player.getTitle());
        if (player.getLevelInfo() != null) {
            JSONObject levelInfo = new JSONObject();
            levelInfo.put("currentLevel", player.getLevelInfo().getCurrentLevel().getLevelNumber());
            levelInfo.put("currentXp", player.getLevelInfo().getCurrentXp());
            levelInfo.put("lastLevelUpTimestamp", player.getLevelInfo().getLastLevelUpTimestamp());
            json.put("levelInfo", levelInfo);
        }
        return json;
    }

    private void loadTopScoresAction(String leaderboardId, int timeSpan, int collection, int maxResults, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .loadTopScores(leaderboardId, timeSpan, collection, maxResults)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<Leaderboards.LoadScoresResult>>() {
                            @Override
                            public void onSuccess(AnnotatedData<Leaderboards.LoadScoresResult> data) {
                                try {
                                    callbackContext.success(convertLoadScoresResultToJson(data.get()));
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void loadPlayerCenteredScoresAction(String leaderboardId, int timeSpan, int collection, int maxResults, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .loadPlayerCenteredScores(leaderboardId, timeSpan, collection, maxResults)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<Leaderboards.LoadScoresResult>>() {
                            @Override
                            public void onSuccess(AnnotatedData<Leaderboards.LoadScoresResult> data) {
                                try {
                                    callbackContext.success(convertLoadScoresResultToJson(data.get()));
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void loadAllLeaderboardsMetadataAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .loadLeaderboardMetadata(false)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<LeaderboardBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<LeaderboardBuffer> data) {
                                LeaderboardBuffer buffer = data.get();
                                if (buffer == null) {
                                    callbackContext.success(new JSONArray());
                                    return;
                                }
                                try {
                                    JSONArray result = new JSONArray();
                                    for (Leaderboard leaderboard : buffer) {
                                        result.put(convertLeaderboardToJson(leaderboard));
                                    }
                                    buffer.release();
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void loadLeaderboardMetadataAction(String leaderboardId, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getLeaderboardsClient(cordova.getActivity())
                        .loadLeaderboardMetadata(leaderboardId, false)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<Leaderboard>>() {
                            @Override
                            public void onSuccess(AnnotatedData<Leaderboard> data) {
                                Leaderboard leaderboard = data.get();
                                if (leaderboard == null) {
                                    callbackContext.error("Leaderboard not found.");
                                    return;
                                }
                                try {
                                    callbackContext.success(convertLeaderboardToJson(leaderboard));
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void deleteSnapshotAction(String snapshotName, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SnapshotsClient snapshotsClient = PlayGames.getSnapshotsClient(cordova.getActivity());
                snapshotsClient.open(snapshotName, false)
                        .addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                            @Override
                            public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> dataOrConflict) {
                                if (dataOrConflict.isConflict()) {
                                    callbackContext.error("Snapshot conflict. Cannot delete.");
                                    return;
                                }
                                Snapshot snapshot = dataOrConflict.getData();
                                if (snapshot == null) {
                                    callbackContext.error("Snapshot not found.");
                                    return;
                                }
                                snapshotsClient.delete(snapshot.getMetadata())
                                        .addOnSuccessListener(new OnSuccessListener<String>() {
                                            @Override
                                            public void onSuccess(String s) {
                                                callbackContext.success(s);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                handleError(e, callbackContext);
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void loadAllSnapshotsAction(boolean forceReload, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                PlayGames.getSnapshotsClient(cordova.getActivity())
                        .load(forceReload)
                        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<SnapshotMetadataBuffer>>() {
                            @Override
                            public void onSuccess(AnnotatedData<SnapshotMetadataBuffer> data) {
                                SnapshotMetadataBuffer buffer = data.get();
                                if (buffer == null) {
                                    callbackContext.success(new JSONArray());
                                    return;
                                }
                                try {
                                    JSONArray result = new JSONArray();
                                    for (SnapshotMetadata metadata : buffer) {
                                        result.put(convertSnapshotMetadataToJson(metadata));
                                    }
                                    buffer.release();
                                    callbackContext.success(result);
                                } catch (JSONException e) {
                                    handleError(e, callbackContext);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                handleError(e, callbackContext);
                            }
                        });
            }
        });
    }

    private void initializeAction(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                signInSilently();
                callbackContext.success();
            }
        });
    }
} 