# Google Play Games Services Plugin for Cordova

A modern Cordova plugin for Google Play Games Services v2 API with comprehensive gaming features.

## Features

- **Authentication**
  - Automatic silent sign-in on startup
  - Background sign-out detection
  - Manual sign-in support
  - Sign-in state events
  - Modern v2 authentication flow
- **Leaderboards**
  - Submit scores
  - Show leaderboards
  - Get player scores
  - Get player rankings
- **Achievements**
  - Unlock achievements
  - Increment achievements
  - Show achievements UI
  - Reveal hidden achievements
  - Set achievement steps
- **Cloud Saves**
  - Save game data
  - Load game data
  - Show saved games UI
  - Conflict resolution
  - Snapshot management
- **Friends**
  - Get friends list
  - Show player profiles
  - Player search
  - Compare profiles
- **Player Stats**
  - Get player info
  - Get player stats
  - Get player level info
- **Events**
  - Increment events
  - Get event data
  - Get all events
  - Event tracking
- **Quests & Milestones**
  - Track quest progress
  - Manage milestones
- **Challenges**
  - Create challenges
  - Accept challenges
  - Track challenge progress

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

## Configuration

Add the following to your `config.xml`:

```xml
<preference name="GPGS_DEBUG" value="true" />
```

## Usage

### Initialization

The plugin automatically initializes when your app starts. It will:
1. Check if Google Play Services are available
2. Attempt silent sign-in if services are available
3. Emit events for sign-in status and service availability

```javascript
// Listen for sign-in events
document.addEventListener('gpgs.signin', function(event) {
    const data = event.detail;
    if (data.isSignedIn) {
        console.log('User is signed in');
        // Enable game features
    } else {
        console.log('Sign-in failed:', data.error);
        // Show sign-in button or handle error
    }
});

// Listen for sign-out events (including background sign-out)
document.addEventListener('gpgs.signout', function(event) {
    const data = event.detail;
    console.log('User signed out:', data.reason);
    // Disable game features that require sign-in
    // Show sign-in button
});

// Listen for Google Play Services availability
document.addEventListener('gpgs.availability', function(event) {
    const data = event.detail;
    if (data.available) {
        console.log('Google Play Services are available');
    } else {
        console.log('Google Play Services are not available:', data.errorString);
        if (data.isUserResolvable) {
            // Show UI to help user resolve the issue
            console.log('This issue can be resolved by the user');
        }
    }
});
```

### Authentication

```javascript
// Check if user is signed in
GPGS.isSignedIn().then(isSignedIn => {
    if (isSignedIn) {
        console.log('User is signed in');
    } else {
        console.log('User is not signed in');
    }
});
// Returns: Promise<boolean>

// Manual sign-in
GPGS.login().then(() => {
    console.log('Sign-in successful');
}).catch(error => {
    console.error('Sign-in failed:', error);
});
// Returns: Promise<void>
```

### Leaderboards

```javascript
// Submit a score
GPGS.submitScore('leaderboard_id', 1000).then(() => {
    console.log('Score submitted');
});
// Returns: Promise<void>

// Show a leaderboard
GPGS.showLeaderboard('leaderboard_id').then(() => {
    console.log('Leaderboard shown');
});
// Returns: Promise<void>

// Get player's score
GPGS.getPlayerScore('leaderboard_id').then(score => {
    console.log('Player score:', score);
});
// Returns: Promise<{
//   score: number,
//   displayScore: string,
//   scoreTag: string,
//   rank: number
// }>
```

### Achievements

```javascript
// Unlock an achievement
GPGS.unlockAchievement('achievement_id').then(() => {
    console.log('Achievement unlocked');
});
// Returns: Promise<void>

// Increment an achievement
GPGS.incrementAchievement('achievement_id', 1).then(() => {
    console.log('Achievement incremented');
});
// Returns: Promise<void>

// Show achievements UI
GPGS.showAchievements().then(() => {
    console.log('Achievements UI shown');
});
// Returns: Promise<void>

// Reveal a hidden achievement
GPGS.revealAchievement('achievement_id').then(() => {
    console.log('Achievement revealed');
});
// Returns: Promise<void>

// Set achievement steps
GPGS.setStepsInAchievement('achievement_id', 5).then(() => {
    console.log('Achievement steps set');
});
// Returns: Promise<void>
```

### Cloud Saves

```javascript
// Save game data
GPGS.saveGame('save_name', {
    level: 1,
    score: 1000
}).then(() => {
    console.log('Game saved');
});
// Returns: Promise<void>

// Load game data
GPGS.loadGame('save_name').then(data => {
    console.log('Game loaded:', data);
});
// Returns: Promise<Object> - The saved game data

// Show saved games UI
GPGS.showSavedGames({
    title: 'Saved Games',
    allowAddButton: true,
    allowDelete: true,
    maxSnapshots: 5
}).then(() => {
    console.log('Saved games UI shown');
});
// Returns: Promise<void>
```

### Friends

```javascript
// Get friends list
GPGS.getFriendsList().then(friends => {
    console.log('Friends:', friends);
});
// Returns: Promise<Array<{
//   id: string,
//   name: string,
//   title: string,
//   retrievedTimestamp: number,
//   bannerImageLandscapeUri?: string,
//   bannerImagePortraitUri?: string,
//   iconImageUri?: string,
//   hiResImageUri?: string,
//   iconImageBase64?: string,
//   levelInfo?: {
//     currentLevel: number,
//     maxXp: number,
//     minXp: number,
//     hashCode: number
//   },
//   currentPlayerInfo?: {
//     friendsListVisibilityStatus: number
//   },
//   friendStatus?: number
// }>>

// Show player profile
GPGS.showPlayerProfile('player_id').then(() => {
    console.log('Player profile shown');
});
// Returns: Promise<void>

// Show player search
GPGS.showPlayerSearch().then(() => {
    console.log('Player search shown');
});
// Returns: Promise<void>
```

### Player Stats

```javascript
// Get player info
GPGS.getPlayerInfo('player_id').then(info => {
    console.log('Player info:', info);
});
// Returns: Promise<{
//   id: string,
//   displayName: string,
//   title: string,
//   levelInfo?: {
//     currentLevel: number,
//     maxXp: number,
//     minXp: number
//   }
// }>

// Get player stats
GPGS.getPlayerStats().then(stats => {
    console.log('Player stats:', stats);
});
// Returns: Promise<{
//   averageSessionLength: number,
//   daysSinceLastPlayed: number,
//   numberOfPurchases: number,
//   numberOfSessions: number,
//   sessionPercentile: number,
//   spendPercentile: number,
//   spendProbability: number
// }>
```

### Events

```javascript
// Increment an event
GPGS.incrementEvent('event_id', 1).then(() => {
    console.log('Event incremented');
});
// Returns: Promise<void>

// Get all events
GPGS.getAllEvents().then(events => {
    console.log('All events:', events);
});
// Returns: Promise<Array<{
//   id: string,
//   name: string,
//   description: string,
//   value: number
// }>>

// Get specific event
GPGS.getEvent('event_id').then(event => {
    console.log('Event:', event);
});
// Returns: Promise<{
//   id: string,
//   name: string,
//   description: string,
//   value: number
// }>
```

## Events

The plugin emits the following events:

### `gpgs.signin`
Emitted when sign-in state changes.
```javascript
{
    isSignedIn: boolean,
    error?: string  // Present if sign-in failed
}
```

### `gpgs.signout`
Emitted when user signs out (including background sign-out).
```javascript
{
    isSignedIn: false,
    reason: string  // e.g., "background_signout"
}
```

### `gpgs.availability`
Emitted when Google Play Services availability changes.
```javascript
{
    available: boolean,
    errorCode?: number,
    errorString?: string,
    isUserResolvable?: boolean
}
```

## Error Handling

The plugin uses promises for all operations. Errors are passed to the catch handler:

```javascript
GPGS.login().catch(error => {
    console.error('Error:', error);
});
```

Common error codes:
- `ERROR_CODE_HAS_RESOLUTION`: The error can be resolved by the user
- `ERROR_CODE_NO_RESOLUTION`: The error cannot be resolved by the user

## Debug Mode

Enable debug mode in `config.xml` to see detailed logs:

```xml
<preference name="GPGS_DEBUG" value="true" />
```

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