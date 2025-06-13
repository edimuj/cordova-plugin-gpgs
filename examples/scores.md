# Working with Leaderboards & Scores

This short guide shows a few common leaderboard operations using the Cordova `cordova-plugin-gpgs` API. All methods return **Promises** so they can be chained or awaited.

> The examples assume you have already called `GPGS.initialize()` and the player is signed-in.

---

## Constants

```javascript
const LEADERBOARD_ID = 'CgkIxxxxxxxx'; // Replace with your real leaderboard id

// Time spans
const DAILY   = 0;
const WEEKLY  = 1;
const ALLTIME = 2;

// Collections
const PUBLIC   = 0;
const FRIENDS  = 1; // Social collection (= friends)
```

---

## 1. Submitting a Score

```javascript
const score = 42000;
GPGS.submitScore(LEADERBOARD_ID, score)
    .then(() => console.log('Score submitted!'))
    .catch(console.error);
```

---

## 2. Getting the Player's Own Score & Rank

```javascript
GPGS.getPlayerScore(LEADERBOARD_ID)
    .then(result => {
        /* Result example:
        {
            leaderboard_id: 'CgkIxxxxxxxx',
            player_score: 42000,
            player_rank: 7,          // can be -1 if the player has no score yet
            time_span: 2,            // same enum as above
            collection: 0            // 0=public,1=friends
        }
        */
        if (result.player_rank === -1) {
            console.log('No score yet');
        } else {
            console.log(`Your rank is #${result.player_rank}`);
        }
    })
    .catch(console.error);
```

---

## 3. Fetching a Player-Centred Score Page

`loadPlayerCenteredScores` returns a **slice** of the leaderboard where the signed-in player is roughly in the middle. This is useful for showing neighbouring ranks.

```javascript
const maxResults  = 25; // how many rows to request
const timeSpan    = ALLTIME;
const collection  = PUBLIC;

GPGS.loadPlayerCenteredScores(LEADERBOARD_ID, timeSpan, collection, maxResults)
    .then(result => {
        /* Result example:
        {
            leaderboard: {
                id: 'CgkIxxxxxxxx',
                name: 'High Scores',
                iconImageUri: 'https://…',
                scoreOrder: 0 // 0=LARGER_IS_BETTER, 1=SMALLER_IS_BETTER
            },
            scores: [
                {
                    rank: 6,
                    rawScore: 43000,
                    formattedScore: '43,000',
                    player: {
                        id: 'player123',
                        displayName: 'Alice',
                        iconImageUri: 'https://…'
                    }
                },
                {
                    rank: 7, // <= YOU
                    rawScore: 42000,
                    player: { /* … */ }
                },
                // … up to maxResults items
            ]
        }
        */

        // Highlight the current player row
        const myRow = result.scores.find(s => s.player && s.player.isCurrentPlayer);
        console.log('My score row: ', myRow);
    })
    .catch(console.error);
```

---

## 4. Top-N Scores (optional)

If you need the absolute top scores instead, call `loadTopScores`:

```javascript
GPGS.loadTopScores(LEADERBOARD_ID, ALLTIME, PUBLIC, 10)
    .then(result => console.table(result.scores))
    .catch(console.error);
```

---

### Error Handling

All methods reject with an `Error` object that may contain a `statusCode` from the underlying Play Games SDK. Always add a `.catch()` (or use `try/await/catch`) to surface failures.

```javascript
// Example
GPGS.submitScore(LEADERBOARD_ID, 123)
    .catch(err => {
        console.error('Failed to submit score:', err.message, err.statusCode);
    });
```

---

That's it! For more capabilities check the full API in the README. 