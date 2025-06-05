# Google Play Games Services Plugin for Cordova

A modern Cordova plugin for Google Play Games Services v2 API with comprehensive gaming features.

## Features

- ✅ Authentication (v2 API)
- ✅ Leaderboards
- ✅ Achievements
- ✅ Cloud saves
- ✅ Friends
- ✅ Player stats
- ✅ Events
- ✅ Quests
- ✅ Milestones
- ✅ Challenges
- ✅ Snapshots

## Requirements

- Cordova >= 12.0.0
- Cordova Android >= 14.0.0
- Android SDK >= 24
- Google Play Services >= 21.2.0

## Installation

```bash
cordova plugin add cordova-plugin-gpgs --variable APP_ID="your-app-id" --variable PLAY_SERVICES_VERSION="23.2.0"
```

### Configuration Variables

- `APP_ID` (required): Your Google Play Games App ID
- `PLAY_SERVICES_VERSION` (optional): Version of Google Play Services to use (default: 23.2.0)

## Usage

All methods return Promises for modern async/await support.

### Authentication

```javascript
// Check if user is signed in
try {
    const isSignedIn = await cordova.plugins.GPGS.isSignedIn();
    if (isSignedIn) {
        // User is signed in
        enableGameFeatures();
    } else {
        // Show sign-in button
        showSignInButton();
    }
} catch (error) {
    console.error('Error checking sign-in status:', error);
}

// Sign in
try {
    await cordova.plugins.GPGS.login();
    // Sign-in successful
    enableGameFeatures();
} catch (error) {
    console.error('Sign-in failed:', error);
}
```

### Achievements

```javascript
// Unlock achievement
try {
    await cordova.plugins.GPGS.unlockAchievement('achievement_id');
    console.log('Achievement unlocked!');
} catch (error) {
    console.error('Failed to unlock achievement:', error);
}

// Increment achievement
try {
    await cordova.plugins.GPGS.incrementAchievement('achievement_id', 1);
    console.log('Achievement incremented!');
} catch (error) {
    console.error('Failed to increment achievement:', error);
}

// Show achievements UI
try {
    await cordova.plugins.GPGS.showAchievements();
    console.log('Achievements UI closed');
} catch (error) {
    console.error('Failed to show achievements:', error);
}
```

### Leaderboards

```javascript
// Submit score
try {
    await cordova.plugins.GPGS.submitScore('leaderboard_id', 1000);
    console.log('Score submitted!');
} catch (error) {
    console.error('Failed to submit score:', error);
}

// Get player's score
try {
    const score = await cordova.plugins.GPGS.getPlayerScore('leaderboard_id');
    console.log('Player score:', score);
} catch (error) {
    console.error('Failed to get player score:', error);
}

// Show leaderboard UI
try {
    await cordova.plugins.GPGS.showLeaderboard('leaderboard_id');
    console.log('Leaderboard UI closed');
} catch (error) {
    console.error('Failed to show leaderboard:', error);
}

// Show all leaderboards
try {
    await cordova.plugins.GPGS.showAllLeaderboards();
    console.log('Leaderboards UI closed');
} catch (error) {
    console.error('Failed to show leaderboards:', error);
}
```

### Cloud Saves

```javascript
// Save game
try {
    await cordova.plugins.GPGS.saveGame('save_name', { level: 5, score: 1000 });
    console.log('Game saved!');
} catch (error) {
    console.error('Failed to save game:', error);
}

// Load game
try {
    const saveData = await cordova.plugins.GPGS.loadGame('save_name');
    console.log('Game loaded:', saveData);
} catch (error) {
    console.error('Failed to load game:', error);
}

// Show saved games UI
try {
    await cordova.plugins.GPGS.showSavedGames({
        title: 'Saved Games',
        allowAddButton: true,
        allowDelete: true,
        maxSnapshots: 5
    });
    console.log('Saved games UI closed');
} catch (error) {
    console.error('Failed to show saved games:', error);
}
```

### Player Data

```javascript
// Get player info
try {
    const playerInfo = await cordova.plugins.GPGS.getPlayerInfo();
    console.log('Player info:', playerInfo);
} catch (error) {
    console.error('Failed to get player info:', error);
}

// Get player stats
try {
    const stats = await cordova.plugins.GPGS.getPlayerStats();
    console.log('Player stats:', stats);
} catch (error) {
    console.error('Failed to get player stats:', error);
}

// Get friends list
try {
    const friends = await cordova.plugins.GPGS.getFriendsList();
    console.log('Friends:', friends);
} catch (error) {
    console.error('Failed to get friends:', error);
}

// Show player profile
try {
    await cordova.plugins.GPGS.showPlayerProfile('player_id');
    console.log('Profile UI closed');
} catch (error) {
    console.error('Failed to show profile:', error);
}
```

### Events

```javascript
// Increment event
try {
    await cordova.plugins.GPGS.incrementEvent('event_id', 1);
    console.log('Event incremented!');
} catch (error) {
    console.error('Failed to increment event:', error);
}

// Get all events
try {
    const events = await cordova.plugins.GPGS.getAllEvents();
    console.log('Events:', events);
} catch (error) {
    console.error('Failed to get events:', error);
}

// Get specific event
try {
    const event = await cordova.plugins.GPGS.getEvent('event_id');
    console.log('Event:', event);
} catch (error) {
    console.error('Failed to get event:', error);
}
```

## Error Handling

The plugin uses standard JavaScript Promises for error handling. All methods can throw errors that should be caught using try/catch blocks. Common error types include:

- Authentication errors
- Network errors
- Invalid parameter errors
- Google Play Services errors

## Troubleshooting

### Common Issues

1. **Sign-in not working**
   - Make sure your app is properly configured in the Google Play Console
   - Verify your APP_ID is correct
   - Check that the app is signed with the correct keystore
   - Ensure Google Play Services is up to date on the device

2. **Achievements/Leaderboards not showing**
   - Verify they are properly configured in the Google Play Console
   - Check that the IDs match exactly
   - Ensure the app is published to the correct track
   - Verify the player is signed in

3. **Cloud saves not working**
   - Check that cloud save is enabled in the Google Play Console
   - Verify the player is signed in
   - Check for proper error handling
   - Ensure the device has enough storage space

### Debug Mode

To enable debug mode, add this to your `config.xml`:

```xml
<preference name="GPGS_DEBUG" value="true" />
```

This will enable verbose logging in the Android logcat.

## License

This project is licensed under the GPL-3.0-or-later License - see the [LICENSE](LICENSE) file for details.

## Author

Exelerus AB - [https://exelerus.com](https://exelerus.com)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request