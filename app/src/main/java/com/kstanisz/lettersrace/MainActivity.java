/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kstanisz.lettersrace;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.*;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.kstanisz.lettersrace.communication.Message;
import com.kstanisz.lettersrace.communication.MessageType;
import com.kstanisz.lettersrace.game.GameMode;
import com.kstanisz.lettersrace.game.LettersRace;
import org.apache.commons.lang3.SerializationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements View.OnClickListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

    private final static String TAG = "LettersRace";

    private Activity activity = this;

    // Request codes for the UIs that we show with startActivityForResult:
    private final static int RC_SELECT_PLAYERS = 10000;
    private final static int RC_INVITATION_INBOX = 10001;
    private final static int RC_WAITING_ROOM = 10002;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to sign in with Google APIs
    private GoogleSignInClient googleSignInClient = null;

    // Client used to interact with the real time multiplayer system.
    private RealTimeMultiplayerClient realTimeMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient invitationsClient = null;

    // Room ID where the currently active game is taking place; null if we're
    // not playing.
    private String roomId = null;

    // Holds the configuration of the current room.
    private RoomConfig roomConfig;

    // Game mode
    private boolean multiplayerMode;
    private GameMode gameMode;

    // The participants in the currently active game
    private ArrayList<Participant> participants = null;

    // My participant ID in the currently active game
    private String myId = null;

    // If non-null, this is the id of the invitation we received via the
    // invitation listener
    private String incomingInvitationId = null;

    private LettersRace lettersRace;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the client used to sign in.
        googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        switchToMainScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        signInSilently();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister our listeners.  They will be re-registered via onResume->signInSilently->onConnected.
        if (invitationsClient != null) {
            invitationsClient.unregisterInvitationCallback(invitationCallback);
        }
    }

    @Override
    public void onClick(View view) {
        startSignInIntent();
    }

    public void onMenuClick(View view) {
        switch (view.getId()) {
            case R.id.button_single_player:
            case R.id.button_single_player_2:
                Log.d(TAG, "Single player button clicked");
                startSinglePlayerGame();
                break;
            case R.id.button_sign_in:
                Log.d(TAG, "Sign-in button clicked");
                startSignInIntent();
                break;
            case R.id.button_sign_out:
                Log.d(TAG, "Sign-out button clicked");
                signOut();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                Log.d(TAG, "Invite players button clicked");
                invitePlayers();
                break;
            case R.id.button_see_invitations:
                Log.d(TAG, "See invitations button clicked");
                seeInvitations();
                break;
            case R.id.button_accept_popup_invitation:
                Log.d(TAG, "Accept invitation button clicked");
                gameMode = GameMode.MULTIPLAYER_FRIENDS;
                acceptInviteToRoom(incomingInvitationId);
                incomingInvitationId = null;
                break;
            case R.id.button_quick_game:
                Log.d(TAG, "Quick game button clicked");
                startQuickGame();
                break;
        }
    }

    public void onGameScreenClick(View view) {
        switch (view.getId()) {
            case R.id.button_guess_phrase:
                Log.d(TAG, "Guess phrase button clicked");
                sendGuessStartedMessage();
                break;
            case R.id.button_guess_confirm:
                Log.d(TAG, "Guess confirm button clicked");
                lettersRace.confirmGuess();
                break;
            case R.id.button_guess_cancel:
                Log.d(TAG, "Guess cancel button clicked");
                lettersRace.cancelGuess();
                break;
            case R.id.button_play_again:
                Log.d(TAG, "Play again button clicked");
                leaveRoom();
                switch (gameMode) {
                    case SINGLE_PLAYER:
                        startSinglePlayerGame();
                        break;
                    case MULTIPLAYER_FRIENDS:
                        invitePlayers();
                        break;
                    case MULTIPLAYER_RANDOM:
                        startQuickGame();
                        break;
                }
                break;
            case R.id.button_leave_game:
                Log.d(TAG, "Leave room button clicked");
                leaveRoom();
                switchToMainScreen();
                break;
        }
    }

    public void onGameKeyboardClick(View view) {
        Button button = (Button) view;
        String letter = button.getText().toString();
        Log.d(TAG, "Keyboard key: " + letter + " clicked");
        lettersRace.letterPressed(letter);
    }

    private void startSinglePlayerGame() {
        gameMode = GameMode.SINGLE_PLAYER;
        resetGame();
        startGame(false);
    }

    private void startQuickGame() {
        gameMode = GameMode.MULTIPLAYER_RANDOM;

        // quick-start a game with 1 randomly selected opponent
        final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS, MAX_OPPONENTS, 0);
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGame();

        roomConfig = RoomConfig.builder(roomUpdateCallback)
                .setOnMessageReceivedListener(onRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(roomStatusUpdateCallback)
                .setAutoMatchCriteria(autoMatchCriteria)
                .build();
        realTimeMultiplayerClient.create(roomConfig);
    }

    private void invitePlayers() {
        gameMode = GameMode.MULTIPLAYER_FRIENDS;
        switchToScreen(R.id.screen_wait);

        // show list of invitable players
        realTimeMultiplayerClient.getSelectOpponentsIntent(1, 3).addOnSuccessListener(
                new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                }
        ).addOnFailureListener(createFailureListener("There was a problem selecting opponents."));
    }

    private void seeInvitations() {
        gameMode = GameMode.MULTIPLAYER_FRIENDS;
        switchToScreen(R.id.screen_wait);

        // show list of pending invitations
        invitationsClient.getInvitationInboxIntent().addOnSuccessListener(
                new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_INVITATION_INBOX);
                    }
                }
        ).addOnFailureListener(createFailureListener("There was a problem getting the inbox."));
    }

    /**
     * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
     * your Activity's onActivityResult function
     */
    private void startSignInIntent() {
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    /**
     * Try to sign in without displaying dialogs to the user.
     * <p>
     * If the user has already signed in previously, it will not show dialog.
     */
    private void signInSilently() {
        Log.d(TAG, "signInSilently()");

        googleSignInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Log.d(TAG, "signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    private void signOut() {
        Log.d(TAG, "signOut()");

        googleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signOut(): success");
                        } else {
                            handleException(task.getException(), "signOut() failed!");
                        }

                        onDisconnected();
                    }
                });
    }

    /**
     * Since a lot of the operations use tasks, we can use a common handler for whenever one fails.
     *
     * @param exception The exception to evaluate.  Will try to display a more descriptive reason for the exception.
     * @param details   Will display alongside the exception if you wish to provide more details for why the exception
     *                  happened
     */
    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

        String errorString = null;
        switch (status) {
            case GamesCallbackStatusCodes.OK:
                break;
            case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                errorString = getString(R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
                errorString = getString(R.string.match_error_already_rematched);
                break;
            case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
                errorString = getString(R.string.network_error_operation_failed);
                break;
            case GamesClientStatusCodes.INTERNAL_ERROR:
                errorString = getString(R.string.internal_error);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
                errorString = getString(R.string.match_error_inactive_match);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
                errorString = getString(R.string.match_error_locally_modified);
                break;
            default:
                errorString = getString(R.string.unexpected_status, GamesClientStatusCodes.getStatusCodeString(status));
                break;
        }

        if (errorString == null) {
            return;
        }

        String message = getString(R.string.status_exception_error, details, status, exception);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Error")
                .setMessage(message + "\n" + errorString)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }

                onDisconnected();

                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        } else if (requestCode == RC_SELECT_PLAYERS) {
            // we got the result from the "select players" UI -- ready to create the room
            handleSelectPlayersResult(resultCode, intent);

        } else if (requestCode == RC_INVITATION_INBOX) {
            // we got the result from the "select invitation" UI (invitation inbox). We're
            // ready to accept the selected invitation:
            handleInvitationInboxResult(resultCode, intent);

        } else if (requestCode == RC_WAITING_ROOM) {
            // we got the result from the "waiting room" UI.
            if (resultCode == Activity.RESULT_OK) {
                // ready to start playing
                Log.d(TAG, "Starting game (waiting room returned OK).");
                startGame(true);
            } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                // player indicated that they want to leave the room
                leaveRoom();
                switchToMainScreen();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // Dialog was cancelled (user pressed back key, for instance). In our game,
                // this means leaving the room too. In more elaborate games, this could mean
                // something else (like minimizing the waiting room UI).
                leaveRoom();
                switchToMainScreen();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.

    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Log.d(TAG, "Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGame();

        roomConfig = RoomConfig.builder(roomUpdateCallback)
                .addPlayersToInvite(invitees)
                .setOnMessageReceivedListener(onRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(roomStatusUpdateCallback)
                .setAutoMatchCriteria(autoMatchCriteria).build();
        realTimeMultiplayerClient.create(roomConfig);
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation invitation = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        if (invitation != null) {
            acceptInviteToRoom(invitation.getInvitationId());
        }
    }

    // Accept the given invitation.
    private void acceptInviteToRoom(String invitationId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invitationId);

        roomConfig = RoomConfig.builder(roomUpdateCallback)
                .setInvitationIdToAccept(invitationId)
                .setOnMessageReceivedListener(onRealTimeMessageReceivedListener)
                .setRoomStatusUpdateCallback(roomStatusUpdateCallback)
                .build();

        switchToScreen(R.id.screen_wait);
        keepScreenOn();
        resetGame();

        realTimeMultiplayerClient.join(roomConfig)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Room Joined Successfully!");
                    }
                });
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        switchToMainScreen();

        super.onStop();
    }

    // Handle back key to make sure we cleanly leave a game if we are in the middle of one
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK && (mCurScreen == R.id.screen_game || mCurScreen == R.id.screen_wait)) {
            leaveRoom();
            switchToMainScreen();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    // Leave the room.
    private void leaveRoom() {
        Log.d(TAG, "Leaving room.");
        stopKeepingScreenOn();
        if (roomId != null) {
            realTimeMultiplayerClient.leave(roomConfig, roomId)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            roomId = null;
                            roomConfig = null;
                        }
                    });
            switchToScreen(R.id.screen_wait);
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    private void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        realTimeMultiplayerClient.getWaitingRoomIntent(room, MIN_PLAYERS)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        // show waiting room UI
                        startActivityForResult(intent, RC_WAITING_ROOM);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the waiting room!"));
    }

    private InvitationCallback invitationCallback = new InvitationCallback() {
        // Called when we get an invitation to play a game. We react by showing that to the user.
        @Override
        public void onInvitationReceived(@NonNull Invitation invitation) {
            // We got an invitation to play a game! So, store it in
            // incomingInvitationId
            // and show the popup on the screen.
            incomingInvitationId = invitation.getInvitationId();
            String invitationText = invitation.getInviter().getDisplayName() + " " + getString(R.string.is_inviting_you);
            ((TextView) findViewById(R.id.incoming_invitation_text)).setText(invitationText);
            if (mCurScreen == R.id.screen_game) {
                Toast.makeText(activity, invitationText, Toast.LENGTH_LONG).show();
            } else {
                switchToScreen(mCurScreen); // This will show the invitation popup
            }
        }

        @Override
        public void onInvitationRemoved(@NonNull String invitationId) {

            if (incomingInvitationId.equals(invitationId) && incomingInvitationId != null) {
                incomingInvitationId = null;
                switchToScreen(mCurScreen); // This will hide the invitation popup
            }
        }
    };

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

    private String playerId;

    // The currently signed in account, used to check the account has changed outside of this activity when resuming.
    private GoogleSignInAccount signedInAccount = null;

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        if (signedInAccount != googleSignInAccount) {

            signedInAccount = googleSignInAccount;

            // update the clients
            realTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(this, googleSignInAccount);
            invitationsClient = Games.getInvitationsClient(MainActivity.this, googleSignInAccount);

            // get the playerId from the PlayersClient
            PlayersClient playersClient = Games.getPlayersClient(this, googleSignInAccount);
            playersClient.getCurrentPlayer()
                    .addOnSuccessListener(new OnSuccessListener<Player>() {
                        @Override
                        public void onSuccess(Player player) {
                            playerId = player.getPlayerId();

                            switchToMainScreen();
                        }
                    })
                    .addOnFailureListener(createFailureListener("There was a problem getting the player id!"));
        }

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        invitationsClient.registerInvitationCallback(invitationCallback);

        // get the invitation from the connection hint
        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(MainActivity.this, googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle hint) {
                        if (hint != null) {
                            Invitation invitation = hint.getParcelable(Multiplayer.EXTRA_INVITATION);

                            if (invitation != null && invitation.getInvitationId() != null) {
                                // retrieve and cache the invitation ID
                                Log.d(TAG, "onConnected: connection hint has a room invite!");
                                acceptInviteToRoom(invitation.getInvitationId());
                            }
                        }
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem getting the activation hint!"));
    }

    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, string);
            }
        };
    }

    private void onDisconnected() {
        Log.d(TAG, "onDisconnected()");

        realTimeMultiplayerClient = null;
        invitationsClient = null;

        switchToMainScreen();
    }

    private RoomStatusUpdateCallback roomStatusUpdateCallback = new RoomStatusUpdateCallback() {
        // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
        // is connected yet).
        @Override
        public void onConnectedToRoom(Room room) {
            Log.d(TAG, "onConnectedToRoom.");

            //get participants and my ID:
            participants = room.getParticipants();
            myId = room.getParticipantId(playerId);

            // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
            if (roomId == null) {
                roomId = room.getRoomId();
            }

            // print out the list of participants (for debug purposes)
            Log.d(TAG, "Room ID: " + roomId);
            Log.d(TAG, "My ID " + myId);
            Log.d(TAG, "<< CONNECTED TO ROOM>>");
        }

        // Called when we get disconnected from the room. We return to the main screen.
        @Override
        public void onDisconnectedFromRoom(Room room) {
            updateRoom(room);
        }


        // We treat most of the room update callbacks in the same way: we update our list of
        // participants and update the display. In a real game we would also have to check if that
        // change requires some action like removing the corresponding player avatar from the screen,
        // etc.
        @Override
        public void onPeerDeclined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerInvitedToRoom(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onP2PDisconnected(@NonNull String participant) {
        }

        @Override
        public void onP2PConnected(@NonNull String participant) {
        }

        @Override
        public void onPeerJoined(Room room, @NonNull List<String> arg1) {
            updateRoom(room);
        }

        @Override
        public void onPeerLeft(Room room, @NonNull List<String> peersWhoLeft) {
            updateRoom(room);
        }

        @Override
        public void onRoomAutoMatching(Room room) {
            updateRoom(room);
        }

        @Override
        public void onRoomConnecting(Room room) {
            updateRoom(room);
        }

        @Override
        public void onPeersConnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }

        @Override
        public void onPeersDisconnected(Room room, @NonNull List<String> peers) {
            updateRoom(room);
        }
    };

    // Show error message about game being cancelled and return to main screen.
    private void showGameError() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.game_problem))
                .setNeutralButton(android.R.string.ok, null).create();

        switchToMainScreen();
    }

    private RoomUpdateCallback roomUpdateCallback = new RoomUpdateCallback() {

        // Called when room has been created
        @Override
        public void onRoomCreated(int statusCode, Room room) {
            Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
                showGameError();
                return;
            }

            // save room ID so we can leave cleanly before the game starts.
            roomId = room.getRoomId();

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Called when room is fully connected.
        @Override
        public void onRoomConnected(int statusCode, Room room) {
            Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
                showGameError();
                return;
            }
            updateRoom(room);
        }

        @Override
        public void onJoinedRoom(int statusCode, Room room) {
            Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
            if (statusCode != GamesCallbackStatusCodes.OK) {
                Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
                showGameError();
                return;
            }

            // show the waiting room UI
            showWaitingRoom(room);
        }

        // Called when we've successfully left the room (this happens a result of voluntarily leaving
        // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
        @Override
        public void onLeftRoom(int statusCode, @NonNull String roomId) {
            Log.d(TAG, "onLeftRoom, code " + statusCode);
        }
    };

    private void updateRoom(Room room) {
        if (room != null) {
            participants = room.getParticipants();
        }
/*        if (participants != null) {
            updatePeerScoresDisplay();
        }*/
    }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

    // Start the gameplay phase of the game.
    private void startGame(boolean multiplayer) {
        multiplayerMode = multiplayer;
        switchToScreen(R.id.screen_game);

        boolean host = !multiplayer || isHost();

        if (host) {
            lettersRace = new LettersRace(this);
            sendStartGameMessage();
        }
    }

    // Reset game
    private void resetGame() {
        if (lettersRace != null) {
            lettersRace.resetGame();
        }
    }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

    private OnRealTimeMessageReceivedListener onRealTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
        @Override
        public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
            byte[] buf = realTimeMessage.getMessageData();
            String senderId = realTimeMessage.getSenderParticipantId();
            Object receivedMessage = SerializationUtils.deserialize(buf);

            if (!(receivedMessage instanceof Message)) {
                throw new RuntimeException("Not recognized message");
            }

            Message message = (Message) receivedMessage;
            MessageType messageType = message.getType();
            switch (messageType) {
                case START_GAME: {
                    Log.d(TAG, "Received start game message");
                    receiveStartGameMessage(message);
                    break;
                }
                case GUESS_STARTED: {
                    Log.d(TAG, "Received guess started message");
                    receiveGuessStartedMessage(senderId);
                    break;
                }
                case GUESS_SUCCEEDED: {
                    Log.d(TAG, "Received guess succeeded message");
                    receiveGuessSucceededMessage(senderId);
                    break;
                }
                case GUESS_FAILED: {
                    Log.d(TAG, "Received guess failed message");
                    receiveGuessFailedMessage(senderId);
                    break;
                }
            }
        }
    };

    private void sendStartGameMessage() {
        Random random = new Random();
        int min = 10000000, max = Integer.MAX_VALUE;
        int hash = random.nextInt(max - min + 1) + min;

        if (!multiplayerMode) {
            lettersRace.startGame(hash);
            return;
        }

        Message message = new Message(MessageType.START_GAME);
        message.setValue(hash);
        byte[] messageBuff = SerializationUtils.serialize(message);

        for (Participant p : participants) {
            if (myId.equals(p.getParticipantId())) {
                lettersRace.startGame(hash);
            }
            sendMessage(messageBuff, p);
        }
    }

    private void receiveStartGameMessage(Message message) {
        int hash = message.getValue();
        lettersRace = new LettersRace(this);
        lettersRace.startGame(hash);
    }

    private void sendGuessStartedMessage() {
        if (!lettersRace.canUserGuess()) {
            return;
        }

        if (!multiplayerMode) {
            lettersRace.startGuessing();
            return;
        }

        Message message = new Message(MessageType.GUESS_STARTED);
        byte[] messageBuff = SerializationUtils.serialize(message);

        // Send to every other participant.
        for (Participant p : participants) {
            if (myId.equals(p.getParticipantId())) {
                lettersRace.startGuessing();
            }
            sendMessage(messageBuff, p);
        }
    }

    private void receiveGuessStartedMessage(String senderId) {
        lettersRace.stopGame();

        Button guessPhraseButton = findViewById(R.id.button_guess_phrase);
        guessPhraseButton.setVisibility(View.GONE);
        TextView guessInfoTextView = findViewById(R.id.guess_info);

        Participant sender = findParticipantById(senderId);
        guessInfoTextView.setText(sender.getDisplayName() + " odgaduje hasło!");
        guessInfoTextView.setVisibility(View.VISIBLE);
    }

    public void sendGuessFailedMessage() {
        if (!multiplayerMode) {
            lettersRace.resumeGame();
            return;
        }

        Message message = new Message(MessageType.GUESS_FAILED);
        byte[] messageBuff = SerializationUtils.serialize(message);

        // Send to every other participant.
        for (Participant p : participants) {
            if (myId.equals(p.getParticipantId())) {
                lettersRace.resumeGame();
            }

            sendMessage(messageBuff, p);
        }
    }

    private void receiveGuessFailedMessage(String senderId) {
        Button guessPhraseButton = findViewById(R.id.button_guess_phrase);
        guessPhraseButton.setVisibility(View.VISIBLE);
        TextView guessInfoTextView = findViewById(R.id.guess_info);

        Participant sender = findParticipantById(senderId);
        guessInfoTextView.setVisibility(View.GONE);

        Toast.makeText(this, sender.getDisplayName() + " nie odgadł hasła!", Toast.LENGTH_LONG).show();

        lettersRace.resumeGame();
    }

    public void sendGuessSucceededMessage() {
        if (!multiplayerMode) {
            lettersRace.endGame(true, null);
            return;
        }

        Message message = new Message(MessageType.GUESS_SUCCEEDED);
        byte[] messageBuff = SerializationUtils.serialize(message);

        // Send to every other participant.
        for (Participant p : participants) {
            if (myId.equals(p.getParticipantId())) {
                lettersRace.endGame(true, p.getDisplayName());
            }

            sendMessage(messageBuff, p);
        }
    }

    private void receiveGuessSucceededMessage(String senderId) {
        Participant winner = findParticipantById(senderId);
        lettersRace.endGame(false, winner.getDisplayName());
    }

    private void sendMessage(byte[] buffer, Participant participant) {
        if (participant.getStatus() != Participant.STATUS_JOINED) {
            return;
        }
        // final score notification must be sent via reliable message
        realTimeMultiplayerClient.sendReliableMessage(buffer,
                roomId, participant.getParticipantId(), new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
                    @Override
                    public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                        Log.d(TAG, "RealTime message sent");
                        Log.d(TAG, "  statusCode: " + statusCode);
                        Log.d(TAG, "  tokenId: " + tokenId);
                        Log.d(TAG, "  recipientParticipantId: " + recipientParticipantId);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer tokenId) {
                        Log.d(TAG, "Created a reliable message with tokenId: " + tokenId);
                    }
                });
    }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

    // This array lists all the individual screens our game has.
    private final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
            R.id.screen_wait
    };
    private int mCurScreen = -1;

    private void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (incomingInvitationId == null) {
            // no invitation, so no popup
            showInvPopup = false;
        } else if (multiplayerMode) {
            // if in multiplayer, only show invitation on main screen
            showInvPopup = (mCurScreen == R.id.screen_main);
        } else {
            showInvPopup = (mCurScreen == R.id.screen_main);
        }
        findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
    }

    private void switchToMainScreen() {
        if (realTimeMultiplayerClient != null) {
            switchToScreen(R.id.screen_main);
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
    }

    /*
     * MISC SECTION. Miscellaneous methods.
     */


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    private void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private Participant findParticipantById(String id) {
        for (Participant participant : participants) {
            if (id.equals(participant.getParticipantId())) {
                return participant;
            }
        }
        return null;
    }

    private boolean isHost() {
        for (Participant p : participants) {
            if (p.getParticipantId().compareTo(myId) < 0) {
                return false;
            }
        }
        return true;
    }
}
